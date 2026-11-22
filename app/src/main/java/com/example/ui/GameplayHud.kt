package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CameraMode
import com.example.game.CarromConfig
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.QueenState
import com.example.game.ToastTone
import com.example.game.UiHudState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonQueen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private val playerAvatars = listOf("👑", "🦁", "🦅", "⚡", "🔥", "🎯")

@Composable
fun GameplayHudOverlay(
  state: UiHudState,
  onToggleCamera: () -> Unit,
  onToggleSound: () -> Unit,
  onOpenLobby: () -> Unit,
  onRewindShot: () -> Unit,
  onTriggerEmote: (String) -> Unit,
  onSliderChanged: (Float) -> Unit,
) {
  var emotesOpen by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    // Red Foul Vignette Overlay
    AnimatedVisibility(
      visible = state.foulFlash,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.radialGradient(
                colors = listOf(Color.Transparent, CrimsonQueen.copy(alpha = 0.50f)),
                radius = 900f,
              )
            )
      )
    }

    // Top Esports Scoreboard Header
    Column(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ProTopHeaderBar(
        state = state,
        onToggleCamera = onToggleCamera,
        onToggleSound = onToggleSound,
        onOpenLobby = onOpenLobby,
      )

      BoardCoinCounterStrip(state = state)

      AnimatedVisibility(
        visible = state.queenState == QueenState.PENDING_COVER,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
      ) {
        Surface(
          shape = CircleShape,
          color = CrimsonQueen.copy(alpha = 0.95f),
          border = BorderStroke(1.2.dp, Color(0xFFFFD0DC)),
          shadowElevation = 10.dp,
        ) {
          Text(
            text = stringResource(R.string.queen_pending_banner),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
          )
        }
      }
    }

    // Floating Emote Reaction Bubble
    AnimatedVisibility(
      visible = state.activeEmote != null,
      enter = fadeIn() + scaleIn(initialScale = 0.7f),
      exit = fadeOut() + scaleOut(targetScale = 0.7f),
      modifier =
        Modifier.align(Alignment.TopStart)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(top = 132.dp, start = 20.dp),
    ) {
      MahoganyGlassCard(cornerRadius = 18.dp, glowAlpha = 0.32f, elevation = 12.dp) {
        Text(
          text = state.activeEmote?.emoji.orEmpty(),
          fontSize = 28.sp,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }
    }

    // Center Toast Notification
    AnimatedVisibility(
      visible = state.toastMessage != null,
      enter = fadeIn() + scaleIn(initialScale = 0.88f),
      exit = fadeOut() + scaleOut(targetScale = 0.88f),
      modifier = Modifier.align(Alignment.Center).padding(bottom = 88.dp),
    ) {
      val borderCol =
        when (state.toastTone) {
          ToastTone.GOOD -> CyanAccent
          ToastTone.QUEEN -> RoyalGold
          ToastTone.FOUL -> CrimsonQueen
          ToastTone.NEUTRAL -> RoyalGold.copy(alpha = 0.55f)
        }
      val textCol =
        when (state.toastTone) {
          ToastTone.GOOD -> Color(0xFF72EFDD)
          ToastTone.QUEEN -> RoyalGoldBright
          ToastTone.FOUL -> Color(0xFFFF8FA3)
          ToastTone.NEUTRAL -> TextPrimary
        }
      MahoganyGlassCard(
        cornerRadius = 16.dp,
        glowColor = borderCol,
        glowAlpha = 0.34f,
        borderBrush = Brush.linearGradient(listOf(borderCol, RoyalGoldBright)),
        elevation = 16.dp,
      ) {
        Text(
          text = state.toastMessage.orEmpty(),
          style = royalTextShadowStyle(color = textCol, fontSize = 15.sp, shadowColor = Color.Black),
          modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
      }
    }

    // Circular Power Gauge (when pulling slingshot)
    AnimatedVisibility(
      visible = state.hintPowerPercent != null,
      enter = fadeIn() + scaleIn(initialScale = 0.85f),
      exit = fadeOut() + scaleOut(targetScale = 0.85f),
      modifier =
        Modifier.align(Alignment.BottomStart)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(start = 16.dp, bottom = 114.dp),
    ) {
      CircularPowerGauge(powerPercent = state.hintPowerPercent ?: 0)
    }

    // Floating Right Controls: Rewind Shot + Quick Emotes
    Column(
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(end = 14.dp, bottom = 114.dp),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AnimatedVisibility(visible = emotesOpen, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        MahoganyGlassCard(cornerRadius = 18.dp, elevation = 12.dp) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf("🔥", "😎", "😱", "👏").forEach { emoji ->
              val interactionSource = remember { MutableInteractionSource() }
              val scale = rememberBounceScale(interactionSource)
              Surface(
                modifier =
                  Modifier.size(44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .clickable(
                      interactionSource = interactionSource,
                      indication = ripple(color = RoyalGold),
                    ) {
                      onTriggerEmote(emoji)
                      emotesOpen = false
                    }
                    .testTag("emote_btn_$emoji"),
                shape = CircleShape,
                color = ObsidianBg.copy(alpha = 0.72f),
              ) {
                Box(contentAlignment = Alignment.Center) { Text(text = emoji, fontSize = 20.sp) }
              }
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val rewindActive = state.canRewindNow
        MahoganyGlassCard(
          modifier =
            Modifier.height(48.dp)
              .alpha(if (rewindActive) 1f else 0.45f)
              .testTag("rewind_shot_button"),
          cornerRadius = 14.dp,
          glowColor = if (rewindActive) RoyalGold else Color.Transparent,
          glowAlpha = if (rewindActive) 0.30f else 0f,
          onClick = onRewindShot,
          enabled = rewindActive,
        ) {
          Row(
            modifier = Modifier.height(48.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Undo,
              contentDescription = stringResource(R.string.cd_rewind_button),
              tint = if (rewindActive) RoyalGoldBright else TextMuted,
              modifier = Modifier.size(18.dp),
            )
            Column {
              Text(
                text = "REWIND",
                color = if (rewindActive) RoyalGoldBright else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
              )
              Text(
                text = if (state.rewindsRemaining > 0) "1 LEFT" else "USED",
                color = TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        RoyalBounceIconButton(
          onClick = { emotesOpen = !emotesOpen },
          modifier = Modifier.size(48.dp).testTag("emote_toggle_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.EmojiEmotions,
            contentDescription = "Quick Emotes",
            tint = RoyalGoldBright,
          )
        }
      }
    }

    val isHumanAim =
      state.phase == GamePhase.AIMING &&
        !state.menuOpen &&
        ((state.mode != GameMode.CLASSIC_1V1 && state.mode != GameMode.CHAOS_ARENA) || state.currentPlayer == 0)

    BottomStrikerControlBar(
      state = state,
      enabled = isHumanAim,
      onSliderChange = onSliderChanged,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 14.dp, vertical = 10.dp),
    )
  }
}

@Composable
private fun ProTopHeaderBar(
  state: UiHudState,
  onToggleCamera: () -> Unit,
  onToggleSound: () -> Unit,
  onOpenLobby: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    val p1Glyph = playerAvatars.getOrElse(state.avatarId) { "👑" }
    val p1Score =
      if (state.mode == GameMode.DESI_ADDA_2V2) state.scores[0] + state.scores[2] else state.scores[0]
    val p2Score =
      if (state.mode == GameMode.DESI_ADDA_2V2) state.scores[1] + state.scores[3] else state.scores[1]

    val p1Active =
      state.phase != GamePhase.GAMEOVER &&
        if (state.mode == GameMode.DESI_ADDA_2V2) (state.currentPlayer == 0 || state.currentPlayer == 2)
        else state.currentPlayer == 0
    val p2Active =
      state.phase != GamePhase.GAMEOVER &&
        if (state.mode == GameMode.DESI_ADDA_2V2) (state.currentPlayer == 1 || state.currentPlayer == 3)
        else state.currentPlayer == 1

    EsportsScoreBanner(
      avatar = p1Glyph,
      name =
        when (state.mode) {
          GameMode.DESI_ADDA_2V2 -> "TEAM A"
          GameMode.DESI_ADDA_FFA -> "P1"
          else -> state.playerName
        },
      score = p1Score,
      isActive = p1Active,
      timerProgress = if (p1Active) state.turnTimeProgress else 0f,
      accent = CyanAccent,
      isLeft = true,
      modifier = Modifier.weight(1f).widthIn(max = 156.dp),
    )

    Spacer(modifier = Modifier.width(6.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val is3d = state.cameraMode == CameraMode.ANGLED_3D
        RoyalBounceIconButton(
          onClick = onToggleCamera,
          glowColor = if (is3d) RoyalGold else CyanAccent,
          modifier = Modifier.size(48.dp).testTag("camera_toggle_button"),
        ) {
          Icon(
            imageVector = if (is3d) Icons.Filled.ViewInAr else Icons.Filled.Videocam,
            contentDescription = stringResource(R.string.cd_camera_toggle),
            tint = if (is3d) RoyalGoldBright else CyanAccent,
          )
        }

        RoyalBounceIconButton(
          onClick = onToggleSound,
          modifier = Modifier.size(48.dp).testTag("sound_toggle_button"),
        ) {
          Icon(
            imageVector =
              if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp
              else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = stringResource(R.string.cd_sound_toggle),
            tint = if (state.soundEnabled) TextPrimary else TextMuted,
          )
        }

        RoyalBounceIconButton(
          onClick = onOpenLobby,
          modifier = Modifier.size(48.dp).testTag("menu_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = stringResource(R.string.cd_menu_button),
            tint = RoyalGoldBright,
          )
        }
      }

      val turnLabel =
        when {
          state.phase == GamePhase.REWINDING -> stringResource(R.string.turn_rewinding)
          state.phase == GamePhase.AI_THINKING -> stringResource(R.string.turn_ai_thinking)
          state.phase == GamePhase.SIMULATING -> stringResource(R.string.turn_shot_in_play)
          state.mode == GameMode.TRICK_CHALLENGE -> "SHOTS: ${state.challengeShotsLeft}"
          state.is4PlayerMode -> "PLAYER ${state.currentPlayer + 1}"
          state.currentPlayer == 0 -> stringResource(R.string.turn_your)
          else -> "AI TURN"
        }
      Surface(
        shape = CircleShape,
        color = Color(0xFF190804),
        border = BorderStroke(1.2.dp, RoyalBrushes.goldMetallic145),
        shadowElevation = 8.dp,
      ) {
        Text(
          text = turnLabel,
          color = RoyalGoldBright,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.6.sp,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    if (state.mode == GameMode.TRICK_CHALLENGE) {
      MahoganyGlassCard(
        modifier = Modifier.weight(1f).widthIn(max = 156.dp),
        cornerRadius = 16.dp,
        glowAlpha = 0.28f,
        elevation = 12.dp,
      ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
          Text(text = "LVL #${state.challengeLevel}", color = RoyalGoldBright, fontSize = 10.sp, fontWeight = FontWeight.Black)
          Text(
            text = state.challengeTitle,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(text = "+${state.challengeRewardCoins} Coins", color = EmeraldWin, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    } else {
      val rightAvatar = if (state.is4PlayerMode) "⚔️" else "🤖"
      val rightName =
        when (state.mode) {
          GameMode.DESI_ADDA_2V2 -> "TEAM B"
          GameMode.DESI_ADDA_FFA -> "P2:${state.scores[1]} P3:${state.scores[2]}"
          else -> "AI (${state.aiDifficulty.label})"
        }
      EsportsScoreBanner(
        avatar = rightAvatar,
        name = rightName,
        score = p2Score,
        isActive = p2Active || (state.mode == GameMode.DESI_ADDA_FFA && state.currentPlayer >= 1),
        timerProgress = if (p2Active || (state.mode == GameMode.DESI_ADDA_FFA && state.currentPlayer >= 1)) state.turnTimeProgress else 0f,
        accent = RoyalGold,
        isLeft = false,
        modifier = Modifier.weight(1f).widthIn(max = 156.dp),
      )
    }
  }
}

@Composable
private fun EsportsScoreBanner(
  avatar: String,
  name: String,
  score: Int,
  isActive: Boolean,
  timerProgress: Float,
  accent: Color,
  isLeft: Boolean,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(16.dp)
  val borderBrush =
    if (isActive) Brush.linearGradient(listOf(accent, RoyalGoldBright, accent))
    else Brush.linearGradient(listOf(RoyalGold.copy(alpha = 0.35f), Color(0xFF422112)))

  Box(
    modifier =
      modifier
        .shadow(if (isActive) 14.dp else 6.dp, shape, clip = false)
        .then(
          if (isActive) Modifier.royalGoldGlow(cornerRadius = 16.dp, glowColor = accent, glowAlpha = 0.36f)
          else Modifier
        )
        .clip(shape)
        .background(if (isActive) Color(0xFF6B4C04) else Color(0xFF140704), shape)
        .padding(bottom = 3.5.dp)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            if (isActive) listOf(Color(0xFF491E11), Color(0xFF230C06))
            else listOf(Color(0xFF2B120B), Color(0xFF160704))
          ),
          shape,
        )
        .border(BorderStroke(if (isActive) 1.8.dp else 1.1.dp, borderBrush), shape),
  ) {
    Column {
      // Glowing active-turn top bar strip
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(3.dp)
            .background(
              if (isActive) accent else Color.Transparent
            )
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        if (!isLeft) {
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
              text = name.uppercase(),
              color = if (isActive) RoyalGoldBright else TextMuted,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "$score PTS",
              style =
                TextStyle(
                  color = TextPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Black,
                  shadow = Shadow(color = Color.Black, offset = Offset(0f, 1.5f), blurRadius = 3f),
                ),
              maxLines = 1,
            )
          }
        }

        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White.copy(alpha = 0.14f), style = Stroke(width = 3.2.dp.toPx()))
            if (isActive && timerProgress > 0f) {
              drawArc(
                color = if (timerProgress < 0.28f) CrimsonQueen else accent,
                startAngle = -90f,
                sweepAngle = 360f * timerProgress,
                useCenter = false,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
              )
            }
          }
          Text(text = avatar, fontSize = 16.sp)
        }

        if (isLeft) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = name.uppercase(),
              color = if (isActive) CyanAccent else TextMuted,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "$score PTS",
              style =
                TextStyle(
                  color = TextPrimary,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Black,
                  shadow = Shadow(color = Color.Black, offset = Offset(0f, 1.5f), blurRadius = 3f),
                ),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BoardCoinCounterStrip(state: UiHudState) {
  MahoganyGlassCard(cornerRadius = 99.dp, glowAlpha = 0.18f, elevation = 8.dp) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CoinDotCounter(color = Color(0xFFF2E6CF), count = state.remainingWhite)
      CoinDotCounter(color = Color(0xFF282C38), border = Color.White.copy(alpha = 0.45f), count = state.remainingBlack)
      val qText =
        when (state.queenState) {
          QueenState.ON_BOARD -> "QUEEN: BOARD"
          QueenState.PENDING_COVER -> "QUEEN: COVER"
          QueenState.COVERED -> "QUEEN: COVERED"
        }
      Text(
        text = qText,
        color = if (state.queenState == QueenState.COVERED) RoyalGoldBright else CrimsonQueen,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Black,
      )
    }
  }
}

@Composable
private fun CoinDotCounter(color: Color, count: Int, border: Color = Color.Transparent) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(
      modifier =
        Modifier.size(11.dp)
          .clip(CircleShape)
          .background(color)
          .border(1.dp, border, CircleShape)
    )
    Text(text = count.toString(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
  }
}

@Composable
private fun CircularPowerGauge(powerPercent: Int) {
  MahoganyGlassCard(
    cornerRadius = 99.dp,
    glowAlpha = 0.34f,
    elevation = 14.dp,
    modifier = Modifier.size(76.dp).testTag("circular_power_gauge"),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val ratio = (powerPercent / 100f).coerceIn(0f, 1f)
        drawCircle(color = Color.White.copy(alpha = 0.14f), style = Stroke(width = 6.dp.toPx()))
        drawArc(
          color =
            when {
              ratio > 0.78f -> CrimsonQueen
              ratio > 0.45f -> AmberGold
              else -> CyanAccent
            },
          startAngle = -90f,
          sweepAngle = 360f * ratio,
          useCenter = false,
          style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$powerPercent%", color = RoyalGoldBright, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
        Text(text = "PWR", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun BottomStrikerControlBar(
  state: UiHudState,
  enabled: Boolean,
  onSliderChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().widthIn(max = 440.dp).alpha(if (enabled) 1f else 0.45f),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    CarvedWoodBrassSlider(
      value = state.strikerSliderValue,
      valueRange = -CarromConfig.BASELINE_LIMIT..CarromConfig.BASELINE_LIMIT,
      enabled = enabled,
      onValueChange = onSliderChange,
      modifier = Modifier.fillMaxWidth().testTag("striker_slider"),
    )

    val hintText =
      when {
        state.hintPowerPercent != null -> "POWER: ${state.hintPowerPercent}% • RELEASE TO SHOOT"
        state.isCancelingDrag -> stringResource(R.string.hint_cancel)
        else -> stringResource(R.string.hint_default)
      }
    Surface(
      shape = CircleShape,
      color = ObsidianBg.copy(alpha = 0.86f),
      border =
        BorderStroke(
          1.2.dp,
          if (state.hintPowerPercent != null) RoyalGoldBright else RoyalGold.copy(alpha = 0.42f),
        ),
    ) {
      Text(
        text = hintText,
        color = if (state.hintPowerPercent != null) RoyalGoldBright else TextMuted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
      )
    }
  }
}

/**
 * Custom carved mahogany wood & brass mechanical striker track slider.
 */
@Composable
private fun CarvedWoodBrassSlider(
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  enabled: Boolean,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  var widthPx by remember { mutableFloatStateOf(1f) }
  val currentOnChange by rememberUpdatedState(onValueChange)
  val currentValue by rememberUpdatedState(value)
  val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
  val norm = ((value - valueRange.start) / span).coerceIn(0f, 1f)

  fun pxToVal(x: Float, trackPadPx: Float): Float {
    val usable = (widthPx - trackPadPx * 2f).coerceAtLeast(1f)
    val t = ((x - trackPadPx) / usable).coerceIn(0f, 1f)
    return valueRange.start + t * span
  }

  val shape = RoundedCornerShape(18.dp)
  Box(
    modifier =
      modifier
        .height(52.dp)
        .shadow(14.dp, shape, clip = false)
        .royalGoldGlow(cornerRadius = 18.dp, glowColor = RoyalGold, glowAlpha = 0.25f)
        .clip(shape)
        .background(Color(0xFF100503))
        .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
        .semantics {
          progressBarRangeInfo = ProgressBarRangeInfo(currentValue, valueRange)
        }
        .pointerInput(enabled) {
          if (!enabled) return@pointerInput
          val pad = 26.dp.toPx()
          detectTapGestures { offset ->
            currentOnChange(pxToVal(offset.x, pad))
          }
        }
        .pointerInput(enabled) {
          if (!enabled) return@pointerInput
          val pad = 26.dp.toPx()
          detectHorizontalDragGestures { change, _ ->
            change.consume()
            currentOnChange(pxToVal(change.position.x, pad))
          }
        },
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cr = 18.dp.toPx()
      // 1. Carved Rosewood / Mahogany Housing
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            listOf(Color(0xFF542516), Color(0xFF36150C), Color(0xFF1E0A05), Color(0xFF120502))
          ),
        cornerRadius = CornerRadius(cr, cr),
      )

      // Subtle horizontal carved wood grain lines
      for (g in 1..6) {
        val gy = size.height * (g / 7f)
        drawLine(
          color = if (g % 2 == 0) Color(0x28FFD599) else Color(0x33000000),
          start = Offset(10.dp.toPx(), gy),
          end = Offset(size.width - 10.dp.toPx(), gy),
          strokeWidth = 1.dp.toPx(),
        )
      }

      // Outer Machined Brass Bezel Frame
      drawRoundRect(
        brush = RoyalBrushes.goldMetallic145,
        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = 2.2.dp.toPx()),
      )

      // 4 Brass Corner Rivets
      val rivPad = 9.dp.toPx()
      val rivPositions =
        listOf(
          Offset(rivPad, rivPad),
          Offset(size.width - rivPad, rivPad),
          Offset(rivPad, size.height - rivPad),
          Offset(size.width - rivPad, size.height - rivPad),
        )
      for (rp in rivPositions) {
        drawCircle(color = Color(0xFF422803), radius = 2.8.dp.toPx(), center = rp)
        drawCircle(brush = RoyalBrushes.goldMetallic145, radius = 2.1.dp.toPx(), center = rp)
      }

      // 2. Recessed Mechanical Groove Track
      val trackPad = 26.dp.toPx()
      val usableW = (size.width - trackPad * 2f).coerceAtLeast(1f)
      val grooveH = 12.dp.toPx()
      val grooveTop = (size.height - grooveH) * 0.5f

      drawRoundRect(
        color = Color(0xFF0A0403),
        topLeft = Offset(trackPad - 6.dp.toPx(), grooveTop),
        size = Size(usableW + 12.dp.toPx(), grooveH),
        cornerRadius = CornerRadius(grooveH / 2f, grooveH / 2f),
      )
      drawRoundRect(
        color = RoyalGoldDeep.copy(alpha = 0.7f),
        topLeft = Offset(trackPad - 6.dp.toPx(), grooveTop),
        size = Size(usableW + 12.dp.toPx(), grooveH),
        cornerRadius = CornerRadius(grooveH / 2f, grooveH / 2f),
        style = Stroke(width = 1.2.dp.toPx()),
      )

      // Etched Brass Ruler Notches
      for (i in 0..12) {
        val tx = trackPad + usableW * (i / 12f)
        val isCenter = i == 6
        val tickH = if (isCenter) 8.dp.toPx() else 4.dp.toPx()
        drawLine(
          color = if (isCenter) RoyalGoldBright else RoyalGold.copy(alpha = 0.45f),
          start = Offset(tx, grooveTop - tickH - 2.dp.toPx()),
          end = Offset(tx, grooveTop - 2.dp.toPx()),
          strokeWidth = if (isCenter) 1.8.dp.toPx() else 1.dp.toPx(),
        )
      }

      // Active Glowing Travel Bar from Center to Thumb
      val centerX = trackPad + usableW * 0.5f
      val thumbX = trackPad + usableW * norm
      val activeLeft = minOf(centerX, thumbX)
      val activeRight = maxOf(centerX, thumbX)
      if (activeRight - activeLeft > 2f) {
        drawRoundRect(
          brush = Brush.horizontalGradient(listOf(RoyalGoldDeep, RoyalGoldBright)),
          topLeft = Offset(activeLeft, grooveTop + 2.dp.toPx()),
          size = Size(activeRight - activeLeft, grooveH - 4.dp.toPx()),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
      }

      // 3. Machined 3D Brass & Ivory Striker Puck Thumb
      val thumbCenter = Offset(thumbX, size.height * 0.5f)
      val puckR = 17.dp.toPx()
      drawCircle(color = Color(0xAA000000), radius = puckR + 2.dp.toPx(), center = thumbCenter + Offset(0f, 2.dp.toPx()))
      drawCircle(brush = RoyalBrushes.goldMetallic145, radius = puckR, center = thumbCenter)
      drawCircle(color = Color(0xFF432903), radius = puckR, center = thumbCenter, style = Stroke(width = 1.5.dp.toPx()))
      drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFFF8E2), Color(0xFFE5CE9E)), center = thumbCenter, radius = puckR * 0.68f),
        radius = puckR * 0.68f,
        center = thumbCenter,
      )
      drawCircle(color = RoyalGoldDeep, radius = puckR * 0.68f, center = thumbCenter, style = Stroke(width = 1.4.dp.toPx()))
      drawCircle(color = CrimsonQueen, radius = puckR * 0.24f, center = thumbCenter)
    }
  }
}
