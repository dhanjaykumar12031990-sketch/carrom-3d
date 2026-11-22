package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreenOverlay(onEnterLobby: () -> Unit) {
  var targetProgress by remember { mutableFloatStateOf(0f) }
  var ready by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    targetProgress = 1f
    delay(1350L)
    ready = true
  }

  val progress by
    animateFloatAsState(
      targetValue = targetProgress,
      animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
      label = "splash_progress",
    )

  val infinite = rememberInfiniteTransition(label = "splash_fx")
  val pulse by
    infinite.animateFloat(
      initialValue = 0.97f,
      targetValue = 1.045f,
      animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "enter_pulse",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              ObsidianBg.copy(alpha = 0.84f),
              ObsidianBg.copy(alpha = 0.16f),
              ObsidianBg.copy(alpha = 0.90f),
            )
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 22.dp)
  ) {
    // Top Title Header
    Column(
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = CircleShape,
        color = GlassPanel,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 8.dp,
      ) {
        Text(
          text = stringResource(R.string.splash_subtitle),
          color = RoyalGold,
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.1.sp,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.splash_title),
        textAlign = TextAlign.Center,
        style =
          TextStyle(
            brush =
              Brush.verticalGradient(
                listOf(Color(0xFFFFF8D0), RoyalGoldBright, RoyalGold, RoyalGoldDeep)
              ),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            shadow = Shadow(color = Color.Black.copy(alpha = 0.92f), offset = Offset(0f, 6f), blurRadius = 16f),
          ),
      )
    }

    // Bottom Loading Progress -> Pulsing Metallic Gold TAP TO ENTER
    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().widthIn(max = 360.dp).padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!ready) {
        MahoganyGlassCard(
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = stringResource(R.string.splash_loading),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart),
              )
              Text(
                text = "${(progress * 100).toInt()}%",
                color = RoyalGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterEnd),
              )
            }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(10.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.1f))
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(progress.coerceIn(0.03f, 1f))
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(RoyalBrushes.goldMetallic145)
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = ready,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
      ) {
        RoyalMetallicButton(
          onClick = onEnterLobby,
          shape = RoundedCornerShape(18.dp),
          cornerRadius = 18.dp,
          modifier =
            Modifier.fillMaxWidth()
              .height(58.dp)
              .scale(pulse)
              .testTag("splash_enter_button"),
        ) {
          Text(
            text = stringResource(R.string.splash_tap_enter),
            style =
              royalTextShadowStyle(
                color = ObsidianBg,
                fontSize = 16.sp,
                letterSpacing = 1.6.sp,
                shadowColor = Color(0x88FFF6B8),
              ),
          )
        }
      }
    }
  }
}
