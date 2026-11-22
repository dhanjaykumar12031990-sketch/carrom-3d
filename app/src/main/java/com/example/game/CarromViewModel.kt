package com.example.game

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.auth.signOutGoogleAndFirebase
import com.example.data.CarromStatsRepository
import com.example.data.CloudMatchRecord
import com.example.data.CloudProfileRepository
import com.example.data.LocalAuthResult
import com.example.data.OfflinePlayerStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CloudSyncState {
  data object Offline : CloudSyncState
  data object Syncing : CloudSyncState
  data class Ready(val recentMatches: List<CloudMatchRecord>) : CloudSyncState
  data class Error(val message: String) : CloudSyncState
}

class CarromViewModel(
  private val repository: CarromStatsRepository,
  private val cloudRepository: CloudProfileRepository,
  private val authenticatedUid: String?,
  initialScreen: AppScreen = AppScreen.SPLASH,
  private val authenticatedEmail: String = "",
  private val authenticatedName: String = "",
  private val sound: SoundEngine = SoundEngine(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(UiHudState(appScreen = initialScreen))
  val uiState: StateFlow<UiHudState> = _uiState.asStateFlow()

  private var latestStats = OfflinePlayerStats()
  private var toastJob: Job? = null
  private var foulJob: Job? = null
  private var emoteJob: Job? = null
  private var cloudProfileListenerJob: Job? = null

  // Auth-gated realtime match history stream using two-tier CancellationException unwrapping
  val cloudSyncState: StateFlow<CloudSyncState> =
    if (!authenticatedUid.isNullOrBlank()) {
      cloudRepository
        .observeRecentMatches(authenticatedUid)
        .map<List<CloudMatchRecord>, CloudSyncState> { matches -> CloudSyncState.Ready(matches) }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          Log.w(TAG, "Error observing match history", rootError)
          emit(CloudSyncState.Error(rootError.localizedMessage ?: "Cloud sync paused"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = CloudSyncState.Syncing,
        )
    } else {
      MutableStateFlow(CloudSyncState.Offline)
    }

  val engine: CarromEngine =
    CarromEngine(
      sound = sound,
      onHudUpdated = { snapshot ->
        _uiState.update { current ->
          snapshot.copy(
            lobbyTab = current.lobbyTab,
            toastMessage = current.toastMessage,
            toastTone = current.toastTone,
            foulFlash = current.foulFlash,
            rewardCoinsEarned = current.rewardCoinsEarned,
            activeEmote = current.activeEmote,
            isLoggedIn = latestStats.isLoggedIn || !authenticatedUid.isNullOrBlank(),
            authProvider =
              if (!authenticatedUid.isNullOrBlank()) AuthProvider.GOOGLE else latestStats.authProvider,
            accountEmail = latestStats.accountEmail.ifBlank { authenticatedEmail },
            playerName = latestStats.playerName,
            avatarId = latestStats.avatarId,
            coins = latestStats.coins,
            highScore = latestStats.highScore,
            careerPoints = latestStats.careerPoints,
            matchesWon = latestStats.matchesWon,
            selectedStrikerId = latestStats.selectedStrikerId,
            unlockedStrikerIds = latestStats.unlockedStrikerIds,
            selectedBoardId = latestStats.selectedBoardId,
            unlockedBoardIds = latestStats.unlockedBoardIds,
            highestChallengeUnlocked = latestStats.highestChallengeUnlocked,
            completedChallenges = latestStats.completedChallenges,
            canSpinDailyWheel = latestStats.canSpinDailyWheel,
            soundEnabled = latestStats.soundEnabled,
            musicEnabled = latestStats.musicEnabled,
            cameraSensitivity = latestStats.cameraSensitivity,
            aiDifficulty = latestStats.aiDifficulty,
            isNewHighScore = current.isNewHighScore,
            newlyUnlockedStrikerNames = current.newlyUnlockedStrikerNames,
          )
        }
      },
      onToast = { message, tone -> showToast(message, tone) },
      onFoulFlash = { triggerFoulFlash() },
      onMatchFinished = { player1Score, won, finishedMode, challengeLevel ->
        viewModelScope.launch {
          val earnedCoins: Int
          if (finishedMode == GameMode.TRICK_CHALLENGE) {
            if (won) {
              val ch = TrickChallengeCatalog.findByLevel(challengeLevel)
              val firstClear = challengeLevel !in latestStats.completedChallenges
              earnedCoins = if (firstClear) ch.rewardCoins else (ch.rewardCoins / 3).coerceAtLeast(15)
              repository.recordChallengeWin(challengeLevel, ch.rewardCoins)
              _uiState.update { it.copy(rewardCoinsEarned = earnedCoins, isNewHighScore = false) }
            } else {
              earnedCoins = 0
              _uiState.update { it.copy(rewardCoinsEarned = 0, isNewHighScore = false) }
            }
          } else {
            val result = repository.recordMatchResult(player1Score, won)
            earnedCoins = result.coinsEarned
            _uiState.update {
              it.copy(
                isNewHighScore = result.isNewHighScore,
                rewardCoinsEarned = result.coinsEarned,
                newlyUnlockedStrikerNames = result.newlyUnlockedStrikers,
              )
            }
          }

          if (!authenticatedUid.isNullOrBlank()) {
            val refreshed = repository.statsFlow.first()
            cloudRepository.pushProfileUpdate(authenticatedUid, refreshed, authenticatedEmail)
            cloudRepository.recordMatch(
              userId = authenticatedUid,
              mode = finishedMode,
              playerScore = player1Score,
              won = won,
              challengeLevel = if (finishedMode == GameMode.TRICK_CHALLENGE) challengeLevel else 0,
              coinsEarned = earnedCoins,
            )
          }
        }
      },
    )

  init {
    engine.setAppScreen(initialScreen)

    viewModelScope.launch {
      repository.statsFlow.collect { stats ->
        latestStats = stats
        sound.enabled = stats.soundEnabled
        sound.setMusicEnabled(stats.musicEnabled)
        engine.setSelectedStrikerId(stats.selectedStrikerId)
        engine.setSelectedBoardId(stats.selectedBoardId)
        engine.setAiDifficulty(stats.aiDifficulty)
        engine.setCameraSensitivity(stats.cameraSensitivity)
        _uiState.update {
          it.copy(
            isLoggedIn = stats.isLoggedIn || !authenticatedUid.isNullOrBlank(),
            authProvider =
              if (!authenticatedUid.isNullOrBlank()) AuthProvider.GOOGLE else stats.authProvider,
            accountEmail = stats.accountEmail.ifBlank { authenticatedEmail },
            playerName = stats.playerName,
            avatarId = stats.avatarId,
            coins = stats.coins,
            highScore = stats.highScore,
            careerPoints = stats.careerPoints,
            matchesWon = stats.matchesWon,
            selectedStrikerId = stats.selectedStrikerId,
            unlockedStrikerIds = stats.unlockedStrikerIds,
            selectedBoardId = stats.selectedBoardId,
            unlockedBoardIds = stats.unlockedBoardIds,
            highestChallengeUnlocked = stats.highestChallengeUnlocked,
            completedChallenges = stats.completedChallenges,
            canSpinDailyWheel = stats.canSpinDailyWheel,
            soundEnabled = stats.soundEnabled,
            musicEnabled = stats.musicEnabled,
            cameraSensitivity = stats.cameraSensitivity,
            aiDifficulty = stats.aiDifficulty,
          )
        }
      }
    }

    // Start Firestore profile sync ONLY when authenticated with Google
    if (!authenticatedUid.isNullOrBlank()) {
      startAuthenticatedCloudSync(authenticatedUid)
    }
  }

  private fun startAuthenticatedCloudSync(uid: String) {
    cloudProfileListenerJob?.cancel()
    cloudProfileListenerJob =
      viewModelScope.launch {
        val currentLocal = repository.statsFlow.first()
        repository.signInWithGoogleSession(authenticatedEmail, authenticatedName)
        val syncResult =
          cloudRepository.syncOrInitializeProfile(
            userId = uid,
            localStats = currentLocal,
            googleEmail = authenticatedEmail,
            googleDisplayName = authenticatedName,
          )
        syncResult.onSuccess { mergedCloud ->
          repository.applyCloudProfileSnapshot(mergedCloud)
        }

        // Observe ongoing updates for this authenticated user cleanly
        cloudRepository
          .observeUserProfile(uid)
          .catch { error ->
            val firestoreError =
              generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Profile snapshot stream ended", firestoreError ?: error)
          }
          .collect { snapshotProfile ->
            if (snapshotProfile != null) {
              repository.applyCloudProfileSnapshot(snapshotProfile)
            }
          }
      }
  }

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  fun clearAuthError() {
    _authError.value = null
  }

  fun reportAuthError(message: String) {
    sound.playFoul()
    _authError.value = message
  }

  fun enterLobbyFromSplash() {
    sound.playQueenCover()
    _authError.value = null
    if (!authenticatedUid.isNullOrBlank() || latestStats.isLoggedIn) {
      engine.setAppScreen(AppScreen.LOBBY)
    } else {
      engine.setAppScreen(AppScreen.AUTH)
    }
  }

  fun openAuthScreen() {
    _authError.value = null
    engine.setAppScreen(AppScreen.AUTH)
  }

  fun continueAsGuest() {
    viewModelScope.launch {
      repository.signInAsGuest()
      sound.playQueenCover()
      _authError.value = null
      engine.setAppScreen(AppScreen.LOBBY)
      showToast("WELCOME, GUEST STRIKER!", ToastTone.GOOD)
    }
  }

  fun signInWithEmail(email: String, password: String) {
    viewModelScope.launch {
      when (val res = repository.signInWithEmail(email, password)) {
        LocalAuthResult.Success -> {
          sound.playRewardWin()
          _authError.value = null
          engine.setAppScreen(AppScreen.LOBBY)
          showToast("WELCOME BACK!", ToastTone.QUEEN)
        }
        is LocalAuthResult.Error -> {
          sound.playFoul()
          _authError.value = res.message
        }
      }
    }
  }

  fun signUpWithEmail(displayName: String, email: String, password: String) {
    viewModelScope.launch {
      when (val res = repository.signUpWithEmail(displayName, email, password)) {
        LocalAuthResult.Success -> {
          sound.playRewardWin()
          _authError.value = null
          engine.setAppScreen(AppScreen.LOBBY)
          showToast("ROYAL ACCOUNT CREATED!", ToastTone.QUEEN)
        }
        is LocalAuthResult.Error -> {
          sound.playFoul()
          _authError.value = res.message
        }
      }
    }
  }

  fun onGoogleSignInComplete(onMarkInteracted: () -> Unit) {
    sound.playRewardWin()
    _authError.value = null
    onMarkInteracted()
    engine.setAppScreen(AppScreen.LOBBY)
    showToast("GOOGLE CLOUD SYNC ACTIVE!", ToastTone.QUEEN)
  }

  fun logOut(credentialManager: CredentialManager, onAfterSignOut: () -> Unit = {}) {
    viewModelScope.launch {
      cloudProfileListenerJob?.cancel()
      cloudProfileListenerJob = null
      signOutGoogleAndFirebase(credentialManager)
      repository.logOut()
      _authError.value = null
      engine.setAppScreen(AppScreen.AUTH)
      onAfterSignOut()
      showToast("SIGNED OUT", ToastTone.NEUTRAL)
    }
  }

  fun selectLobbyTab(tab: LobbyTab) {
    _uiState.update { it.copy(lobbyTab = tab) }
  }

  fun startNewGame(mode: GameMode) {
    _uiState.update {
      it.copy(isNewHighScore = false, rewardCoinsEarned = 0, newlyUnlockedStrikerNames = emptyList())
    }
    engine.startNewGame(mode)
  }

  fun startChallenge(level: Int) {
    _uiState.update {
      it.copy(isNewHighScore = false, rewardCoinsEarned = 0, newlyUnlockedStrikerNames = emptyList())
    }
    engine.startChallenge(level)
  }

  fun openMenu() = engine.openMenu()

  fun resumeGame() = engine.resumeGame()

  fun toggleCamera() = engine.toggleCamera()

  fun toggleSound() {
    val next = !latestStats.soundEnabled
    viewModelScope.launch { repository.updateSettings(soundEnabled = next) }
  }

  fun toggleMusic() {
    val next = !latestStats.musicEnabled
    viewModelScope.launch { repository.updateSettings(musicEnabled = next) }
  }

  fun updateCameraSensitivity(value: Float) {
    viewModelScope.launch { repository.updateSettings(cameraSensitivity = value) }
  }

  fun updateAiDifficulty(difficulty: AiDifficulty) {
    viewModelScope.launch { repository.updateSettings(aiDifficulty = difficulty) }
  }

  fun updateProfile(name: String, avatarId: Int) {
    viewModelScope.launch {
      repository.updateProfile(name, avatarId)
      pushCloudSnapshotIfSignedIn()
    }
  }

  fun onSliderChanged(value: Float) = engine.onSliderChanged(value)

  fun rewindLastShot() {
    engine.rewindLastShot()
  }

  fun triggerEmote(emoji: String) {
    sound.playEmote()
    emoteJob?.cancel()
    val playerIdx = _uiState.value.currentPlayer
    _uiState.update {
      it.copy(activeEmote = ActiveEmote(emoji, playerIdx, System.currentTimeMillis()))
    }
    emoteJob =
      viewModelScope.launch {
        delay(2000L)
        _uiState.update { it.copy(activeEmote = null) }
      }
  }

  fun selectOrBuyStriker(strikerId: String) {
    val skin = StrikerCatalog.findById(strikerId)
    val alreadyUnlocked = strikerId in latestStats.unlockedStrikerIds
    viewModelScope.launch {
      val ok = repository.purchaseOrSelectStriker(strikerId)
      if (ok) {
        if (!alreadyUnlocked) {
          sound.playRewardWin()
          showToast("UNLOCKED ${skin.name.uppercase()}!", ToastTone.QUEEN)
        } else {
          sound.playEmote()
        }
        pushCloudSnapshotIfSignedIn()
      } else {
        sound.playFoul()
        showToast("NEED ${skin.unlockCoins} COINS!", ToastTone.FOUL)
      }
    }
  }

  fun selectOrBuyBoard(boardId: String) {
    val board = BoardCatalog.findById(boardId)
    val alreadyUnlocked = boardId in latestStats.unlockedBoardIds
    viewModelScope.launch {
      val ok = repository.purchaseOrSelectBoard(boardId)
      if (ok) {
        if (!alreadyUnlocked) {
          sound.playRewardWin()
          showToast("UNLOCKED ${board.name.uppercase()}!", ToastTone.QUEEN)
        } else {
          sound.playEmote()
        }
        pushCloudSnapshotIfSignedIn()
      } else {
        sound.playFoul()
        showToast("NEED ${board.unlockCoins} COINS!", ToastTone.FOUL)
      }
    }
  }

  fun playSpinTick() = sound.playSpinTick()

  fun claimDailySpinReward(coinsWon: Int) {
    viewModelScope.launch {
      val claimed = repository.claimDailySpinReward(coinsWon)
      if (claimed) {
        sound.playRewardWin()
        showToast("+$coinsWon COINS WON!", ToastTone.QUEEN)
        pushCloudSnapshotIfSignedIn()
      }
    }
  }

  private suspend fun pushCloudSnapshotIfSignedIn() {
    val uid = authenticatedUid
    if (!uid.isNullOrBlank()) {
      val refreshed = repository.statsFlow.first()
      cloudRepository.pushProfileUpdate(uid, refreshed, authenticatedEmail)
    }
  }

  private fun showToast(message: String, tone: ToastTone) {
    toastJob?.cancel()
    _uiState.update { it.copy(toastMessage = message, toastTone = tone) }
    toastJob =
      viewModelScope.launch {
        delay(1750L)
        _uiState.update { it.copy(toastMessage = null) }
      }
  }

  private fun triggerFoulFlash() {
    foulJob?.cancel()
    _uiState.update { it.copy(foulFlash = true) }
    foulJob =
      viewModelScope.launch {
        delay(450L)
        _uiState.update { it.copy(foulFlash = false) }
      }
  }

  override fun onCleared() {
    super.onCleared()
    cloudProfileListenerJob?.cancel()
    sound.release()
  }

  private companion object {
    const val TAG = "CarromViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
