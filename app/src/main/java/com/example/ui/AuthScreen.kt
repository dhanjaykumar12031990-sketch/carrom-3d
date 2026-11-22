package com.example.ui

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.CrimsonQueen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun AuthPortalOverlay(
  authError: String?,
  isAlreadySignedIn: Boolean,
  onGoogleAuthSuccess: () -> Unit,
  onGoogleAuthError: (String) -> Unit,
  onEmailSignIn: (email: String, password: String) -> Unit,
  onEmailSignUp: (name: String, email: String, password: String) -> Unit,
  onPlayAsGuest: () -> Unit,
  onReturnToLobby: () -> Unit,
  onClearError: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isGoogleLoading by remember { mutableStateOf(false) }
  var isSignUpMode by remember { mutableStateOf(false) }
  var displayName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(RoyalBrushes.royalLobbyVignette)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 18.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    MahoganyGlassCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
      cornerRadius = 26.dp,
      glowAlpha = 0.34f,
      borderWidth = 2.dp,
      elevation = 24.dp,
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = ObsidianBg.copy(alpha = 0.78f),
          border = BorderStroke(1.2.dp, RoyalGold.copy(alpha = 0.75f)),
        ) {
          Text(
            text = "ROYAL PASSPORT • CLOUD & OFFLINE",
            color = RoyalGoldBright,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Text(
          text = "CARROM ROYALE 3D",
          textAlign = TextAlign.Center,
          style =
            TextStyle(
              brush = Brush.verticalGradient(listOf(Color(0xFFFFF8D0), RoyalGoldBright, RoyalGoldDeep)),
              fontSize = 24.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.4.sp,
              shadow = Shadow(color = Color.Black, offset = Offset(0f, 3f), blurRadius = 8f),
            ),
        )

        // 1. Continue with Google Button (Jetpack Credential Manager + Firebase Auth)
        RoyalMetallicButton(
          onClick = {
            if (!isGoogleLoading) {
              onClearError()
              isGoogleLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isGoogleLoading = false
                  onGoogleAuthSuccess()
                },
                onAuthError = { msg ->
                  isGoogleLoading = false
                  onGoogleAuthError(msg)
                },
                scope = scope,
                onAuthCancelled = {
                  isGoogleLoading = false
                },
              )
            }
          },
          enabled = !isGoogleLoading,
          style = RoyalBevelStyle.GOOGLE_IVORY,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("auth_google_button"),
        ) {
          if (isGoogleLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color(0xFF1F1F1F),
              strokeWidth = 2.5.dp,
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              GoogleGLogo()
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "CONTINUE WITH GOOGLE",
                color = Color(0xFF1F1F1F),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f), color = RoyalGold.copy(alpha = 0.28f))
          Text(
            text = "OR WITH EMAIL",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
          )
          HorizontalDivider(modifier = Modifier.weight(1f), color = RoyalGold.copy(alpha = 0.28f))
        }

        // 2. Login / Sign Up Toggle Tabs
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ObsidianBg.copy(alpha = 0.72f))
              .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          AuthModeTab(
            label = "SIGN IN",
            selected = !isSignUpMode,
            onClick = {
              isSignUpMode = false
              onClearError()
            },
            modifier = Modifier.weight(1f).testTag("auth_tab_signin"),
          )
          AuthModeTab(
            label = "SIGN UP",
            selected = isSignUpMode,
            onClick = {
              isSignUpMode = true
              onClearError()
            },
            modifier = Modifier.weight(1f).testTag("auth_tab_signup"),
          )
        }

        val fieldColors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RoyalGoldBright,
            unfocusedBorderColor = RoyalGold.copy(alpha = 0.35f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = RoyalGoldBright,
            unfocusedLabelColor = TextMuted,
            cursorColor = RoyalGoldBright,
            focusedContainerColor = ObsidianBg.copy(alpha = 0.52f),
            unfocusedContainerColor = ObsidianBg.copy(alpha = 0.42f),
          )

        AnimatedVisibility(visible = isSignUpMode) {
          OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 18) displayName = it },
            label = { Text("Display Name") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = RoyalGold) },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
          )
        }

        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            onClearError()
          },
          label = { Text("Email Address") },
          leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = RoyalGold) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          colors = fieldColors,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
        )

        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            onClearError()
          },
          label = { Text("Password (6+ chars)") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = RoyalGold) },
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.testTag("auth_password_toggle"),
            ) {
              Icon(
                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = RoyalGoldBright,
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          colors = fieldColors,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
        )

        if (!authError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CrimsonQueen.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, CrimsonQueen.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth().testTag("auth_error_banner"),
          ) {
            Text(
              text = authError,
              color = Color(0xFFFFB8C6),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }

        RoyalMetallicButton(
          onClick = {
            if (isSignUpMode) {
              onEmailSignUp(displayName, email, password)
            } else {
              onEmailSignIn(email, password)
            }
          },
          style = RoyalBevelStyle.GOLD,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("auth_email_submit_button"),
        ) {
          Text(
            text = if (isSignUpMode) "CREATE ROYAL ACCOUNT" else "LOGIN WITH EMAIL",
            style =
              royalTextShadowStyle(
                color = Color(0xFF1F0F00),
                fontSize = 14.sp,
                shadowColor = Color(0x88FFF6B8),
              ),
          )
        }

        // 3. Play as Guest (Offline) 1-tap button
        RoyalMetallicButton(
          onClick = onPlayAsGuest,
          style = RoyalBevelStyle.MAHOGANY,
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("auth_guest_button"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.WifiOff,
              contentDescription = null,
              tint = RoyalGoldBright,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "PLAY AS GUEST (OFFLINE)",
              color = RoyalGoldBright,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
            )
          }
        }

        if (isAlreadySignedIn) {
          Text(
            text = "Back to Royal Lobby",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onReturnToLobby).padding(vertical = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun AuthModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .height(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .then(
          if (selected) Modifier.background(RoyalBrushes.goldMetallic145)
          else Modifier
        )
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = if (selected) ObsidianBg else TextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 0.8.sp,
    )
  }
}
