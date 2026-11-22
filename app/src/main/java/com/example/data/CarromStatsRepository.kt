package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.AiDifficulty
import com.example.game.AuthProvider
import com.example.game.BoardCatalog
import com.example.game.StrikerCatalog
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.carromDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "carrom_offline_stats")

data class OfflinePlayerStats(
  val isLoggedIn: Boolean = false,
  val authProvider: AuthProvider = AuthProvider.NONE,
  val accountEmail: String = "",
  val playerName: String = "Royal Striker",
  val avatarId: Int = 0,
  val coins: Int = 200,
  val highScore: Int = 0,
  val careerPoints: Int = 0,
  val matchesWon: Int = 0,
  val matchesPlayed: Int = 0,
  val selectedStrikerId: String = "classic",
  val unlockedStrikerIds: Set<String> = setOf("classic"),
  val selectedBoardId: String = "birch_royal",
  val unlockedBoardIds: Set<String> = setOf("birch_royal"),
  val highestChallengeUnlocked: Int = 1,
  val completedChallenges: Set<Int> = emptySet(),
  val lastSpinEpochDay: Long = -1L,
  val soundEnabled: Boolean = true,
  val musicEnabled: Boolean = false,
  val cameraSensitivity: Float = 1.0f,
  val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
) {
  val canSpinDailyWheel: Boolean
    get() = (System.currentTimeMillis() / 86_400_000L) > lastSpinEpochDay
}

data class MatchRecordResult(
  val isNewHighScore: Boolean,
  val coinsEarned: Int,
  val newlyUnlockedStrikers: List<String>,
)

sealed interface LocalAuthResult {
  data object Success : LocalAuthResult
  data class Error(val message: String) : LocalAuthResult
}

class CarromStatsRepository(private val context: Context) {
  private object Keys {
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val AUTH_PROVIDER = stringPreferencesKey("auth_provider")
    val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    val EMAIL_ACCOUNTS = stringSetPreferencesKey("local_email_accounts")
    val PLAYER_NAME = stringPreferencesKey("player_name")
    val AVATAR_ID = intPreferencesKey("avatar_id")
    val COINS = intPreferencesKey("coins")
    val HIGH_SCORE = intPreferencesKey("high_score")
    val CAREER_POINTS = intPreferencesKey("career_points")
    val MATCHES_WON = intPreferencesKey("matches_won")
    val MATCHES_PLAYED = intPreferencesKey("matches_played")
    val SELECTED_STRIKER = stringPreferencesKey("selected_striker")
    val UNLOCKED_STRIKERS = stringSetPreferencesKey("unlocked_strikers")
    val SELECTED_BOARD = stringPreferencesKey("selected_board")
    val UNLOCKED_BOARDS = stringSetPreferencesKey("unlocked_boards")
    val HIGHEST_CHALLENGE = intPreferencesKey("highest_challenge")
    val COMPLETED_CHALLENGES = stringSetPreferencesKey("completed_challenges")
    val LAST_SPIN_DAY = longPreferencesKey("last_spin_day")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    val CAM_SENSITIVITY = floatPreferencesKey("cam_sensitivity")
    val AI_DIFFICULTY = stringPreferencesKey("ai_difficulty")
  }

  val statsFlow: Flow<OfflinePlayerStats> =
    context.carromDataStore.data.map { prefs ->
      val unlockedStrikers = (prefs[Keys.UNLOCKED_STRIKERS] ?: emptySet()) + "classic"
      val selectedStriker = prefs[Keys.SELECTED_STRIKER] ?: "classic"
      val unlockedBoards = (prefs[Keys.UNLOCKED_BOARDS] ?: emptySet()) + "birch_royal"
      val selectedBoard = prefs[Keys.SELECTED_BOARD] ?: "birch_royal"
      val diffName = prefs[Keys.AI_DIFFICULTY] ?: AiDifficulty.MEDIUM.name
      val diff = AiDifficulty.entries.firstOrNull { it.name == diffName } ?: AiDifficulty.MEDIUM
      val provName = prefs[Keys.AUTH_PROVIDER] ?: AuthProvider.NONE.name
      val provider = AuthProvider.entries.firstOrNull { it.name == provName } ?: AuthProvider.NONE
      val completedSet =
        (prefs[Keys.COMPLETED_CHALLENGES] ?: emptySet())
          .mapNotNull { it.toIntOrNull() }
          .toSet()

      OfflinePlayerStats(
        isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
        authProvider = provider,
        accountEmail = prefs[Keys.ACCOUNT_EMAIL] ?: "",
        playerName = prefs[Keys.PLAYER_NAME] ?: "Royal Striker",
        avatarId = prefs[Keys.AVATAR_ID] ?: 0,
        coins = prefs[Keys.COINS] ?: 200,
        highScore = prefs[Keys.HIGH_SCORE] ?: 0,
        careerPoints = prefs[Keys.CAREER_POINTS] ?: 0,
        matchesWon = prefs[Keys.MATCHES_WON] ?: 0,
        matchesPlayed = prefs[Keys.MATCHES_PLAYED] ?: 0,
        selectedStrikerId = if (selectedStriker in unlockedStrikers) selectedStriker else "classic",
        unlockedStrikerIds = unlockedStrikers,
        selectedBoardId = if (selectedBoard in unlockedBoards) selectedBoard else "birch_royal",
        unlockedBoardIds = unlockedBoards,
        highestChallengeUnlocked = (prefs[Keys.HIGHEST_CHALLENGE] ?: 1).coerceIn(1, 50),
        completedChallenges = completedSet,
        lastSpinEpochDay = prefs[Keys.LAST_SPIN_DAY] ?: -1L,
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
        cameraSensitivity = (prefs[Keys.CAM_SENSITIVITY] ?: 1.0f).coerceIn(0.6f, 1.6f),
        aiDifficulty = diff,
      )
    }

  suspend fun signInAsGuest() {
    context.carromDataStore.edit { prefs ->
      prefs[Keys.IS_LOGGED_IN] = true
      prefs[Keys.AUTH_PROVIDER] = AuthProvider.GUEST.name
      prefs[Keys.ACCOUNT_EMAIL] = ""
      if (prefs[Keys.PLAYER_NAME].isNullOrBlank()) {
        prefs[Keys.PLAYER_NAME] = "Guest Striker"
      }
    }
  }

  suspend fun signUpWithEmail(displayName: String, email: String, password: String): LocalAuthResult {
    val cleanEmail = email.trim().lowercase()
    val cleanName = displayName.trim().take(18).ifBlank { cleanEmail.substringBefore('@').take(18).ifBlank { "Royal Striker" } }
    if (!EMAIL_REGEX.matches(cleanEmail)) {
      return LocalAuthResult.Error("Please enter a valid email address.")
    }
    if (password.length < 6) {
      return LocalAuthResult.Error("Password must be at least 6 characters.")
    }

    var outcome: LocalAuthResult = LocalAuthResult.Success
    context.carromDataStore.edit { prefs ->
      val existing = prefs[Keys.EMAIL_ACCOUNTS] ?: emptySet()
      val conflict = existing.any { it.substringBefore('|') == cleanEmail }
      if (conflict) {
        outcome = LocalAuthResult.Error("An offline account with this email already exists. Switch to Sign In.")
      } else {
        val saltHex = generateSaltHex()
        val hashHex = hashPassword(saltHex, password)
        val encodedName = cleanName.replace("|", "")
        prefs[Keys.EMAIL_ACCOUNTS] = existing + "$cleanEmail|$saltHex|$hashHex|$encodedName"
        prefs[Keys.IS_LOGGED_IN] = true
        prefs[Keys.AUTH_PROVIDER] = AuthProvider.EMAIL.name
        prefs[Keys.ACCOUNT_EMAIL] = cleanEmail
        prefs[Keys.PLAYER_NAME] = cleanName
      }
    }
    return outcome
  }

  suspend fun signInWithEmail(email: String, password: String): LocalAuthResult {
    val cleanEmail = email.trim().lowercase()
    if (!EMAIL_REGEX.matches(cleanEmail)) {
      return LocalAuthResult.Error("Please enter a valid email address.")
    }
    if (password.isEmpty()) {
      return LocalAuthResult.Error("Please enter your password.")
    }

    var outcome: LocalAuthResult = LocalAuthResult.Error("No local account found for $cleanEmail. Tap Sign Up to create one.")
    context.carromDataStore.edit { prefs ->
      val existing = prefs[Keys.EMAIL_ACCOUNTS] ?: emptySet()
      val entry = existing.firstOrNull { it.substringBefore('|') == cleanEmail }
      if (entry != null) {
        val parts = entry.split('|', limit = 4)
        if (parts.size >= 4) {
          val salt = parts[1]
          val expectedHash = parts[2]
          val storedName = parts[3].ifBlank { "Royal Striker" }
          if (constantTimeEquals(hashPassword(salt, password), expectedHash)) {
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.AUTH_PROVIDER] = AuthProvider.EMAIL.name
            prefs[Keys.ACCOUNT_EMAIL] = cleanEmail
            prefs[Keys.PLAYER_NAME] = storedName
            outcome = LocalAuthResult.Success
          } else {
            outcome = LocalAuthResult.Error("Incorrect password. Please try again.")
          }
        }
      }
    }
    return outcome
  }

  suspend fun signInWithGoogleSession(email: String, displayName: String) {
    val cleanEmail = email.trim().take(120)
    val cleanName =
      displayName.trim().take(18).ifBlank {
        cleanEmail.substringBefore('@').take(18).ifBlank { "Royal Striker" }
      }
    context.carromDataStore.edit { prefs ->
      prefs[Keys.IS_LOGGED_IN] = true
      prefs[Keys.AUTH_PROVIDER] = AuthProvider.GOOGLE.name
      prefs[Keys.ACCOUNT_EMAIL] = cleanEmail
      if (cleanName.isNotBlank()) {
        prefs[Keys.PLAYER_NAME] = cleanName
      }
    }
  }

  suspend fun applyCloudProfileSnapshot(cloud: CloudUserProfile): OfflinePlayerStats {
    var updatedStats = OfflinePlayerStats()
    context.carromDataStore.edit { prefs ->
      val strikers = (cloud.unlockedStrikerIds + "classic").toSet()
      val boards = (cloud.unlockedBoardIds + "birch_royal").toSet()
      val completed = cloud.completedChallenges.map { it.toInt() }.toSet()
      val selectedStriker =
        if (cloud.selectedStrikerId in strikers) cloud.selectedStrikerId else "classic"
      val selectedBoard =
        if (cloud.selectedBoardId in boards) cloud.selectedBoardId else "birch_royal"

      prefs[Keys.IS_LOGGED_IN] = true
      prefs[Keys.AUTH_PROVIDER] = AuthProvider.GOOGLE.name
      if (cloud.email.isNotBlank()) {
        prefs[Keys.ACCOUNT_EMAIL] = cloud.email
      }
      if (cloud.displayName.isNotBlank()) {
        prefs[Keys.PLAYER_NAME] = cloud.displayName.take(18)
      }
      prefs[Keys.AVATAR_ID] = cloud.avatarId.toInt().coerceIn(0, 5)
      prefs[Keys.COINS] = cloud.coins.toInt().coerceAtLeast(0)
      prefs[Keys.HIGH_SCORE] = cloud.highScore.toInt().coerceAtLeast(0)
      prefs[Keys.CAREER_POINTS] = cloud.careerPoints.toInt().coerceAtLeast(0)
      prefs[Keys.MATCHES_WON] = cloud.matchesWon.toInt().coerceAtLeast(0)
      prefs[Keys.MATCHES_PLAYED] = cloud.matchesPlayed.toInt().coerceAtLeast(0)
      prefs[Keys.SELECTED_STRIKER] = selectedStriker
      prefs[Keys.UNLOCKED_STRIKERS] = strikers
      prefs[Keys.SELECTED_BOARD] = selectedBoard
      prefs[Keys.UNLOCKED_BOARDS] = boards
      prefs[Keys.HIGHEST_CHALLENGE] = cloud.highestChallengeUnlocked.toInt().coerceIn(1, 50)
      prefs[Keys.COMPLETED_CHALLENGES] = completed.map { it.toString() }.toSet()
      prefs[Keys.LAST_SPIN_DAY] = cloud.lastSpinEpochDay

      updatedStats =
        OfflinePlayerStats(
          isLoggedIn = true,
          authProvider = AuthProvider.GOOGLE,
          accountEmail = cloud.email.ifBlank { prefs[Keys.ACCOUNT_EMAIL] ?: "" },
          playerName = cloud.displayName.take(18).ifBlank { prefs[Keys.PLAYER_NAME] ?: "Royal Striker" },
          avatarId = cloud.avatarId.toInt().coerceIn(0, 5),
          coins = cloud.coins.toInt().coerceAtLeast(0),
          highScore = cloud.highScore.toInt().coerceAtLeast(0),
          careerPoints = cloud.careerPoints.toInt().coerceAtLeast(0),
          matchesWon = cloud.matchesWon.toInt().coerceAtLeast(0),
          matchesPlayed = cloud.matchesPlayed.toInt().coerceAtLeast(0),
          selectedStrikerId = selectedStriker,
          unlockedStrikerIds = strikers,
          selectedBoardId = selectedBoard,
          unlockedBoardIds = boards,
          highestChallengeUnlocked = cloud.highestChallengeUnlocked.toInt().coerceIn(1, 50),
          completedChallenges = completed,
          lastSpinEpochDay = cloud.lastSpinEpochDay,
          soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
          musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
          cameraSensitivity = (prefs[Keys.CAM_SENSITIVITY] ?: 1.0f).coerceIn(0.6f, 1.6f),
          aiDifficulty =
            AiDifficulty.entries.firstOrNull { it.name == (prefs[Keys.AI_DIFFICULTY] ?: "") }
              ?: AiDifficulty.MEDIUM,
        )
    }
    return updatedStats
  }

  suspend fun logOut() {
    context.carromDataStore.edit { prefs ->
      prefs[Keys.IS_LOGGED_IN] = false
      prefs[Keys.AUTH_PROVIDER] = AuthProvider.NONE.name
      prefs[Keys.ACCOUNT_EMAIL] = ""
    }
  }

  suspend fun updateProfile(name: String, avatarId: Int) {
    val cleanName = name.trim().take(18).ifEmpty { "Royal Striker" }
    context.carromDataStore.edit { prefs ->
      prefs[Keys.PLAYER_NAME] = cleanName
      prefs[Keys.AVATAR_ID] = avatarId.coerceIn(0, 5)
    }
  }

  suspend fun updateSettings(
    soundEnabled: Boolean? = null,
    musicEnabled: Boolean? = null,
    cameraSensitivity: Float? = null,
    aiDifficulty: AiDifficulty? = null,
  ) {
    context.carromDataStore.edit { prefs ->
      soundEnabled?.let { prefs[Keys.SOUND_ENABLED] = it }
      musicEnabled?.let { prefs[Keys.MUSIC_ENABLED] = it }
      cameraSensitivity?.let { prefs[Keys.CAM_SENSITIVITY] = it.coerceIn(0.6f, 1.6f) }
      aiDifficulty?.let { prefs[Keys.AI_DIFFICULTY] = it.name }
    }
  }

  suspend fun purchaseOrSelectStriker(strikerId: String): Boolean {
    val skin = StrikerCatalog.findById(strikerId)
    var success = false
    context.carromDataStore.edit { prefs ->
      val unlocked = (prefs[Keys.UNLOCKED_STRIKERS] ?: emptySet()) + "classic"
      if (skin.id in unlocked) {
        prefs[Keys.SELECTED_STRIKER] = skin.id
        success = true
      } else {
        val currentCoins = prefs[Keys.COINS] ?: 200
        if (currentCoins >= skin.unlockCoins) {
          prefs[Keys.COINS] = currentCoins - skin.unlockCoins
          prefs[Keys.UNLOCKED_STRIKERS] = unlocked + skin.id
          prefs[Keys.SELECTED_STRIKER] = skin.id
          success = true
        }
      }
    }
    return success
  }

  suspend fun purchaseOrSelectBoard(boardId: String): Boolean {
    val board = BoardCatalog.findById(boardId)
    var success = false
    context.carromDataStore.edit { prefs ->
      val unlocked = (prefs[Keys.UNLOCKED_BOARDS] ?: emptySet()) + "birch_royal"
      if (board.id in unlocked) {
        prefs[Keys.SELECTED_BOARD] = board.id
        success = true
      } else {
        val currentCoins = prefs[Keys.COINS] ?: 200
        if (currentCoins >= board.unlockCoins) {
          prefs[Keys.COINS] = currentCoins - board.unlockCoins
          prefs[Keys.UNLOCKED_BOARDS] = unlocked + board.id
          prefs[Keys.SELECTED_BOARD] = board.id
          success = true
        }
      }
    }
    return success
  }

  suspend fun claimDailySpinReward(coinReward: Int): Boolean {
    val today = System.currentTimeMillis() / 86_400_000L
    var claimed = false
    context.carromDataStore.edit { prefs ->
      val last = prefs[Keys.LAST_SPIN_DAY] ?: -1L
      if (today > last) {
        prefs[Keys.LAST_SPIN_DAY] = today
        prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 200) + coinReward
        claimed = true
      }
    }
    return claimed
  }

  suspend fun recordChallengeWin(level: Int, rewardCoins: Int) {
    context.carromDataStore.edit { prefs ->
      val prevCoins = prefs[Keys.COINS] ?: 200
      val prevCompleted = (prefs[Keys.COMPLETED_CHALLENGES] ?: emptySet()).toMutableSet()
      val firstClear = prevCompleted.add(level.toString())
      val bonus = if (firstClear) rewardCoins else (rewardCoins / 3).coerceAtLeast(15)
      prefs[Keys.COINS] = prevCoins + bonus
      prefs[Keys.COMPLETED_CHALLENGES] = prevCompleted
      val curHigh = prefs[Keys.HIGHEST_CHALLENGE] ?: 1
      if (level >= curHigh && level < 50) {
        prefs[Keys.HIGHEST_CHALLENGE] = (level + 1).coerceAtMost(50)
      }
      prefs[Keys.CAREER_POINTS] = (prefs[Keys.CAREER_POINTS] ?: 0) + 45
      prefs[Keys.MATCHES_WON] = (prefs[Keys.MATCHES_WON] ?: 0) + 1
      prefs[Keys.MATCHES_PLAYED] = (prefs[Keys.MATCHES_PLAYED] ?: 0) + 1
    }
  }

  suspend fun recordMatchResult(player1Score: Int, won: Boolean): MatchRecordResult {
    var isNewBest = false
    val coinsEarned = (if (won) 90 else 25) + (player1Score.coerceAtLeast(0) / 4)
    val newlyUnlocked = mutableListOf<String>()

    context.carromDataStore.edit { prefs ->
      val prevHigh = prefs[Keys.HIGH_SCORE] ?: 0
      if (player1Score > prevHigh) {
        prefs[Keys.HIGH_SCORE] = player1Score
        isNewBest = true
      }

      val newCareer = (prefs[Keys.CAREER_POINTS] ?: 0) + player1Score.coerceAtLeast(0) + (if (won) 35 else 10)
      prefs[Keys.CAREER_POINTS] = newCareer
      prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 200) + coinsEarned
      prefs[Keys.MATCHES_PLAYED] = (prefs[Keys.MATCHES_PLAYED] ?: 0) + 1
      if (won) {
        prefs[Keys.MATCHES_WON] = (prefs[Keys.MATCHES_WON] ?: 0) + 1
      }
    }

    return MatchRecordResult(
      isNewHighScore = isNewBest,
      coinsEarned = coinsEarned,
      newlyUnlockedStrikers = newlyUnlocked,
    )
  }

  private fun generateSaltHex(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun hashPassword(saltHex: String, password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$saltHex:$password".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
    return diff == 0
  }

  private companion object {
    val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
  }
}
