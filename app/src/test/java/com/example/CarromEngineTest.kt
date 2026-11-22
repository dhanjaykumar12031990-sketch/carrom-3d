package com.example

import com.example.audio.SoundEngine
import com.example.game.AiDifficulty
import com.example.game.ArenaElementType
import com.example.game.BoardCatalog
import com.example.game.CarromConfig
import com.example.game.CarromEngine
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.Piece
import com.example.game.PieceType
import com.example.game.QueenState
import com.example.game.StrikerCatalog
import com.example.game.TacticalAi
import com.example.game.TrickChallengeCatalog
import com.example.game.UiHudState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarromEngineTest {

  @Test
  fun `rack initializes with 9 white 9 black 1 queen and 1 striker`() {
    var latest = UiHudState()
    val sound = SoundEngine().apply { enabled = false }
    val engine =
      CarromEngine(
        sound = sound,
        onHudUpdated = { latest = it },
        onToast = { _, _ -> },
        onFoulFlash = {},
      )
    engine.startNewGame(GameMode.CLASSIC_1V1)
    val snap = engine.stepFrame(0.016f)

    assertEquals(GamePhase.AIMING, latest.phase)
    assertEquals(9, snap.pieces.count { it.type == PieceType.WHITE && it.visible })
    assertEquals(9, snap.pieces.count { it.type == PieceType.BLACK && it.visible })
    assertEquals(1, snap.pieces.count { it.type == PieceType.QUEEN && it.visible })
    assertEquals(1, snap.pieces.count { it.type == PieceType.STRIKER && it.visible })
    sound.release()
  }

  @Test
  fun `slider clamps striker inside baseline limits`() {
    var latest = UiHudState()
    val sound = SoundEngine().apply { enabled = false }
    val engine =
      CarromEngine(
        sound = sound,
        onHudUpdated = { latest = it },
        onToast = { _, _ -> },
        onFoulFlash = {},
      )
    engine.startNewGame(GameMode.DESI_ADDA_2V2)
    engine.onSliderChanged(10f)
    val snap = engine.stepFrame(0.016f)

    assertEquals(CarromConfig.BASELINE_LIMIT, snap.strikerX, 0.001f)
    assertEquals(CarromConfig.BASELINE_LIMIT, latest.strikerSliderValue, 0.001f)
    sound.release()
  }

  @Test
  fun `drag back and release fires striker into simulation and rewind restores shot`() {
    var latest = UiHudState()
    val sound = SoundEngine().apply { enabled = false }
    val engine =
      CarromEngine(
        sound = sound,
        onHudUpdated = { latest = it },
        onToast = { _, _ -> },
        onFoulFlash = {},
      )
    engine.startNewGame(GameMode.CLASSIC_1V1)
    engine.onPointerDown(0f, CarromConfig.BASELINE_Z)
    engine.onPointerMove(0f, CarromConfig.BASELINE_Z + 1.5f)
    assertTrue((latest.hintPowerPercent ?: 0) > 20)
    engine.onPointerUp()

    assertEquals(GamePhase.SIMULATING, latest.phase)
    val movingSnap = engine.stepFrame(0.016f)
    val striker = movingSnap.pieces.first { it.type == PieceType.STRIKER }
    assertTrue(striker.z < CarromConfig.BASELINE_Z)

    assertTrue(engine.rewindLastShot())
    assertEquals(GamePhase.REWINDING, latest.phase)
    repeat(30) { engine.stepFrame(0.03f) }
    assertEquals(GamePhase.AIMING, latest.phase)
    assertEquals(0, latest.rewindsRemaining)
    assertFalse(engine.rewindLastShot())
    sound.release()
  }

  @Test
  fun `chaos arena spawns bumpers speed pads and deflectors`() {
    val sound = SoundEngine().apply { enabled = false }
    val engine =
      CarromEngine(
        sound = sound,
        onHudUpdated = {},
        onToast = { _, _ -> },
        onFoulFlash = {},
      )
    engine.startNewGame(GameMode.CHAOS_ARENA)
    val snap = engine.stepFrame(0.016f)

    assertTrue(snap.arenaElements.any { it.type == ArenaElementType.BUMPER })
    assertTrue(snap.arenaElements.any { it.type == ArenaElementType.SPEED_PAD })
    assertTrue(snap.arenaElements.any { it.type == ArenaElementType.DEFLECTOR })
    sound.release()
  }

  @Test
  fun `50 trick challenges load cleanly with shot limits and catalogs resolve`() {
    assertEquals(50, TrickChallengeCatalog.all.size)
    var latest = UiHudState()
    val sound = SoundEngine().apply { enabled = false }
    val engine =
      CarromEngine(
        sound = sound,
        onHudUpdated = { latest = it },
        onToast = { _, _ -> },
        onFoulFlash = {},
      )
    engine.startChallenge(25)
    assertEquals(25, latest.challengeLevel)
    assertTrue(latest.challengeShotsLeft >= 2)

    engine.setSelectedStrikerId("maharaja")
    engine.setSelectedBoardId("maharaja_rosewood")
    val snap = engine.stepFrame(0.016f)
    assertEquals("maharaja", snap.selectedStrikerId)
    assertEquals("maharaja_rosewood", snap.selectedBoardId)
    assertEquals("Fire Inferno", StrikerCatalog.findById("fire").name)
    assertEquals("Lightning Volt", StrikerCatalog.findById("lightning").name)
    assertEquals("Emerald Club", BoardCatalog.findById("emerald_club").name)
    sound.release()
  }

  @Test
  fun `tactical AI computes bank shot when direct line is blocked`() {
    val striker = Piece(1, PieceType.STRIKER, CarromConfig.STRIKER_R, CarromConfig.STRIKER_H, 2.5f, 0f, 0f, -CarromConfig.BASELINE_Z)
    val targetCoin = Piece(2, PieceType.WHITE, CarromConfig.COIN_R, CarromConfig.COIN_H, 1.0f, 3.2f, 0f, -2.9f)
    val blocker = Piece(3, PieceType.BLACK, CarromConfig.COIN_R, CarromConfig.COIN_H, 1.0f, 1.5f, 0f, -3.1f)
    val plan =
      TacticalAi.computeShot(
        pieces = listOf(striker, targetCoin, blocker),
        striker = striker,
        arenaElements = emptyList(),
        queenState = QueenState.ON_BOARD,
        queenHolder = -1,
        aiPlayer = 1,
        difficulty = AiDifficulty.PRO,
        lastTargetPieceId = -1,
      )
    assertNotNull(plan)
    assertTrue(plan!!.speed >= CarromConfig.MIN_SHOT_SPEED)
  }
}
