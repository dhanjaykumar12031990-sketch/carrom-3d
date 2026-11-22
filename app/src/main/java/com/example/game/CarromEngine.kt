package com.example.game

import com.example.audio.SoundEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class CarromEngine(
  private val sound: SoundEngine,
  private val onHudUpdated: (UiHudState) -> Unit,
  private val onToast: (String, ToastTone) -> Unit,
  private val onFoulFlash: () -> Unit,
  private val onMatchFinished: (player1Score: Int, won: Boolean, mode: GameMode, challengeLevel: Int) -> Unit =
    { _, _, _, _ -> },
) {
  private val lock = Any()

  private var appScreen = AppScreen.SPLASH
  private var mode = GameMode.CLASSIC_1V1
  private var phase = GamePhase.MENU
  private var menuOpen = true
  private var hasActiveMatch = false
  private var cameraMode = CameraMode.ANGLED_3D
  private var aiDifficulty = AiDifficulty.MEDIUM
  private var cameraSensitivity = 1.0f
  private var previewOrbitRad = 0f

  private var currentPlayer = 0
  private val scores = intArrayOf(0, 0, 0, 0)
  private val pocketedCounts = intArrayOf(0, 0, 0, 0)
  private var queenState = QueenState.ON_BOARD
  private var queenHolder = -1
  private var turnPocketedAny = false
  private var turnPocketedQueen = false
  private var turnFoul = false
  private var strikerValid = true
  private var sliderValue = 0f
  private var turnTimeRemaining = CarromConfig.TURN_DURATION_SEC
  private var winnerTitle = ""
  private var winnerSubtitle = ""
  private var selectedStrikerId = "classic"
  private var selectedBoardId = "birch_royal"

  // Challenge state
  private var challengeLevel = 1
  private var challengeTitle = ""
  private var challengeShotsLeft = 0
  private var challengeRewardCoins = 0

  // Rewind Shot Power-up (1 per match)
  private data class PieceStateCopy(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val active: Boolean,
    val pocketing: Boolean,
    val tiltX: Float,
    val tiltZ: Float,
    val spinY: Float,
  )

  private data class TurnSnapshot(
    val pieces: List<PieceStateCopy>,
    val scores: IntArray,
    val pocketedCounts: IntArray,
    val queenState: QueenState,
    val queenHolder: Int,
    val currentPlayer: Int,
    val challengeShotsLeft: Int,
    val sliderValue: Float,
  )

  private var rewindsRemaining = 1
  private var lastTurnSnapshot: TurnSnapshot? = null
  private var rewindProgress = 0f
  private var rewindFromStates: Map<Int, PieceStateCopy> = emptyMap()

  private val pieces = mutableListOf<Piece>()
  private val arenaElements = mutableListOf<ArenaElement>()
  private var striker: Piece? = null

  private var aiTimer = 0f
  private var aiTotalDuration = 1.55f
  private var aiPlan: AiShotPlan? = null
  private var aiLastTargetId = -1

  private val sparkPool = Array(96) { SparkParticle() }

  private var dragActive = false
  private var dragAnchorX = 0f
  private var dragAnchorZ = 0f
  private var dragDirX = 0f
  private var dragDirZ = -1f
  private var dragPower = 0f
  private var dragCanceling = false
  private var aimVisual = AimVisualState()

  init {
    setupRackLocked(includeChaos = false)
    resetStrikerForTurnLocked()
  }

  fun setAppScreen(screen: AppScreen) {
    synchronized(lock) {
      appScreen = screen
      menuOpen = screen != AppScreen.GAMEPLAY
      if (screen == AppScreen.LOBBY && !hasActiveMatch) {
        setupRackLocked(includeChaos = false)
        resetStrikerForTurnLocked()
      }
      notifyHudLocked()
    }
  }

  fun addPreviewOrbitDelta(deltaRad: Float) {
    synchronized(lock) {
      previewOrbitRad += deltaRad * cameraSensitivity
    }
  }

  fun startNewGame(selectedMode: GameMode) {
    synchronized(lock) {
      appScreen = AppScreen.GAMEPLAY
      mode = selectedMode
      menuOpen = false
      hasActiveMatch = true
      currentPlayer = 0
      scores.fill(0)
      pocketedCounts.fill(0)
      queenState = QueenState.ON_BOARD
      queenHolder = -1
      turnPocketedAny = false
      turnPocketedQueen = false
      turnFoul = false
      rewindsRemaining = 1
      lastTurnSnapshot = null
      aiPlan = null
      aiLastTargetId = -1
      dragActive = false
      aimVisual = AimVisualState()
      turnTimeRemaining = CarromConfig.TURN_DURATION_SEC
      clearSparksLocked()
      setupRackLocked(includeChaos = selectedMode == GameMode.CHAOS_ARENA)
      resetStrikerForTurnLocked()
      phase = GamePhase.AIMING
      notifyHudLocked()
    }
    val msg =
      when (selectedMode) {
        GameMode.CLASSIC_1V1 -> "CLASSIC 1v1 • BREAK SHOT!"
        GameMode.CHAOS_ARENA -> "CHAOS ARENA • BUMPERS & SPEED PADS!"
        GameMode.DESI_ADDA_2V2 -> "DESI ADDA 2v2 • TEAM A BREAK!"
        GameMode.DESI_ADDA_FFA -> "DESI ADDA FFA • PLAYER 1 BREAK!"
        GameMode.TRICK_CHALLENGE -> "CHALLENGE #$challengeLevel"
      }
    onToast(msg, ToastTone.GOOD)
  }

  fun startChallenge(level: Int) {
    val ch = TrickChallengeCatalog.findByLevel(level)
    synchronized(lock) {
      appScreen = AppScreen.GAMEPLAY
      mode = GameMode.TRICK_CHALLENGE
      challengeLevel = ch.level
      challengeTitle = ch.title
      challengeShotsLeft = ch.shotLimit
      challengeRewardCoins = ch.rewardCoins
      menuOpen = false
      hasActiveMatch = true
      currentPlayer = 0
      scores.fill(0)
      pocketedCounts.fill(0)
      queenState = QueenState.ON_BOARD
      queenHolder = -1
      turnPocketedAny = false
      turnPocketedQueen = false
      turnFoul = false
      rewindsRemaining = 1
      lastTurnSnapshot = null
      aiPlan = null
      dragActive = false
      aimVisual = AimVisualState()
      turnTimeRemaining = 40f
      clearSparksLocked()
      setupChallengeRackLocked(ch)
      resetStrikerForTurnLocked()
      phase = GamePhase.AIMING
      notifyHudLocked()
    }
    onToast("CHALLENGE #${ch.level}: ${ch.title.uppercase()}", ToastTone.QUEEN)
  }

  fun openMenu() {
    synchronized(lock) {
      appScreen = AppScreen.LOBBY
      menuOpen = true
      dragActive = false
      aimVisual = AimVisualState()
      notifyHudLocked()
    }
  }

  fun resumeGame() {
    synchronized(lock) {
      if (!hasActiveMatch) return
      appScreen = AppScreen.GAMEPLAY
      menuOpen = false
      notifyHudLocked()
    }
  }

  fun toggleCamera() {
    synchronized(lock) {
      cameraMode =
        if (cameraMode == CameraMode.ANGLED_3D) CameraMode.TOP_DOWN else CameraMode.ANGLED_3D
      notifyHudLocked()
    }
  }

  fun setSoundEnabled(enabled: Boolean) {
    sound.enabled = enabled
    synchronized(lock) { notifyHudLocked() }
  }

  fun getSelectedStrikerId(): String = synchronized(lock) { selectedStrikerId }

  fun getSelectedBoardId(): String = synchronized(lock) { selectedBoardId }

  fun setSelectedStrikerId(id: String) {
    synchronized(lock) {
      selectedStrikerId = id
      notifyHudLocked()
    }
  }

  fun setSelectedBoardId(id: String) {
    synchronized(lock) {
      selectedBoardId = id
      notifyHudLocked()
    }
  }

  fun setAiDifficulty(diff: AiDifficulty) {
    synchronized(lock) { aiDifficulty = diff }
  }

  fun setCameraSensitivity(sens: Float) {
    synchronized(lock) { cameraSensitivity = sens.coerceIn(0.6f, 1.6f) }
  }

  fun rewindLastShot(): Boolean {
    synchronized(lock) {
      val snap = lastTurnSnapshot ?: return false
      if (rewindsRemaining <= 0 || phase == GamePhase.REWINDING || phase == GamePhase.GAMEOVER || menuOpen) {
        return false
      }
      if (mode == GameMode.CLASSIC_1V1 && phase == GamePhase.AI_THINKING) return false
      rewindsRemaining--
      dragActive = false
      aimVisual = AimVisualState()
      rewindFromStates =
        pieces.associate {
          it.id to PieceStateCopy(it.id, it.x, it.y, it.z, it.active, it.pocketing, it.tiltX, it.tiltZ, it.spinY)
        }
      for (p in pieces) {
        p.vx = 0f
        p.vy = 0f
        p.vz = 0f
        p.pocketing = false
        val target = snap.pieces.firstOrNull { it.id == p.id }
        if (target != null && target.active) {
          p.active = true
        }
      }
      rewindProgress = 0f
      phase = GamePhase.REWINDING
      sound.playRewind()
      notifyHudLocked()
    }
    onToast("TIME REWIND!", ToastTone.GOOD)
    return true
  }

  fun onSliderChanged(value: Float) {
    synchronized(lock) {
      if (phase != GamePhase.AIMING || menuOpen) return
      if (isAiTurnLocked()) return
      sliderValue = value.coerceIn(-CarromConfig.BASELINE_LIMIT, CarromConfig.BASELINE_LIMIT)
      val s = striker ?: return
      val pos = baselineCoordsLocked(currentPlayer, sliderValue)
      s.x = pos.first
      s.z = pos.second
      strikerValid = checkStrikerValidLocked()
      notifyHudLocked()
    }
  }

  fun onPointerDown(boardX: Float, boardZ: Float) {
    synchronized(lock) {
      if (phase != GamePhase.AIMING || menuOpen) return
      if (isAiTurnLocked()) return
      val s = striker ?: return
      dragActive = true
      dragPower = 0f
      dragCanceling = false
      if (hypot(boardX - s.x, boardZ - s.z) < 1.45f) {
        dragAnchorX = s.x
        dragAnchorZ = s.z
      } else {
        dragAnchorX = boardX
        dragAnchorZ = boardZ
      }
      aimVisual = AimVisualState()
      notifyHudLocked()
    }
  }

  fun onPointerMove(boardX: Float, boardZ: Float) {
    synchronized(lock) {
      if (!dragActive) return
      val s = striker ?: return
      val pdx = (dragAnchorX - boardX) * (0.85f + cameraSensitivity * 0.15f)
      val pdz = (dragAnchorZ - boardZ) * (0.85f + cameraSensitivity * 0.15f)
      val dist = hypot(pdx, pdz)
      if (dist < 0.22f) {
        dragPower = 0f
        dragCanceling = true
        aimVisual = AimVisualState()
        notifyHudLocked()
        return
      }
      val dirX = pdx / dist
      val dirZ = pdz / dist
      val clamped = min(CarromConfig.MAX_PULL_DIST, dist)
      val power = min(1f, (clamped - 0.22f) / (CarromConfig.MAX_PULL_DIST - 0.22f))
      dragDirX = dirX
      dragDirZ = dirZ
      dragPower = power
      dragCanceling = false
      aimVisual = buildAimVisualLocked(s.x, s.z, dirX, dirZ, power, s.x - dirX * clamped, s.z - dirZ * clamped)
      notifyHudLocked()
    }
  }

  fun onPointerUp() {
    var toastMsg: String? = null
    var toastTone = ToastTone.NEUTRAL
    synchronized(lock) {
      if (!dragActive) return
      dragActive = false
      dragCanceling = false
      aimVisual = AimVisualState()
      if (dragPower >= 0.05f && phase == GamePhase.AIMING) {
        if (!strikerValid) {
          sound.playFoul()
          toastMsg = "MOVE STRIKER TO CLEAR SPOT!"
          toastTone = ToastTone.FOUL
        } else {
          val s = striker
          if (s != null) {
            captureTurnSnapshotLocked()
            if (mode == GameMode.TRICK_CHALLENGE && challengeShotsLeft > 0) {
              challengeShotsLeft--
            }
            val spd =
              CarromConfig.MIN_SHOT_SPEED +
                dragPower.pow(1.15f) * (CarromConfig.MAX_SHOT_SPEED - CarromConfig.MIN_SHOT_SPEED)
            s.vx = dragDirX * spd
            s.vz = dragDirZ * spd
            sound.playShoot(dragPower)
            phase = GamePhase.SIMULATING
          }
        }
      }
      dragPower = 0f
      notifyHudLocked()
    }
    if (toastMsg != null) onToast(toastMsg!!, toastTone)
  }

  fun stepFrame(dtRaw: Float): RenderSceneSnapshot {
    val dt = min(0.033f, max(0.001f, dtRaw))
    var timeoutToast: String? = null
    synchronized(lock) {
      if (appScreen == AppScreen.SPLASH || appScreen == AppScreen.AUTH) {
        previewOrbitRad += dt * 0.30f
      } else if (appScreen == AppScreen.LOBBY) {
        previewOrbitRad += dt * 0.24f
      }

      if (!menuOpen && appScreen == AppScreen.GAMEPLAY) {
        if (phase == GamePhase.AIMING && !isAiTurnLocked()) {
          turnTimeRemaining -= dt
          if (turnTimeRemaining <= 0f) {
            turnTimeRemaining = 0f
            dragActive = false
            dragCanceling = false
            aimVisual = AimVisualState()
            if (mode != GameMode.TRICK_CHALLENGE) {
              currentPlayer = (currentPlayer + 1) % playerCountLocked()
              resetStrikerForTurnLocked()
              timeoutToast = "TIME UP! TURN PASSED"
              if (isAiTurnLocked()) {
                startAiTurnLocked()
              }
            } else {
              turnTimeRemaining = 40f
            }
            notifyHudLocked()
          } else {
            notifyHudLocked()
          }
        } else if (phase == GamePhase.AI_THINKING && aiPlan != null) {
          stepAiThinkingLocked(dt)
        } else if (phase == GamePhase.REWINDING) {
          stepRewindAnimationLocked(dt)
        } else if (phase == GamePhase.SIMULATING) {
          if (!stepPhysicsLocked(dt)) {
            resolveTurnEndLocked()
          }
        }
      }

      updateSparksLocked(dt)
      val s = striker
      return RenderSceneSnapshot(
        pieces =
          pieces.map {
            RenderPieceSnapshot(
              id = it.id,
              type = it.type,
              x = it.x,
              y = it.y,
              z = it.z,
              height = it.height,
              tiltX = it.tiltX,
              tiltZ = it.tiltZ,
              spinY = it.spinY,
              visible = it.active || it.pocketing,
            )
          },
        sparks =
          sparkPool
            .filter { it.active }
            .map {
              val t = 1f - it.life / it.maxLife
              RenderSparkSnapshot(it.x, it.y, it.z, 0.4f + t * 0.9f, t, it.r, it.g, it.b)
            },
        arenaElements = arenaElements.toList(),
        aim = aimVisual,
        invalidRingVisible = !strikerValid && phase == GamePhase.AIMING && !menuOpen,
        strikerX = s?.x ?: 0f,
        strikerZ = s?.z ?: CarromConfig.BASELINE_Z,
        selectedStrikerId = selectedStrikerId,
        selectedBoardId = selectedBoardId,
        cameraMode = cameraMode,
        gameMode = mode,
        currentPlayer = currentPlayer,
        appScreen = appScreen,
        previewOrbitRad = previewOrbitRad,
        aiPlanning = phase == GamePhase.AI_THINKING,
      )
    }.also {
      if (timeoutToast != null) onToast(timeoutToast!!, ToastTone.FOUL)
    }
  }

  private fun stepAiThinkingLocked(dt: Float) {
    val plan = aiPlan ?: return
    val s = striker ?: return
    aiTimer -= dt
    val elapsed = (aiTotalDuration - aiTimer).coerceIn(0f, aiTotalDuration)
    val slidePhase = (elapsed / 0.65f).coerceIn(0f, 1f)
    if (slidePhase < 1f) {
      s.x += (plan.sx - s.x) * min(1f, 6.5f * dt)
      sliderValue = s.x
    } else {
      s.x = plan.sx
      sliderValue = plan.sx
    }

    if (elapsed >= 0.48f && aiTimer > 0.05f) {
      val aimPhase = ((elapsed - 0.48f) / (aiTotalDuration - 0.60f)).coerceIn(0f, 1f)
      // Realistic human aim sweep oscillation that settles onto the true target angle
      val wobble = sin((1f - aimPhase) * 9.5f) * (1f - aimPhase) * 0.22f
      val c = cos(wobble)
      val sn = sin(wobble)
      val curDirX = plan.dirX * c - plan.dirZ * sn
      val curDirZ = plan.dirX * sn + plan.dirZ * c
      val pRatio = min(1f, plan.speed / CarromConfig.MAX_SHOT_SPEED) * (0.4f + 0.6f * aimPhase)
      val pullLen = pRatio * 1.95f
      aimVisual =
        buildAimVisualLocked(
          s.x,
          s.z,
          curDirX,
          curDirZ,
          pRatio,
          s.x - curDirX * pullLen,
          s.z - curDirZ * pullLen,
        )
    }

    if (aiTimer <= 0f) {
      aimVisual = AimVisualState()
      s.x = plan.sx
      s.vx = plan.dirX * plan.speed
      s.vz = plan.dirZ * plan.speed
      aiLastTargetId = plan.targetPieceId
      sound.playShoot(plan.speed / CarromConfig.MAX_SHOT_SPEED)
      aiPlan = null
      phase = GamePhase.SIMULATING
      notifyHudLocked()
    }
  }

  private fun stepRewindAnimationLocked(dt: Float) {
    val snap = lastTurnSnapshot
    if (snap == null) {
      phase = GamePhase.AIMING
      return
    }
    rewindProgress = min(1f, rewindProgress + dt / 0.58f)
    val ease = 1f - (1f - rewindProgress).pow(3)
    for (p in pieces) {
      val from = rewindFromStates[p.id] ?: continue
      val to = snap.pieces.firstOrNull { it.id == p.id } ?: continue
      p.x = from.x + (to.x - from.x) * ease
      p.y = from.y + (to.y - from.y) * ease
      p.z = from.z + (to.z - from.z) * ease
      p.tiltX = from.tiltX + (to.tiltX - from.tiltX) * ease
      p.tiltZ = from.tiltZ + (to.tiltZ - from.tiltZ) * ease
      p.spinY = from.spinY + (to.spinY - from.spinY) * ease
      p.active = to.active
      p.pocketing = false
    }
    if (rewindProgress >= 1f) {
      for (i in scores.indices) {
        scores[i] = snap.scores[i]
        pocketedCounts[i] = snap.pocketedCounts[i]
      }
      queenState = snap.queenState
      queenHolder = snap.queenHolder
      currentPlayer = snap.currentPlayer
      challengeShotsLeft = snap.challengeShotsLeft
      sliderValue = snap.sliderValue
      turnPocketedAny = false
      turnPocketedQueen = false
      turnFoul = false
      strikerValid = checkStrikerValidLocked()
      turnTimeRemaining =
        if (mode == GameMode.TRICK_CHALLENGE) 40f else CarromConfig.TURN_DURATION_SEC
      lastTurnSnapshot = null
      phase = GamePhase.AIMING
      notifyHudLocked()
    }
  }

  private fun captureTurnSnapshotLocked() {
    lastTurnSnapshot =
      TurnSnapshot(
        pieces =
          pieces.map {
            PieceStateCopy(it.id, it.x, it.y, it.z, it.active, it.pocketing, it.tiltX, it.tiltZ, it.spinY)
          },
        scores = scores.copyOf(),
        pocketedCounts = pocketedCounts.copyOf(),
        queenState = queenState,
        queenHolder = queenHolder,
        currentPlayer = currentPlayer,
        challengeShotsLeft = challengeShotsLeft,
        sliderValue = sliderValue,
      )
  }

  private fun setupRackLocked(includeChaos: Boolean) {
    pieces.clear()
    arenaElements.clear()
    var nextId = 1
    fun addPiece(type: PieceType, x: Float, z: Float): Piece {
      val isStriker = type == PieceType.STRIKER
      val p =
        Piece(
          id = nextId++,
          type = type,
          radius = if (isStriker) CarromConfig.STRIKER_R else CarromConfig.COIN_R,
          height = if (isStriker) CarromConfig.STRIKER_H else CarromConfig.COIN_H,
          mass = if (isStriker) 2.5f else 1.0f,
          x = x,
          z = z,
        )
      pieces.add(p)
      return p
    }

    addPiece(PieceType.QUEEN, 0f, 0f)
    val d1 = CarromConfig.COIN_R * 2.04f
    for (k in 0 until 6) {
      val a = (k * PI / 3.0).toFloat()
      addPiece(if (k % 2 == 0) PieceType.WHITE else PieceType.BLACK, cos(a) * d1, sin(a) * d1)
    }
    val dCorner = CarromConfig.COIN_R * 4.08f
    val dMid = CarromConfig.COIN_R * 2.04f * sqrt(3f)
    for (k in 0 until 6) {
      val aC = (k * PI / 3.0).toFloat()
      addPiece(PieceType.WHITE, cos(aC) * dCorner, sin(aC) * dCorner)
      val aM = aC + (PI / 6.0).toFloat()
      addPiece(PieceType.BLACK, cos(aM) * dMid, sin(aM) * dMid)
    }
    striker = addPiece(PieceType.STRIKER, 0f, CarromConfig.BASELINE_Z)

    if (includeChaos) {
      addChaosObstaclesLocked()
    }
  }

  private fun setupChallengeRackLocked(ch: TrickChallenge) {
    pieces.clear()
    arenaElements.clear()
    var nextId = 1
    for (c in ch.coins) {
      pieces.add(
        Piece(
          id = nextId++,
          type = c.type,
          radius = CarromConfig.COIN_R,
          height = CarromConfig.COIN_H,
          mass = 1.0f,
          x = c.x,
          z = c.z,
        )
      )
    }
    striker =
      Piece(
        id = nextId,
        type = PieceType.STRIKER,
        radius = CarromConfig.STRIKER_R,
        height = CarromConfig.STRIKER_H,
        mass = 2.5f,
        x = 0f,
        z = CarromConfig.BASELINE_Z,
      ).also { pieces.add(it) }

    if (ch.hasChaosObstacles) {
      addChaosObstaclesLocked()
    }
  }

  private fun addChaosObstaclesLocked() {
    arenaElements.add(ArenaElement(1, ArenaElementType.BUMPER, -1.95f, 0f, 0.34f))
    arenaElements.add(ArenaElement(2, ArenaElementType.BUMPER, 1.95f, 0f, 0.34f))
    arenaElements.add(ArenaElement(3, ArenaElementType.SPEED_PAD, 0f, -1.95f, 0.56f))
    arenaElements.add(ArenaElement(4, ArenaElementType.SPEED_PAD, 0f, 1.95f, 0.56f))
    arenaElements.add(ArenaElement(5, ArenaElementType.DEFLECTOR, -2.25f, -1.65f, 0.24f))
    arenaElements.add(ArenaElement(6, ArenaElementType.DEFLECTOR, 2.25f, 1.65f, 0.24f))
  }

  private fun playerCountLocked(): Int =
    if (mode == GameMode.DESI_ADDA_2V2 || mode == GameMode.DESI_ADDA_FFA) 4 else 2

  private fun isAiTurnLocked(): Boolean =
    (mode == GameMode.CLASSIC_1V1 || mode == GameMode.CHAOS_ARENA) && currentPlayer == 1

  private fun baselineAngleForPlayerLocked(playerIdx: Int): Float =
    when (mode) {
      GameMode.DESI_ADDA_2V2, GameMode.DESI_ADDA_FFA -> (playerIdx % 4) * (PI / 2.0).toFloat()
      else -> if (playerIdx == 1) PI.toFloat() else 0f
    }

  private fun baselineCoordsLocked(playerIdx: Int, slider: Float): Pair<Float, Float> {
    val ang = baselineAngleForPlayerLocked(playerIdx)
    val sinA = sin(ang)
    val cosA = cos(ang)
    val baseX = sinA * CarromConfig.BASELINE_Z
    val baseZ = cosA * CarromConfig.BASELINE_Z
    val rightX = cosA
    val rightZ = -sinA
    return (baseX + rightX * slider) to (baseZ + rightZ * slider)
  }

  private fun findValidStrikerSliderLocked(playerIdx: Int, preferredSlider: Float): Float {
    fun isFree(sVal: Float): Boolean {
      val (tx, tz) = baselineCoordsLocked(playerIdx, sVal)
      for (p in pieces) {
        if (!p.active || p.pocketing || p === striker) continue
        val dx = p.x - tx
        val dz = p.z - tz
        val minD = p.radius + CarromConfig.STRIKER_R + 0.015f
        if (dx * dx + dz * dz < minD * minD) return false
      }
      for (obs in arenaElements) {
        if (obs.type == ArenaElementType.SPEED_PAD) continue
        val dx = obs.x - tx
        val dz = obs.z - tz
        val minD = obs.radius + CarromConfig.STRIKER_R + 0.015f
        if (dx * dx + dz * dz < minD * minD) return false
      }
      return true
    }
    if (isFree(preferredSlider)) return preferredSlider
    var step = 0.08f
    while (step <= CarromConfig.BASELINE_LIMIT * 2f) {
      if (preferredSlider + step <= CarromConfig.BASELINE_LIMIT && isFree(preferredSlider + step)) {
        return preferredSlider + step
      }
      if (preferredSlider - step >= -CarromConfig.BASELINE_LIMIT && isFree(preferredSlider - step)) {
        return preferredSlider - step
      }
      step += 0.08f
    }
    return preferredSlider
  }

  private fun checkStrikerValidLocked(): Boolean {
    val s = striker ?: return true
    for (p in pieces) {
      if (!p.active || p.pocketing || p === s) continue
      val dx = p.x - s.x
      val dz = p.z - s.z
      val minD = p.radius + s.radius + 0.01f
      if (dx * dx + dz * dz < minD * minD) return false
    }
    for (obs in arenaElements) {
      if (obs.type == ArenaElementType.SPEED_PAD) continue
      val dx = obs.x - s.x
      val dz = obs.z - s.z
      val minD = obs.radius + s.radius + 0.01f
      if (dx * dx + dz * dz < minD * minD) return false
    }
    return true
  }

  private fun resetStrikerForTurnLocked() {
    val s = striker ?: return
    val bestSlider = findValidStrikerSliderLocked(currentPlayer, 0f)
    val (wx, wz) = baselineCoordsLocked(currentPlayer, bestSlider)
    s.active = true
    s.pocketing = false
    s.x = wx
    s.y = 0f
    s.z = wz
    s.vx = 0f
    s.vy = 0f
    s.vz = 0f
    s.tiltX = 0f
    s.tiltZ = 0f
    s.spinY = 0f
    s.speedPadCooldown = 0f
    sliderValue = bestSlider
    strikerValid = checkStrikerValidLocked()
    turnTimeRemaining =
      if (mode == GameMode.TRICK_CHALLENGE) 40f else CarromConfig.TURN_DURATION_SEC
  }

  private fun startAiTurnLocked() {
    phase = GamePhase.AI_THINKING
    aiTotalDuration = 1.55f
    aiTimer = aiTotalDuration
    aiPlan =
      TacticalAi.computeShot(
        pieces = pieces,
        striker = striker,
        arenaElements = arenaElements,
        queenState = queenState,
        queenHolder = queenHolder,
        aiPlayer = currentPlayer,
        difficulty = aiDifficulty,
        lastTargetPieceId = aiLastTargetId,
      )
  }

  private fun returnQueenToCenterLocked() {
    val q = pieces.firstOrNull { it.type == PieceType.QUEEN } ?: return
    var spotX = 0f
    var spotZ = 0f
    outer@ for (ring in 0..10) {
      val rad = ring * 0.24f
      val samples = if (ring == 0) 1 else 14
      for (s in 0 until samples) {
        val a = (s.toFloat() / samples) * (PI * 2.0).toFloat()
        val tx = cos(a) * rad
        val tz = sin(a) * rad
        var ok = true
        for (p in pieces) {
          if (!p.active || p.pocketing || p === striker || p === q) continue
          val dx = p.x - tx
          val dz = p.z - tz
          val minD = p.radius + CarromConfig.COIN_R + 0.03f
          if (dx * dx + dz * dz < minD * minD) {
            ok = false
            break
          }
        }
        if (ok) {
          spotX = tx
          spotZ = tz
          break@outer
        }
      }
    }
    q.active = true
    q.pocketing = false
    q.x = spotX
    q.y = 0f
    q.z = spotZ
    q.vx = 0f
    q.vy = 0f
    q.vz = 0f
    q.tiltX = 0f
    q.tiltZ = 0f
    queenState = QueenState.ON_BOARD
    queenHolder = -1
  }

  private fun handlePiecePocketedLocked(p: Piece) {
    p.active = false
    p.pocketing = false
    val sparkColor =
      when (p.type) {
        PieceType.QUEEN -> Triple(1f, 0.30f, 0.43f)
        PieceType.STRIKER -> Triple(1f, 0.23f, 0.36f)
        else -> Triple(1f, 0.82f, 0.40f)
      }
    spawnSparksLocked(p.x, -0.2f, p.z, sparkColor.first, sparkColor.second, sparkColor.third, 12)
    if (p.type == PieceType.STRIKER) {
      sound.playPocket(true)
      sound.playFoul()
      onFoulFlash()
      turnFoul = true
      return
    }
    sound.playPocket(false)
    val cp = currentPlayer
    if (p.type == PieceType.QUEEN) {
      turnPocketedQueen = true
      queenState = QueenState.PENDING_COVER
      queenHolder = cp
      onToast("QUEEN POCKETED! COVER IT!", ToastTone.QUEEN)
    } else {
      val pts = if (p.type == PieceType.WHITE) 10 else 5
      scores[cp] += pts
      pocketedCounts[cp] += 1
      turnPocketedAny = true
      onToast("+$pts PTS!", ToastTone.GOOD)
    }
    notifyHudLocked()
  }

  private fun resolveTurnEndLocked() {
    val cp = currentPlayer
    val pCount = playerCountLocked()
    if (turnFoul) {
      scores[cp] = max(0, scores[cp] - 10)
      if (queenState == QueenState.PENDING_COVER && queenHolder == cp) {
        returnQueenToCenterLocked()
        onToast("FOUL! -10 & QUEEN RETURNED", ToastTone.FOUL)
      } else {
        onToast("STRIKER FOUL! -10 PTS", ToastTone.FOUL)
      }
      if (mode != GameMode.TRICK_CHALLENGE) {
        currentPlayer = (cp + 1) % pCount
      }
    } else if (queenState == QueenState.PENDING_COVER && queenHolder == cp) {
      if (turnPocketedAny) {
        queenState = QueenState.COVERED
        scores[cp] += 50
        sound.playQueenCover()
        onToast("QUEEN COVERED! +50 PTS", ToastTone.QUEEN)
      } else if (turnPocketedQueen) {
        val rem = pieces.count { it.active && (it.type == PieceType.WHITE || it.type == PieceType.BLACK) }
        if (rem == 0) {
          queenState = QueenState.COVERED
          scores[cp] += 50
          sound.playQueenCover()
        } else {
          onToast("COVER THE QUEEN NOW!", ToastTone.QUEEN)
        }
      } else {
        returnQueenToCenterLocked()
        onToast("QUEEN NOT COVERED!", ToastTone.FOUL)
        if (mode != GameMode.TRICK_CHALLENGE) {
          currentPlayer = (cp + 1) % pCount
        }
      }
    } else if (!turnPocketedAny && mode != GameMode.TRICK_CHALLENGE) {
      currentPlayer = (cp + 1) % pCount
    }

    turnPocketedAny = false
    turnPocketedQueen = false
    turnFoul = false

    if (checkMatchOverLocked()) return
    resetStrikerForTurnLocked()
    if (isAiTurnLocked()) {
      startAiTurnLocked()
    } else {
      phase = GamePhase.AIMING
    }
    notifyHudLocked()
  }

  private fun checkMatchOverLocked(): Boolean {
    val remainingCoins = pieces.count { it.active && (it.type == PieceType.WHITE || it.type == PieceType.BLACK) }
    val queenOnBoard = pieces.any { it.active && it.type == PieceType.QUEEN }

    if (mode == GameMode.TRICK_CHALLENGE) {
      val cleared = remainingCoins == 0 && !queenOnBoard
      val outOfShots = challengeShotsLeft <= 0 && !cleared
      if (cleared || outOfShots) {
        if (cleared && queenState == QueenState.PENDING_COVER && queenHolder >= 0) {
          scores[queenHolder] += 50
          queenState = QueenState.COVERED
        }
        phase = GamePhase.GAMEOVER
        hasActiveMatch = false
        if (cleared) {
          winnerTitle = "CHALLENGE CLEARED!"
          winnerSubtitle = "Level #$challengeLevel: $challengeTitle (+$challengeRewardCoins Coins)"
          sound.playQueenCover()
        } else {
          winnerTitle = "OUT OF SHOTS!"
          winnerSubtitle = "Use Rewind Shot or retry Level #$challengeLevel."
          sound.playFoul()
        }
        notifyHudLocked()
        onMatchFinished(scores[0], cleared, mode, challengeLevel)
        return true
      }
      return false
    }

    if (remainingCoins == 0 && !queenOnBoard) {
      if (queenState == QueenState.PENDING_COVER && queenHolder >= 0) {
        scores[queenHolder] += 50
        queenState = QueenState.COVERED
      }
      phase = GamePhase.GAMEOVER
      hasActiveMatch = false

      var p1Won = false
      var p1ScoreOut = scores[0]
      when (mode) {
        GameMode.DESI_ADDA_2V2 -> {
          val teamA = scores[0] + scores[2]
          val teamB = scores[1] + scores[3]
          p1ScoreOut = teamA
          p1Won = teamA > teamB
          when {
            teamA > teamB -> {
              winnerTitle = "TEAM A WINS!"
              winnerSubtitle = "Players 1 & 3 ruled the Desi Adda!"
            }
            teamB > teamA -> {
              winnerTitle = "TEAM B WINS!"
              winnerSubtitle = "Players 2 & 4 took the crown!"
            }
            else -> {
              winnerTitle = "TEAM DRAW!"
              winnerSubtitle = "Honors even on the royal board!"
            }
          }
        }
        GameMode.DESI_ADDA_FFA -> {
          val maxScore = scores.maxOrNull() ?: 0
          val leaders = scores.indices.filter { scores[it] == maxScore }
          p1Won = scores[0] == maxScore && leaders.size == 1
          if (leaders.size == 1) {
            winnerTitle = "PLAYER ${leaders.first() + 1} WINS!"
            winnerSubtitle = "Champion of the 4-Player Desi Adda!"
          } else {
            winnerTitle = "SHARED CROWN!"
            winnerSubtitle = "Top players tied at $maxScore pts!"
          }
        }
        else -> {
          val s1 = scores[0]
          val s2 = scores[1]
          p1Won = s1 > s2
          when {
            s1 > s2 -> {
              winnerTitle = "YOU WIN!"
              winnerSubtitle = "Royal precision on the board."
            }
            s2 > s1 -> {
              winnerTitle = "AI WINS!"
              winnerSubtitle = "Tactical AI (${aiDifficulty.label}) outscored you."
            }
            else -> {
              winnerTitle = "DRAW MATCH!"
              winnerSubtitle = "Dead even score!"
            }
          }
        }
      }
      sound.playQueenCover()
      notifyHudLocked()
      onMatchFinished(p1ScoreOut, p1Won, mode, 0)
      return true
    }
    return false
  }

  private fun stepPhysicsLocked(dt: Float): Boolean {
    val substeps = 5
    val h = dt / substeps
    var anyMoving = false
    for (step in 0 until substeps) {
      for (p in pieces) {
        if (!p.active) continue
        if (p.speedPadCooldown > 0f) p.speedPadCooldown = max(0f, p.speedPadCooldown - h)
        if (p.pocketing) {
          anyMoving = true
          p.vy -= 26f * h
          p.y += p.vy * h
          p.x += (p.pocketTargetX - p.x) * 8f * h
          p.z += (p.pocketTargetZ - p.z) * 8f * h
          p.vx *= 0.92f
          p.vz *= 0.92f
          p.x += p.vx * h
          p.z += p.vz * h
          p.tiltX += p.vz * 1.4f * h
          p.tiltZ -= p.vx * 1.4f * h
          if (p.y <= -0.72f) handlePiecePocketedLocked(p)
          continue
        }
        p.x += p.vx * h
        p.z += p.vz * h

        for (pk in CarromConfig.POCKETS) {
          val dx = p.x - pk.x
          val dz = p.z - pk.z
          val capR = CarromConfig.POCKET_R - 0.015f
          if (dx * dx + dz * dz < capR * capR) {
            p.pocketing = true
            p.pocketTargetX = pk.x
            p.pocketTargetZ = pk.z
            p.vy = -1.2f
            break
          }
        }
        if (p.pocketing) continue

        // Chaos Arena Obstacles & Speed Pads
        for (obs in arenaElements) {
          val dx = p.x - obs.x
          val dz = p.z - obs.z
          val distSq = dx * dx + dz * dz
          when (obs.type) {
            ArenaElementType.SPEED_PAD -> {
              val spd = hypot(p.vx, p.vz)
              if (distSq < obs.radius * obs.radius && spd > 0.6f && p.speedPadCooldown <= 0f) {
                val boost = min(CarromConfig.MAX_SHOT_SPEED * 1.15f, spd * 1.44f) / spd
                p.vx *= boost
                p.vz *= boost
                p.speedPadCooldown = 0.36f
                sound.playSpeedPad()
                spawnSparksLocked(p.x, 0.06f, p.z, 0f, 0.96f, 0.85f, 8)
              }
            }
            ArenaElementType.BUMPER, ArenaElementType.DEFLECTOR -> {
              val minD = p.radius + obs.radius
              if (distSq < minD * minD) {
                val d = sqrt(distSq).coerceAtLeast(1e-4f)
                val nx = dx / d
                val nz = dz / d
                p.x = obs.x + nx * minD
                p.z = obs.z + nz * minD
                val vn = p.vx * nx + p.vz * nz
                if (vn < 0f) {
                  val rest = if (obs.type == ArenaElementType.BUMPER) 1.28f else 0.88f
                  p.vx -= (1f + rest) * vn * nx
                  p.vz -= (1f + rest) * vn * nz
                  if (obs.type == ArenaElementType.BUMPER) {
                    val curSpd = hypot(p.vx, p.vz)
                    if (curSpd < 5.5f) {
                      p.vx += nx * 4.2f
                      p.vz += nz * 4.2f
                    }
                    if (step == 0) {
                      sound.playBumper(-vn)
                      spawnSparksLocked(obs.x + nx * obs.radius, 0.08f, obs.z + nz * obs.radius, 1f, 0.78f, 0.2f, 10)
                    }
                  } else if (step == 0 && -vn > 0.6f) {
                    sound.playRail(-vn)
                  }
                  anyMoving = true
                }
              }
            }
          }
        }

        val limit = CarromConfig.BOARD_HALF - p.radius
        if (p.x < -limit) {
          p.x = -limit
          val imp = abs(p.vx)
          p.vx = imp * CarromConfig.RESTITUTION_RAIL
          if (step == 0 && imp > 0.7f) sound.playRail(imp)
        } else if (p.x > limit) {
          p.x = limit
          val imp = abs(p.vx)
          p.vx = -imp * CarromConfig.RESTITUTION_RAIL
          if (step == 0 && imp > 0.7f) sound.playRail(imp)
        }
        if (p.z < -limit) {
          p.z = -limit
          val imp = abs(p.vz)
          p.vz = imp * CarromConfig.RESTITUTION_RAIL
          if (step == 0 && imp > 0.7f) sound.playRail(imp)
        } else if (p.z > limit) {
          p.z = limit
          val imp = abs(p.vz)
          p.vz = -imp * CarromConfig.RESTITUTION_RAIL
          if (step == 0 && imp > 0.7f) sound.playRail(imp)
        }

        val spd = hypot(p.vx, p.vz)
        if (spd > CarromConfig.STOP_SPEED) {
          val newSpd =
            max(0f, spd * CarromConfig.DAMPING_PER_SEC.pow(h * 60f) - CarromConfig.FRICTION_COEFF * h)
          val ratio = newSpd / spd
          p.vx *= ratio
          p.vz *= ratio
          p.spinY += (p.vx - p.vz) * 0.25f * h
          if (newSpd > CarromConfig.STOP_SPEED) anyMoving = true
          else {
            p.vx = 0f
            p.vz = 0f
          }
        } else {
          p.vx = 0f
          p.vz = 0f
        }
      }

      for (i in pieces.indices) {
        val a = pieces[i]
        if (!a.active || a.pocketing) continue
        for (j in i + 1 until pieces.size) {
          val b = pieces[j]
          if (!b.active || b.pocketing) continue
          val dx = b.x - a.x
          val dz = b.z - a.z
          val minDist = a.radius + b.radius
          val d2 = dx * dx + dz * dz
          if (d2 < minDist * minDist) {
            val dist = sqrt(d2).coerceAtLeast(1e-4f)
            val nx = dx / dist
            val nz = dz / dist
            val ov = minDist - dist
            val invA = 1f / a.mass
            val invB = 1f / b.mass
            val invSum = invA + invB
            a.x -= nx * ov * (invA / invSum)
            a.z -= nz * ov * (invA / invSum)
            b.x += nx * ov * (invB / invSum)
            b.z += nz * ov * (invB / invSum)
            val vn = (b.vx - a.vx) * nx + (b.vz - a.vz) * nz
            if (vn < 0f) {
              val jImp = -(1f + CarromConfig.RESTITUTION_PIECE) * vn / invSum
              a.vx -= jImp * nx * invA
              a.vz -= jImp * nz * invA
              b.vx += jImp * nx * invB
              b.vz += jImp * nz * invB
              val imp = -vn
              if (imp > 0.45f) sound.playClack(imp)
              if (imp > 5.8f) {
                spawnSparksLocked(
                  a.x + nx * a.radius,
                  0f,
                  a.z + nz * a.radius,
                  1f,
                  0.82f,
                  0.4f,
                  min(10, (imp * 0.7f).toInt()),
                )
              }
              anyMoving = true
            }
          }
        }
      }
    }
    return anyMoving
  }

  private fun buildAimVisualLocked(
    ox: Float,
    oz: Float,
    dirX: Float,
    dirZ: Float,
    power: Float,
    pullX: Float,
    pullZ: Float,
  ): AimVisualState {
    var bestT = 20f
    var hitPiece: Piece? = null
    val limit = CarromConfig.BOARD_HALF - CarromConfig.STRIKER_R
    if (dirX > 1e-4f) bestT = min(bestT, (limit - ox) / dirX)
    else if (dirX < -1e-4f) bestT = min(bestT, (-limit - ox) / dirX)
    if (dirZ > 1e-4f) bestT = min(bestT, (limit - oz) / dirZ)
    else if (dirZ < -1e-4f) bestT = min(bestT, (-limit - oz) / dirZ)

    for (p in pieces) {
      if (!p.active || p.pocketing || p === striker) continue
      val rx = ox - p.x
      val rz = oz - p.z
      val sumR = CarromConfig.STRIKER_R + p.radius
      val b = rx * dirX + rz * dirZ
      val c = rx * rx + rz * rz - sumR * sumR
      val disc = b * b - c
      if (disc >= 0f) {
        val t = -b - sqrt(disc)
        if (t > 0.01f && t < bestT) {
          bestT = t
          hitPiece = p
        }
      }
    }
    val len = max(0.1f, bestT)
    val ex = ox + dirX * len
    val ez = oz + dirZ * len
    var hasHit = false
    var tx = 0f
    var tz = 0f
    var ddx = 0f
    var ddz = 0f
    if (hitPiece != null) {
      hasHit = true
      tx = hitPiece.x
      tz = hitPiece.z
      val dx = hitPiece.x - ex
      val dz = hitPiece.z - ez
      val d = hypot(dx, dz).coerceAtLeast(1e-4f)
      ddx = dx / d
      ddz = dz / d
    }
    return AimVisualState(
      visible = true,
      dirX = dirX,
      dirZ = dirZ,
      power = power,
      strikerX = ox,
      strikerZ = oz,
      beamLength = len,
      impactX = ex,
      impactZ = ez,
      hasTargetHit = hasHit,
      targetX = tx,
      targetZ = tz,
      deflectDirX = ddx,
      deflectDirZ = ddz,
      pullX = pullX,
      pullZ = pullZ,
    )
  }

  private fun spawnSparksLocked(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int) {
    var spawned = 0
    for (sp in sparkPool) {
      if (sp.active) continue
      sp.active = true
      sp.life = 0f
      sp.maxLife = 0.25f + Random.nextFloat() * 0.2f
      val ang = Random.nextFloat() * (PI * 2.0).toFloat()
      val spd = 1.8f + Random.nextFloat() * 4.5f
      sp.vx = cos(ang) * spd
      sp.vy = 1.6f + Random.nextFloat() * 3.4f
      sp.vz = sin(ang) * spd
      sp.x = x
      sp.y = y + 0.08f
      sp.z = z
      sp.r = r
      sp.g = g
      sp.b = b
      spawned++
      if (spawned >= count) break
    }
  }

  private fun updateSparksLocked(dt: Float) {
    for (sp in sparkPool) {
      if (!sp.active) continue
      sp.life += dt
      if (sp.life >= sp.maxLife) {
        sp.active = false
        continue
      }
      sp.vy -= 14f * dt
      sp.x += sp.vx * dt
      sp.y += sp.vy * dt
      sp.z += sp.vz * dt
    }
  }

  private fun clearSparksLocked() {
    for (sp in sparkPool) sp.active = false
  }

  private fun notifyHudLocked() {
    val powerPct = if (dragActive && dragPower >= 0.05f) (dragPower * 100f).toInt() else null
    val remW = pieces.count { it.active && it.type == PieceType.WHITE }
    val remB = pieces.count { it.active && it.type == PieceType.BLACK }
    val maxTurn = if (mode == GameMode.TRICK_CHALLENGE) 40f else CarromConfig.TURN_DURATION_SEC
    val canRewind =
      rewindsRemaining > 0 &&
        lastTurnSnapshot != null &&
        !menuOpen &&
        phase != GamePhase.REWINDING &&
        phase != GamePhase.GAMEOVER &&
        phase != GamePhase.AI_THINKING

    onHudUpdated(
      UiHudState(
        appScreen = appScreen,
        mode = mode,
        phase = phase,
        cameraMode = cameraMode,
        currentPlayer = currentPlayer,
        scores = scores.toList(),
        coinsPocketed = pocketedCounts.toList(),
        remainingWhite = remW,
        remainingBlack = remB,
        queenState = queenState,
        queenHolder = queenHolder,
        strikerSliderValue = sliderValue,
        strikerValid = strikerValid,
        turnTimeRemaining = turnTimeRemaining,
        turnTimeProgress = (turnTimeRemaining / maxTurn).coerceIn(0f, 1f),
        rewindsRemaining = rewindsRemaining,
        canRewindNow = canRewind,
        challengeLevel = challengeLevel,
        challengeTitle = challengeTitle,
        challengeShotsLeft = challengeShotsLeft,
        challengeRewardCoins = challengeRewardCoins,
        soundEnabled = sound.enabled,
        musicEnabled = sound.musicEnabled,
        cameraSensitivity = cameraSensitivity,
        aiDifficulty = aiDifficulty,
        hintPowerPercent = powerPct,
        isCancelingDrag = dragCanceling,
        hasActiveMatch = hasActiveMatch,
        menuOpen = menuOpen,
        winnerTitle = winnerTitle,
        winnerSubtitle = winnerSubtitle,
        selectedStrikerId = selectedStrikerId,
        selectedBoardId = selectedBoardId,
      )
    )
  }
}
