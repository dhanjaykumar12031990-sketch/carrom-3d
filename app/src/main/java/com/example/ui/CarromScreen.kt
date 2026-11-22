package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.AppScreen
import com.example.game.CarromViewModel
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.gl.CarromGLSurfaceView
import com.example.ui.theme.ObsidianBg

@Composable
fun CarromScreen(
  viewModel: CarromViewModel,
  onMarkInteracted: () -> Unit = {},
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val glSurfaceView = remember(viewModel) { CarromGLSurfaceView(context, viewModel.engine) }

  DisposableEffect(lifecycleOwner, glSurfaceView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
        Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      glSurfaceView.onPause()
    }
  }

  BackHandler(
    enabled =
      state.appScreen == AppScreen.GAMEPLAY ||
        (state.appScreen == AppScreen.LOBBY && state.hasActiveMatch) ||
        (state.appScreen == AppScreen.AUTH && state.isLoggedIn)
  ) {
    when {
      state.appScreen == AppScreen.GAMEPLAY -> viewModel.openMenu()
      state.appScreen == AppScreen.LOBBY && state.hasActiveMatch -> viewModel.resumeGame()
      state.appScreen == AppScreen.AUTH && state.isLoggedIn -> viewModel.openMenu()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
    // Persistent Full-Screen 3D OpenGL ES 2.0 Board
    AndroidView(
      factory = { glSurfaceView },
      modifier = Modifier.fillMaxSize().testTag("carrom_3d_canvas"),
    )

    // Glowing Ambient Particle Sparkles across Splash, Auth, Lobby & subtle in Gameplay
    RoyalSparkleField(
      intensity =
        when (state.appScreen) {
          AppScreen.SPLASH -> 0.95f
          AppScreen.AUTH -> 0.85f
          AppScreen.LOBBY -> 0.78f
          AppScreen.GAMEPLAY -> 0.22f
        },
      particleCount = if (state.appScreen == AppScreen.GAMEPLAY) 14 else 30,
    )

    when (state.appScreen) {
      AppScreen.SPLASH -> {
        SplashScreenOverlay(
          onEnterLobby = {
            onMarkInteracted()
            viewModel.enterLobbyFromSplash()
          }
        )
      }

      AppScreen.AUTH -> {
        AuthPortalOverlay(
          authError = authError,
          isAlreadySignedIn = state.isLoggedIn,
          onGoogleAuthSuccess = { viewModel.onGoogleSignInComplete(onMarkInteracted) },
          onGoogleAuthError = viewModel::reportAuthError,
          onEmailSignIn = { email, pass ->
            onMarkInteracted()
            viewModel.signInWithEmail(email, pass)
          },
          onEmailSignUp = { name, email, pass ->
            onMarkInteracted()
            viewModel.signUpWithEmail(name, email, pass)
          },
          onPlayAsGuest = {
            onMarkInteracted()
            viewModel.continueAsGuest()
          },
          onReturnToLobby = viewModel::openMenu,
          onClearError = viewModel::clearAuthError,
        )
      }

      AppScreen.LOBBY -> {
        LobbyDashboardOverlay(
          state = state,
          onSelectTab = viewModel::selectLobbyTab,
          onStartMode = viewModel::startNewGame,
          onResumeMatch = viewModel::resumeGame,
          onStartChallenge = viewModel::startChallenge,
          onSelectOrBuyStriker = viewModel::selectOrBuyStriker,
          onSelectOrBuyBoard = viewModel::selectOrBuyBoard,
          onSpinTick = viewModel::playSpinTick,
          onClaimSpinReward = viewModel::claimDailySpinReward,
          onUpdateProfile = viewModel::updateProfile,
          onToggleSound = viewModel::toggleSound,
          onToggleMusic = viewModel::toggleMusic,
          onSensitivityChanged = viewModel::updateCameraSensitivity,
          onDifficultySelected = viewModel::updateAiDifficulty,
          onLinkAccount = viewModel::openAuthScreen,
          onLogOut = { viewModel.logOut(credentialManager, onMarkInteracted) },
        )
      }

      AppScreen.GAMEPLAY -> {
        GameplayHudOverlay(
          state = state,
          onToggleCamera = viewModel::toggleCamera,
          onToggleSound = viewModel::toggleSound,
          onOpenLobby = viewModel::openMenu,
          onRewindShot = viewModel::rewindLastShot,
          onTriggerEmote = viewModel::triggerEmote,
          onSliderChanged = viewModel::onSliderChanged,
        )

        if (state.phase == GamePhase.GAMEOVER) {
          GameOverOverlay(
            state = state,
            onPlayAgain = {
              if (state.mode == GameMode.TRICK_CHALLENGE) {
                viewModel.startChallenge(state.challengeLevel)
              } else {
                viewModel.startNewGame(state.mode)
              }
            },
            onNextChallenge = {
              viewModel.startChallenge((state.challengeLevel + 1).coerceAtMost(50))
            },
            onMainMenu = viewModel::openMenu,
          )
        }
      }
    }
  }
}
