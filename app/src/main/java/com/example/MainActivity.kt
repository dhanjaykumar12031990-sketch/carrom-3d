package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.data.CarromStatsRepository
import com.example.data.CloudProfileRepository
import com.example.game.AppScreen
import com.example.game.CarromViewModel
import com.example.ui.CarromScreen
import com.example.ui.theme.CarromTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      CarromTheme {
        CarromAppRoot()
      }
    }
  }
}

@Composable
private fun CarromAppRoot() {
  val context = LocalContext.current
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var hasInteractedWithApp by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { /* authStateFlow updates currentUser automatically */ },
      onUnauthenticated = { /* Player stays in offline / auth portal until tapping Google Sign-In */ },
      scope = this,
    )
  }

  val firebaseUid = currentUser?.uid
  val firebaseEmail = currentUser?.email ?: ""
  val firebaseName = currentUser?.displayName ?: ""
  val scopeKey = firebaseUid ?: "offline"

  // Key ViewModel & UI composition by uid so signing in, switching accounts, or signing out
  // cleanly disposes old Firestore listeners and reinitializes state.
  key(scopeKey) {
    val startScreen =
      when {
        !firebaseUid.isNullOrBlank() && hasInteractedWithApp -> AppScreen.LOBBY
        firebaseUid == null && hasInteractedWithApp -> AppScreen.AUTH
        else -> AppScreen.SPLASH
      }
    val vm: CarromViewModel =
      viewModel(
        key = scopeKey,
        factory =
          viewModelFactory {
            initializer {
              val app =
                checkNotNull(this[APPLICATION_KEY]) {
                  "APPLICATION_KEY missing from CreationExtras"
                }
              val databaseId = app.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(databaseId)
              CarromViewModel(
                repository = CarromStatsRepository(app.applicationContext),
                cloudRepository = CloudProfileRepository(db, auth),
                authenticatedUid = firebaseUid,
                initialScreen = startScreen,
                authenticatedEmail = firebaseEmail,
                authenticatedName = firebaseName,
              )
            }
          },
      )
    CarromScreen(
      viewModel = vm,
      onMarkInteracted = { hasInteractedWithApp = true },
    )
  }
}
