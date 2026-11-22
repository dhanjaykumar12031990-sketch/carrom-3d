package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonQueen
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MahoganyDeep
import com.example.ui.theme.MahoganyRich
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object RoyalBrushes {
  val goldMetallic145: Brush =
    Brush.linearGradient(
      colors = listOf(Color(0xFFFFF199), RoyalGold, Color(0xFFDAA520), RoyalGoldDeep),
      start = Offset(0f, 0f),
      end = Offset(420f, 600f),
    )

  val goldBorderGlow: Brush =
    Brush.linearGradient(
      colors = listOf(RoyalGoldBright.copy(alpha = 0.95f), RoyalGoldDeep.copy(alpha = 0.78f), RoyalGold.copy(alpha = 0.95f)),
      start = Offset(0f, 0f),
      end = Offset(360f, 500f),
    )

  val mahoganyGlass: Brush =
    Brush.verticalGradient(
      colors =
        listOf(
          MahoganyRich.copy(alpha = 0.92f),
          Color(0xE8240E08),
          MahoganyDeep.copy(alpha = 0.95f),
        )
    )

  val royalLobbyVignette: Brush =
    Brush.radialGradient(
      colors =
        listOf(
          Color(0x184F1E10),
          Color(0x961D0905),
          Color(0xE80A0302),
        ),
      radius = 1250f,
    )
}

enum class RoyalBevelStyle(
  val topBrush: Brush,
  val bevelLipColor: Color,
  val topRimColor: Color,
  val outerBorderColor: Color,
  val contentColor: Color,
  val glowColor: Color,
) {
  GOLD(
    topBrush =
      Brush.linearGradient(
        colors = listOf(Color(0xFFFFF29C), Color(0xFFFFD700), Color(0xFFDAA520), Color(0xFFB8860B)),
        start = Offset(0f, 0f),
        end = Offset(400f, 520f),
      ),
    bevelLipColor = Color(0xFF8B6508),
    topRimColor = Color(0xFFFFF9C9),
    outerBorderColor = Color(0xFF432903),
    contentColor = Color(0xFF1F0F00),
    glowColor = RoyalGold,
  ),
  EMERALD(
    topBrush =
      Brush.linearGradient(
        colors = listOf(Color(0xFF42F5A7), EmeraldWin, Color(0xFF109E64)),
        start = Offset(0f, 0f),
        end = Offset(400f, 520f),
      ),
    bevelLipColor = Color(0xFF0A5C3A),
    topRimColor = Color(0xFFB6FFE0),
    outerBorderColor = Color(0xFF052B1B),
    contentColor = Color(0xFF031A10),
    glowColor = EmeraldWin,
  ),
  CRIMSON(
    topBrush =
      Brush.linearGradient(
        colors = listOf(Color(0xFFFF6B8A), CrimsonQueen, Color(0xFFA81238)),
        start = Offset(0f, 0f),
        end = Offset(400f, 520f),
      ),
    bevelLipColor = Color(0xFF6B0822),
    topRimColor = Color(0xFFFFC4D1),
    outerBorderColor = Color(0xFF33020E),
    contentColor = Color.White,
    glowColor = CrimsonQueen,
  ),
  MAHOGANY(
    topBrush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFF4A2114), Color(0xFF2D120A), Color(0xFF1A0805)),
      ),
    bevelLipColor = Color(0xFF0E0402),
    topRimColor = RoyalGold.copy(alpha = 0.72f),
    outerBorderColor = RoyalGoldDeep,
    contentColor = RoyalGoldBright,
    glowColor = RoyalGold,
  ),
  GOOGLE_IVORY(
    topBrush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3EFE6), Color(0xFFE4DDD0)),
      ),
    bevelLipColor = Color(0xFFA9A192),
    topRimColor = Color.White,
    outerBorderColor = Color(0xFF5D5342),
    contentColor = Color(0xFF1F1F1F),
    glowColor = Color(0xFFFFF4D2),
  ),
}

fun royalTextShadowStyle(
  color: Color,
  fontSize: TextUnit,
  fontWeight: FontWeight = FontWeight.Black,
  letterSpacing: TextUnit = 0.8.sp,
  shadowColor: Color = Color(0x99FFFFFF),
): TextStyle =
  TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
    shadow = Shadow(color = shadowColor, offset = Offset(0f, 1.5f), blurRadius = 2.5f),
  )

@Composable
fun rememberBounceScale(interactionSource: MutableInteractionSource): Float {
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.94f else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "royal_bounce_scale",
    )
  return scale
}

fun Modifier.royalGoldGlow(
  cornerRadius: Dp = 20.dp,
  glowColor: Color = RoyalGold,
  glowAlpha: Float = 0.24f,
): Modifier =
  this.drawBehind {
    val cr = cornerRadius.toPx()
    drawRoundRect(
      color = glowColor.copy(alpha = glowAlpha),
      topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
      size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
      cornerRadius = CornerRadius(cr + 2.dp.toPx(), cr + 2.dp.toPx()),
      style = Stroke(width = 4.5.dp.toPx()),
    )
  }

@Composable
fun MahoganyGlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  glowColor: Color = RoyalGold,
  glowAlpha: Float = 0.22f,
  borderBrush: Brush = RoyalBrushes.goldBorderGlow,
  borderWidth: Dp = 1.6.dp,
  elevation: Dp = 14.dp,
  onClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val interactionSource = remember { MutableInteractionSource() }
  val scale = if (onClick != null && enabled) rememberBounceScale(interactionSource) else 1f

  Box(
    modifier =
      modifier
        .scale(scale)
        .shadow(elevation = elevation, shape = shape, clip = false)
        .royalGoldGlow(cornerRadius = cornerRadius, glowColor = glowColor, glowAlpha = glowAlpha)
        .clip(shape)
        .background(RoyalBrushes.mahoganyGlass, shape)
        .border(BorderStroke(borderWidth, borderBrush), shape)
        .drawBehind {
          // Top specular rim & subtle inner dark bottom bevel
          drawLine(
            color = RoyalGoldBright.copy(alpha = 0.28f),
            start = Offset(cornerRadius.toPx() * 0.45f, 1.5.dp.toPx()),
            end = Offset(size.width - cornerRadius.toPx() * 0.45f, 1.5.dp.toPx()),
            strokeWidth = 1.4.dp.toPx(),
          )
        }
        .then(
          if (onClick != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = ripple(color = RoyalGoldBright),
              enabled = enabled,
              onClick = onClick,
            )
          } else {
            Modifier
          }
        ),
    content = content,
  )
}

/**
 * Heavy AAA 3D beveled button with thick 5.dp bottom extrusion lip (`#8b6508` for gold),
 * metallic 145deg gradient face, specular highlight, and physical press depression.
 */
@Composable
fun RoyalMetallicButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  style: RoyalBevelStyle = RoyalBevelStyle.GOLD,
  shape: Shape = RoundedCornerShape(16.dp),
  cornerRadius: Dp = 16.dp,
  bevelDepth: Dp = 5.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale = if (enabled) rememberBounceScale(interactionSource) else 1f
  val topOffsetY by
    animateDpAsState(
      targetValue = if (pressed && enabled) bevelDepth * 0.62f else 0.dp,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "bevel_press_y",
    )

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 48.dp)
        .scale(scale)
        .shadow(elevation = if (enabled) 12.dp else 2.dp, shape = shape, clip = false)
        .then(
          if (enabled) {
            Modifier.royalGoldGlow(cornerRadius = cornerRadius, glowColor = style.glowColor, glowAlpha = 0.28f)
          } else {
            Modifier
          }
        )
        .clip(shape)
        .background(
          color = if (enabled) style.bevelLipColor else Color(0xFF2B2320),
          shape = shape,
        )
        .border(
          BorderStroke(1.2.dp, if (enabled) style.outerBorderColor else Color.White.copy(alpha = 0.12f)),
          shape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = Color.White),
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        ),
  ) {
    // Upper 3D face floating above the 5.dp bottom bevel lip
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(bottom = bevelDepth)
          .offset(y = topOffsetY)
          .clip(shape)
          .background(
            brush =
              if (enabled) style.topBrush
              else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.09f))),
            shape = shape,
          )
          .border(
            BorderStroke(
              1.2.dp,
              if (enabled) style.topRimColor.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.14f),
            ),
            shape,
          )
          .drawBehind {
            if (enabled) {
              drawLine(
                color = Color.White.copy(alpha = 0.48f),
                start = Offset(cornerRadius.toPx() * 0.4f, 1.5.dp.toPx()),
                end = Offset(size.width - cornerRadius.toPx() * 0.4f, 1.5.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
              )
            }
          }
          .padding(contentPadding),
      contentAlignment = Alignment.Center,
    ) {
      CompositionLocalProvider(
        LocalContentColor provides if (enabled) style.contentColor else Color.White.copy(alpha = 0.45f)
      ) {
        content()
      }
    }
  }
}

@Composable
fun RoyalBounceIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  glowColor: Color = RoyalGold,
  enabled: Boolean = true,
  content: @Composable BoxScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = if (enabled) rememberBounceScale(interactionSource) else 1f
  val shape = RoundedCornerShape(14.dp)

  Surface(
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .scale(scale)
        .shadow(9.dp, shape, clip = false)
        .royalGoldGlow(cornerRadius = 14.dp, glowColor = glowColor, glowAlpha = 0.22f)
        .clip(shape)
        .background(Color(0xFF0E0402), shape)
        .padding(bottom = 3.dp)
        .clip(shape)
        .background(RoyalBrushes.mahoganyGlass, shape)
        .border(1.4.dp, RoyalBrushes.goldBorderGlow, shape)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = RoyalGoldBright),
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        ),
    color = Color.Transparent,
    shape = shape,
  ) {
    Box(contentAlignment = Alignment.Center, content = content)
  }
}

@Composable
fun RoyalCrestAvatar(
  glyph: String,
  level: Int,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
) {
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .shadow(8.dp, CircleShape, clip = false)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(listOf(Color(0xFF471C10), Color(0xFF190704)))
          )
          .border(2.2.dp, RoyalBrushes.goldMetallic145, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = glyph, fontSize = (size.value * 0.44f).sp)
    }
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .offset(y = 4.dp)
          .clip(CircleShape)
          .background(RoyalBrushes.goldMetallic145)
          .border(1.dp, Color(0xFF3E2402), CircleShape)
          .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
      Text(
        text = "LV $level",
        color = ObsidianBg,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
      )
    }
  }
}

@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val strokeW = size.minDimension * 0.21f
    val inset = strokeW * 0.55f
    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
    val topLeft = Offset(inset, inset)
    // Blue right-bottom arc
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -10f,
      sweepAngle = 72f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Butt),
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 60f,
      sweepAngle = 92f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Butt),
    )
    // Yellow left arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 150f,
      sweepAngle = 68f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Butt),
    )
    // Red top arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = 216f,
      sweepAngle = 102f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Butt),
    )
    // Horizontal blue crossbar
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(center.x, center.y),
      end = Offset(size.width - inset * 0.65f, center.y),
      strokeWidth = strokeW * 0.92f,
      cap = StrokeCap.Butt,
    )
  }
}

@Composable
fun RoyalSparkleField(
  modifier: Modifier = Modifier,
  intensity: Float = 1f,
  particleCount: Int = 26,
) {
  val infinite = rememberInfiniteTransition(label = "royal_ambient_sparkles")
  val phase by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = (PI * 2.0).toFloat(),
      animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
      label = "sparkle_drift",
    )

  Canvas(modifier = modifier.fillMaxSize()) {
    for (i in 0 until particleCount) {
      val seedX = ((i * 73 + 19) % 100) / 100f
      val seedY = ((i * 41 + 31) % 100) / 100f
      val driftX = sin(phase + i * 0.9f) * 18.dp.toPx()
      val driftY = cos(phase * 0.8f + i * 1.3f) * 22.dp.toPx()
      val px = size.width * seedX + driftX
      val py = size.height * seedY + driftY
      val twinkle = (0.22f + 0.78f * ((sin(phase * 3f + i * 1.7f) + 1f) * 0.5f)) * intensity
      val coreR = (1.6f + (i % 3) * 0.9f).dp.toPx()
      val col =
        when (i % 4) {
          0 -> RoyalGoldBright
          1 -> RoyalGold
          2 -> Color(0xFFFFC94D)
          else -> Color(0xFF00F5D4)
        }
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(col.copy(alpha = twinkle * 0.55f), Color.Transparent),
            center = Offset(px, py),
            radius = coreR * 4.5f,
          ),
        radius = coreR * 4.5f,
        center = Offset(px, py),
      )
      drawCircle(
        color = col.copy(alpha = twinkle.coerceIn(0f, 1f)),
        radius = coreR,
        center = Offset(px, py),
      )
    }
  }
}
