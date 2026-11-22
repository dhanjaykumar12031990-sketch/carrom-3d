package com.example.game

object CarromConfig {
  const val BOARD_HALF = 5.0f
  const val BOARD_THICK = 0.36f
  const val POCKET_OFFSET = 4.36f
  const val POCKET_R = 0.52f

  val POCKETS =
    arrayOf(
      PocketPos(-POCKET_OFFSET, -POCKET_OFFSET),
      PocketPos(POCKET_OFFSET, -POCKET_OFFSET),
      PocketPos(-POCKET_OFFSET, POCKET_OFFSET),
      PocketPos(POCKET_OFFSET, POCKET_OFFSET),
    )

  const val COIN_R = 0.27f
  const val COIN_H = 0.125f
  const val STRIKER_R = 0.35f
  const val STRIKER_H = 0.155f
  const val BASELINE_Z = 3.35f
  const val BASELINE_LIMIT = 2.85f

  const val RESTITUTION_PIECE = 0.87f
  const val RESTITUTION_RAIL = 0.80f
  const val FRICTION_COEFF = 2.45f
  const val DAMPING_PER_SEC = 0.96f
  const val STOP_SPEED = 0.045f
  const val MIN_SHOT_SPEED = 3.6f
  const val MAX_SHOT_SPEED = 24.0f
  const val MAX_PULL_DIST = 2.65f
  const val TURN_DURATION_SEC = 25f
}

data class PocketPos(val x: Float, val z: Float)

enum class AppScreen {
  SPLASH,
  AUTH,
  LOBBY,
  GAMEPLAY,
}

enum class AuthProvider(val label: String) {
  NONE("Signed Out"),
  GUEST("Guest Offline"),
  EMAIL("Email Account"),
  GOOGLE("Google"),
}

enum class LobbyTab {
  PLAY,
  SHOP,
  REWARDS,
}

enum class GameMode {
  CLASSIC_1V1,
  CHAOS_ARENA,
  DESI_ADDA_2V2,
  DESI_ADDA_FFA,
  TRICK_CHALLENGE,
}

enum class AiDifficulty(val label: String) {
  EASY("Easy"),
  MEDIUM("Medium"),
  PRO("Pro"),
}

enum class GamePhase {
  MENU,
  AIMING,
  AI_THINKING,
  SIMULATING,
  REWINDING,
  GAMEOVER,
}

enum class CameraMode {
  ANGLED_3D,
  TOP_DOWN,
}

enum class QueenState {
  ON_BOARD,
  PENDING_COVER,
  COVERED,
}

enum class PieceType {
  WHITE,
  BLACK,
  QUEEN,
  STRIKER,
}

enum class ToastTone {
  NEUTRAL,
  GOOD,
  QUEEN,
  FOUL,
}

enum class ArenaElementType {
  BUMPER,
  SPEED_PAD,
  DEFLECTOR,
}

data class ArenaElement(
  val id: Int,
  val type: ArenaElementType,
  val x: Float,
  val z: Float,
  val radius: Float,
)

data class Piece(
  val id: Int,
  val type: PieceType,
  val radius: Float,
  val height: Float,
  val mass: Float,
  var x: Float = 0f,
  var y: Float = 0f,
  var z: Float = 0f,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var vz: Float = 0f,
  var active: Boolean = true,
  var pocketing: Boolean = false,
  var pocketTargetX: Float = 0f,
  var pocketTargetZ: Float = 0f,
  var tiltX: Float = 0f,
  var tiltZ: Float = 0f,
  var spinY: Float = 0f,
  var speedPadCooldown: Float = 0f,
)

data class SparkParticle(
  var active: Boolean = false,
  var x: Float = 0f,
  var y: Float = 0f,
  var z: Float = 0f,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var vz: Float = 0f,
  var life: Float = 0f,
  var maxLife: Float = 0.35f,
  var r: Float = 1f,
  var g: Float = 0.82f,
  var b: Float = 0.4f,
)

data class AimVisualState(
  val visible: Boolean = false,
  val dirX: Float = 0f,
  val dirZ: Float = -1f,
  val power: Float = 0f,
  val strikerX: Float = 0f,
  val strikerZ: Float = CarromConfig.BASELINE_Z,
  val beamLength: Float = 1f,
  val impactX: Float = 0f,
  val impactZ: Float = 0f,
  val hasTargetHit: Boolean = false,
  val targetX: Float = 0f,
  val targetZ: Float = 0f,
  val deflectDirX: Float = 0f,
  val deflectDirZ: Float = 0f,
  val pullX: Float = 0f,
  val pullZ: Float = 0f,
)

data class RenderPieceSnapshot(
  val id: Int,
  val type: PieceType,
  val x: Float,
  val y: Float,
  val z: Float,
  val height: Float,
  val tiltX: Float,
  val tiltZ: Float,
  val spinY: Float,
  val visible: Boolean,
)

data class RenderSparkSnapshot(
  val x: Float,
  val y: Float,
  val z: Float,
  val scale: Float,
  val opacity: Float,
  val r: Float,
  val g: Float,
  val b: Float,
)

data class RenderSceneSnapshot(
  val pieces: List<RenderPieceSnapshot>,
  val sparks: List<RenderSparkSnapshot>,
  val arenaElements: List<ArenaElement>,
  val aim: AimVisualState,
  val invalidRingVisible: Boolean,
  val strikerX: Float,
  val strikerZ: Float,
  val selectedStrikerId: String,
  val selectedBoardId: String,
  val cameraMode: CameraMode,
  val gameMode: GameMode,
  val currentPlayer: Int,
  val appScreen: AppScreen,
  val previewOrbitRad: Float,
  val aiPlanning: Boolean,
)

data class ActiveEmote(
  val emoji: String,
  val playerIndex: Int,
  val timestampMs: Long,
)

data class UiHudState(
  val appScreen: AppScreen = AppScreen.SPLASH,
  val lobbyTab: LobbyTab = LobbyTab.PLAY,
  val mode: GameMode = GameMode.CLASSIC_1V1,
  val phase: GamePhase = GamePhase.MENU,
  val cameraMode: CameraMode = CameraMode.ANGLED_3D,
  val currentPlayer: Int = 0,
  val scores: List<Int> = listOf(0, 0, 0, 0),
  val coinsPocketed: List<Int> = listOf(0, 0, 0, 0),
  val remainingWhite: Int = 9,
  val remainingBlack: Int = 9,
  val queenState: QueenState = QueenState.ON_BOARD,
  val queenHolder: Int = -1,
  val strikerSliderValue: Float = 0f,
  val strikerValid: Boolean = true,
  val turnTimeRemaining: Float = CarromConfig.TURN_DURATION_SEC,
  val turnTimeProgress: Float = 1f,
  val rewindsRemaining: Int = 1,
  val canRewindNow: Boolean = false,
  val challengeLevel: Int = 1,
  val challengeTitle: String = "",
  val challengeShotsLeft: Int = 0,
  val challengeRewardCoins: Int = 0,
  val soundEnabled: Boolean = true,
  val musicEnabled: Boolean = false,
  val cameraSensitivity: Float = 1.0f,
  val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
  val toastMessage: String? = null,
  val toastTone: ToastTone = ToastTone.NEUTRAL,
  val foulFlash: Boolean = false,
  val hintPowerPercent: Int? = null,
  val isCancelingDrag: Boolean = false,
  val hasActiveMatch: Boolean = false,
  val menuOpen: Boolean = true,
  val winnerTitle: String = "",
  val winnerSubtitle: String = "",
  val rewardCoinsEarned: Int = 0,
  val activeEmote: ActiveEmote? = null,
  // Persistent Offline Profile, Auth & Economy
  val isLoggedIn: Boolean = false,
  val authProvider: AuthProvider = AuthProvider.NONE,
  val accountEmail: String = "",
  val playerName: String = "Royal Striker",
  val avatarId: Int = 0,
  val coins: Int = 200,
  val highScore: Int = 0,
  val careerPoints: Int = 0,
  val matchesWon: Int = 0,
  val selectedStrikerId: String = "classic",
  val unlockedStrikerIds: Set<String> = setOf("classic"),
  val selectedBoardId: String = "birch_royal",
  val unlockedBoardIds: Set<String> = setOf("birch_royal"),
  val highestChallengeUnlocked: Int = 1,
  val completedChallenges: Set<Int> = emptySet(),
  val canSpinDailyWheel: Boolean = true,
  val isNewHighScore: Boolean = false,
  val newlyUnlockedStrikerNames: List<String> = emptyList(),
) {
  val playerLevel: Int get() = 1 + (careerPoints / 120)
  val levelXpProgress: Float get() = (careerPoints % 120) / 120f
  val levelXpText: String get() = "${careerPoints % 120}/120 XP"
  val is4PlayerMode: Boolean get() = mode == GameMode.DESI_ADDA_2V2 || mode == GameMode.DESI_ADDA_FFA
  val playerCount: Int get() = if (is4PlayerMode) 4 else 2
}
