package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.AiDifficulty
import com.example.game.AuthProvider
import com.example.game.BoardCatalog
import com.example.game.GameMode
import com.example.game.LobbyTab
import com.example.game.StrikerCatalog
import com.example.game.UiHudState
import com.example.ui.theme.CrimsonQueen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val avatarGlyphs = listOf("👑", "🦁", "🦅", "⚡", "🔥", "🎯")

@Composable
fun LobbyDashboardOverlay(
  state: UiHudState,
  onSelectTab: (LobbyTab) -> Unit,
  onStartMode: (GameMode) -> Unit,
  onResumeMatch: () -> Unit,
  onStartChallenge: (Int) -> Unit,
  onSelectOrBuyStriker: (String) -> Unit,
  onSelectOrBuyBoard: (String) -> Unit,
  onSpinTick: () -> Unit,
  onClaimSpinReward: (Int) -> Unit,
  onUpdateProfile: (String, Int) -> Unit,
  onToggleSound: () -> Unit,
  onToggleMusic: () -> Unit,
  onSensitivityChanged: (Float) -> Unit,
  onDifficultySelected: (AiDifficulty) -> Unit,
  onLinkAccount: () -> Unit,
  onLogOut: () -> Unit,
) {
  var showSettings by remember { mutableStateOf(false) }
  var showProfileEdit by remember { mutableStateOf(false) }
  var showChallenges by remember { mutableStateOf(false) }
  var desi2v2 by remember { mutableStateOf(true) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(RoyalBrushes.royalLobbyVignette)
  ) {
    // Top Royal HUD Bar
    LobbyTopBar(
      state = state,
      onEditProfile = { showProfileEdit = true },
      onJumpToRewards = { onSelectTab(LobbyTab.REWARDS) },
      onOpenSettings = { showSettings = true },
      modifier =
        Modifier.align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 12.dp, vertical = 8.dp),
    )

    // Center Stage & Mode Cards or Shop/Rewards Tab
    when (state.lobbyTab) {
      LobbyTab.PLAY -> {
        val equippedStriker = StrikerCatalog.findById(state.selectedStrikerId)
        val equippedBoard = BoardCatalog.findById(state.selectedBoardId)

        MahoganyGlassCard(
          cornerRadius = 99.dp,
          glowAlpha = 0.28f,
          elevation = 10.dp,
          modifier =
            Modifier.align(Alignment.TopCenter)
              .windowInsetsPadding(WindowInsets.statusBars)
              .padding(top = 86.dp),
        ) {
          Text(
            text = "3D STAGE • ${equippedStriker.name.uppercase()} • ${equippedBoard.name.uppercase()}",
            color = RoyalGoldBright,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
          )
        }

        Column(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .windowInsetsPadding(WindowInsets.navigationBars)
              .padding(bottom = 86.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (state.hasActiveMatch) {
            RoyalMetallicButton(
              onClick = onResumeMatch,
              style = RoyalBevelStyle.EMERALD,
              modifier =
                Modifier.padding(horizontal = 18.dp)
                  .fillMaxWidth()
                  .widthIn(max = 420.dp)
                  .height(52.dp)
                  .testTag("resume_button"),
            ) {
              Text(
                text = stringResource(R.string.btn_resume),
                style = royalTextShadowStyle(color = Color(0xFF041C11), fontSize = 14.sp, shadowColor = Color(0x88B8FFE0)),
              )
            }
          }

          LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("lobby_modes_row"),
          ) {
            item(key = "classic") {
              BigIllustratedModeCard(
                title = "CLASSIC 1v1",
                subtitle = "Tactical AI (${state.aiDifficulty.label})",
                badge = "1v1 DUEL",
                accent = RoyalGold,
                bevelLipColor = RoyalGoldDeep,
                onClick = { onStartMode(GameMode.CLASSIC_1V1) },
                testTag = "mode_vs_ai_button",
                illustration = { ClassicModeIllustration() },
              )
            }
            item(key = "chaos") {
              BigIllustratedModeCard(
                title = "CHAOS ARENA",
                subtitle = "Bumpers & Speed Pads",
                badge = "UNIQUE",
                accent = CyanAccent,
                bevelLipColor = Color(0xFF077D70),
                onClick = { onStartMode(GameMode.CHAOS_ARENA) },
                testTag = "mode_chaos_button",
                illustration = { ChaosArenaIllustration() },
              )
            }
            item(key = "desi_adda") {
              DesiAddaIllustratedCard(
                is2v2 = desi2v2,
                onToggleVariant = { desi2v2 = !desi2v2 },
                onClick = {
                  onStartMode(if (desi2v2) GameMode.DESI_ADDA_2V2 else GameMode.DESI_ADDA_FFA)
                },
              )
            }
            item(key = "challenges") {
              BigIllustratedModeCard(
                title = "CHALLENGES",
                subtitle = "${state.completedChallenges.size}/50 Cleared",
                badge = "50 LEVELS",
                accent = CrimsonQueen,
                bevelLipColor = Color(0xFF830B29),
                onClick = { showChallenges = true },
                testTag = "mode_challenges_button",
                illustration = { ChallengesIllustration(cleared = state.completedChallenges.size) },
              )
            }
          }
        }
      }

      LobbyTab.SHOP -> {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .windowInsetsPadding(WindowInsets.navigationBars)
              .padding(top = 88.dp, bottom = 86.dp, start = 14.dp, end = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          ShopTabContent(
            state = state,
            onSelectOrBuyStriker = onSelectOrBuyStriker,
            onSelectOrBuyBoard = onSelectOrBuyBoard,
          )
        }
      }

      LobbyTab.REWARDS -> {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .windowInsetsPadding(WindowInsets.navigationBars)
              .padding(top = 88.dp, bottom = 86.dp, start = 14.dp, end = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          RewardsTabContent(
            canSpin = state.canSpinDailyWheel,
            onSpinTick = onSpinTick,
            onClaimReward = onClaimSpinReward,
          )
        }
      }
    }

    // Bottom Mahogany Glass Navigation Dock
    LobbyBottomDock(
      activeTab = state.lobbyTab,
      canSpin = state.canSpinDailyWheel,
      onSelectTab = onSelectTab,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 14.dp, vertical = 10.dp),
    )

    if (showChallenges) {
      ChallengesPickerDialog(
        highestUnlocked = state.highestChallengeUnlocked,
        completedChallenges = state.completedChallenges,
        onSelectChallenge = { lvl ->
          showChallenges = false
          onStartChallenge(lvl)
        },
        onDismiss = { showChallenges = false },
      )
    }

    if (showSettings) {
      SettingsDialog(
        state = state,
        onToggleSound = onToggleSound,
        onToggleMusic = onToggleMusic,
        onSensitivityChanged = onSensitivityChanged,
        onDifficultySelected = onDifficultySelected,
        onLinkAccount = {
          showSettings = false
          onLinkAccount()
        },
        onLogOut = {
          showSettings = false
          onLogOut()
        },
        onDismiss = { showSettings = false },
      )
    }

    if (showProfileEdit) {
      ProfileEditDialog(
        currentName = state.playerName,
        currentAvatarId = state.avatarId,
        onSave = onUpdateProfile,
        onDismiss = { showProfileEdit = false },
      )
    }
  }
}

@Composable
private fun LobbyTopBar(
  state: UiHudState,
  onEditProfile: () -> Unit,
  onJumpToRewards: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  MahoganyGlassCard(
    modifier = modifier.fillMaxWidth().widthIn(max = 580.dp),
    cornerRadius = 22.dp,
    glowAlpha = 0.28f,
    borderWidth = 1.8.dp,
    elevation = 16.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val profileInteraction = remember { MutableInteractionSource() }
      val profileScale = rememberBounceScale(profileInteraction)

      Row(
        modifier =
          Modifier.weight(1f)
            .scale(profileScale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
              interactionSource = profileInteraction,
              indication = ripple(color = RoyalGold),
              onClick = onEditProfile,
            )
            .padding(2.dp)
            .testTag("profile_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        RoyalCrestAvatar(
          glyph = avatarGlyphs.getOrElse(state.avatarId) { "👑" },
          level = state.playerLevel,
          size = 48.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
              text = state.playerName,
              color = TextPrimary,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Black,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Surface(
              shape = CircleShape,
              color =
                when (state.authProvider) {
                  AuthProvider.GOOGLE -> EmeraldWin.copy(alpha = 0.22f)
                  AuthProvider.EMAIL -> CyanAccent.copy(alpha = 0.22f)
                  else -> RoyalGold.copy(alpha = 0.16f)
                },
              border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
            ) {
              Text(
                text =
                  when (state.authProvider) {
                    AuthProvider.GOOGLE -> "GOOGLE"
                    AuthProvider.EMAIL -> "EMAIL"
                    else -> "GUEST"
                  },
                color = RoyalGoldBright,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          // Chunky Clash-style XP progress bar
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF0E0503))
                .border(1.dp, RoyalGold.copy(alpha = 0.35f), CircleShape),
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(state.levelXpProgress.coerceIn(0.08f, 1f))
                  .height(10.dp)
                  .clip(CircleShape)
                  .background(RoyalBrushes.goldMetallic145)
            )
            Text(
              text = "${state.levelXpText} • BEST ${state.highScore}",
              color = Color.White,
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.align(Alignment.Center),
            )
          }
        }
      }

      // Shiny 3D Coin Pill Counter
      val coinInteraction = remember { MutableInteractionSource() }
      val coinScale = rememberBounceScale(coinInteraction)
      Surface(
        shape = CircleShape,
        color = Color(0xFF140704),
        border = BorderStroke(1.6.dp, RoyalBrushes.goldMetallic145),
        shadowElevation = 10.dp,
        modifier =
          Modifier.scale(coinScale)
            .clip(CircleShape)
            .clickable(interactionSource = coinInteraction, indication = ripple(color = RoyalGold), onClick = onJumpToRewards)
            .testTag("coins_pill"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.MonetizationOn,
            contentDescription = "Total Coins",
            tint = RoyalGoldBright,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = state.coins.toString(),
            style = royalTextShadowStyle(color = RoyalGoldBright, fontSize = 13.sp, shadowColor = Color.Black),
          )
        }
      }

      RoyalBounceIconButton(
        onClick = onOpenSettings,
        modifier = Modifier.size(48.dp).testTag("settings_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Settings,
          contentDescription = stringResource(R.string.cd_settings_button),
          tint = RoyalGoldBright,
        )
      }
    }
  }
}

@Composable
private fun BigIllustratedModeCard(
  title: String,
  subtitle: String,
  badge: String,
  accent: Color,
  bevelLipColor: Color,
  onClick: () -> Unit,
  testTag: String,
  illustration: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberBounceScale(interactionSource)
  val shape = RoundedCornerShape(24.dp)

  Box(
    modifier =
      Modifier.width(166.dp)
        .height(232.dp)
        .scale(scale)
        .shadow(16.dp, shape, clip = false)
        .royalGoldGlow(cornerRadius = 24.dp, glowColor = accent, glowAlpha = 0.28f)
        .clip(shape)
        .background(bevelLipColor, shape)
        .padding(bottom = 5.dp)
        .clip(shape)
        .background(RoyalBrushes.mahoganyGlass, shape)
        .border(
          BorderStroke(1.8.dp, Brush.linearGradient(listOf(RoyalGoldBright, accent, RoyalGoldDeep))),
          shape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = RoyalGoldBright),
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(11.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = CircleShape,
        color = ObsidianBg.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.8f)),
      ) {
        Text(
          text = badge,
          color = accent,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
      }

      Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
        illustration()
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = title,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style =
            TextStyle(
              color = TextPrimary,
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.7.sp,
              shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 4f),
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          color = TextMuted,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Bottom beveled mini PLAY strip
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF7D5904))
            .padding(bottom = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RoyalBrushes.goldMetallic145),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
          Text(text = "PLAY NOW", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        }
      }
    }
  }
}

@Composable
private fun DesiAddaIllustratedCard(
  is2v2: Boolean,
  onToggleVariant: () -> Unit,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberBounceScale(interactionSource)
  val shape = RoundedCornerShape(24.dp)

  Box(
    modifier =
      Modifier.width(172.dp)
        .height(232.dp)
        .scale(scale)
        .shadow(16.dp, shape, clip = false)
        .royalGoldGlow(cornerRadius = 24.dp, glowColor = EmeraldWin, glowAlpha = 0.28f)
        .clip(shape)
        .background(Color(0xFF0D5E3C), shape)
        .padding(bottom = 5.dp)
        .clip(shape)
        .background(RoyalBrushes.mahoganyGlass, shape)
        .border(
          BorderStroke(1.8.dp, Brush.linearGradient(listOf(RoyalGoldBright, EmeraldWin, RoyalGoldDeep))),
          shape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = RoyalGoldBright),
          onClick = onClick,
        )
        .testTag("mode_desi_adda_button"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(11.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        modifier = Modifier.clip(CircleShape).clickable(onClick = onToggleVariant),
        shape = CircleShape,
        color = ObsidianBg.copy(alpha = 0.85f),
        border = BorderStroke(1.2.dp, EmeraldWin.copy(alpha = 0.85f)),
      ) {
        Text(
          text = if (is2v2) "2v2 TEAMS ▾" else "4P FFA ▾",
          color = EmeraldWin,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.6.sp,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
      }

      Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
        DesiAddaIllustration()
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "DESI ADDA",
          textAlign = TextAlign.Center,
          maxLines = 1,
          style =
            TextStyle(
              color = TextPrimary,
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.7.sp,
              shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 4f),
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (is2v2) "P1+P3 vs P2+P4 Party" else "4-Player Free-For-All",
          color = TextMuted,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF0A5C3A))
            .padding(bottom = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RoyalBevelStyle.EMERALD.topBrush),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF031A10), modifier = Modifier.size(16.dp))
          Text(text = "4P PARTY", color = Color(0xFF031A10), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        }
      }
    }
  }
}

@Composable
private fun ClassicModeIllustration() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val r = size.minDimension * 0.44f
    drawCircle(
      brush = Brush.radialGradient(listOf(RoyalGold.copy(alpha = 0.35f), Color.Transparent), center = center, radius = r * 1.15f),
      radius = r * 1.15f,
      center = center,
    )
    // Queen coin top center
    val qCenter = Offset(center.x, center.y - r * 0.35f)
    drawCircle(color = CrimsonQueen, radius = r * 0.28f, center = qCenter)
    drawCircle(color = RoyalGoldBright, radius = r * 0.28f, center = qCenter, style = Stroke(2.dp.toPx()))
    // White & Black coins
    val wCenter = Offset(center.x - r * 0.42f, center.y - r * 0.08f)
    drawCircle(color = Color(0xFFF6EBD7), radius = r * 0.25f, center = wCenter)
    drawCircle(color = RoyalGoldDeep, radius = r * 0.25f, center = wCenter, style = Stroke(1.5.dp.toPx()))
    val bCenter = Offset(center.x + r * 0.42f, center.y - r * 0.08f)
    drawCircle(color = Color(0xFF20242E), radius = r * 0.25f, center = bCenter)
    drawCircle(color = Color.White.copy(alpha = 0.45f), radius = r * 0.25f, center = bCenter, style = Stroke(1.5.dp.toPx()))
    // Golden Striker bottom
    val sCenter = Offset(center.x, center.y + r * 0.45f)
    drawCircle(brush = RoyalBrushes.goldMetallic145, radius = r * 0.34f, center = sCenter)
    drawCircle(color = Color(0xFFFFF8D0), radius = r * 0.34f, center = sCenter, style = Stroke(2.dp.toPx()))
    drawCircle(color = ObsidianBg, radius = r * 0.14f, center = sCenter)
  }
}

@Composable
private fun ChaosArenaIllustration() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val r = size.minDimension * 0.44f
    drawCircle(
      brush = Brush.radialGradient(listOf(CyanAccent.copy(alpha = 0.38f), Color.Transparent), center = center, radius = r * 1.2f),
      radius = r * 1.2f,
      center = center,
    )
    drawCircle(color = CyanAccent.copy(alpha = 0.45f), radius = r * 0.85f, center = center, style = Stroke(2.dp.toPx()))
    drawCircle(color = RoyalGoldBright, radius = r * 0.48f, center = center)
    drawCircle(color = CrimsonQueen, radius = r * 0.34f, center = center, style = Stroke(3.dp.toPx()))
    // Lightning bolt center
    val bolt =
      Path().apply {
        moveTo(center.x + r * 0.10f, center.y - r * 0.55f)
        lineTo(center.x - r * 0.18f, center.y + r * 0.05f)
        lineTo(center.x + r * 0.04f, center.y + r * 0.05f)
        lineTo(center.x - r * 0.10f, center.y + r * 0.55f)
        lineTo(center.x + r * 0.20f, center.y - r * 0.05f)
        lineTo(center.x - r * 0.02f, center.y - r * 0.05f)
        close()
      }
    drawPath(bolt, color = Color(0xFF0A1D24))
  }
}

@Composable
private fun DesiAddaIllustration() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val r = size.minDimension * 0.44f
    drawCircle(
      brush = Brush.radialGradient(listOf(EmeraldWin.copy(alpha = 0.32f), Color.Transparent), center = center, radius = r * 1.2f),
      radius = r * 1.2f,
      center = center,
    )
    // 4-corner player tokens connected by golden ring
    drawCircle(color = RoyalGold.copy(alpha = 0.65f), radius = r * 0.62f, center = center, style = Stroke(2.dp.toPx()))
    val corners = listOf(0f, 90f, 180f, 270f)
    corners.forEachIndexed { idx, deg ->
      val rad = (deg * PI / 180.0).toFloat()
      val px = center.x + cos(rad) * r * 0.62f
      val py = center.y + sin(rad) * r * 0.62f
      val col = if (idx % 2 == 0) EmeraldWin else RoyalGoldBright
      drawCircle(color = col, radius = r * 0.24f, center = Offset(px, py))
      drawCircle(color = ObsidianBg, radius = r * 0.24f, center = Offset(px, py), style = Stroke(1.6.dp.toPx()))
    }
    drawCircle(color = CrimsonQueen, radius = r * 0.22f, center = center)
  }
}

@Composable
private fun ChallengesIllustration(cleared: Int) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val r = size.minDimension * 0.44f
    drawCircle(
      brush = Brush.radialGradient(listOf(CrimsonQueen.copy(alpha = 0.35f), Color.Transparent), center = center, radius = r * 1.2f),
      radius = r * 1.2f,
      center = center,
    )
    // Precision reticle
    drawCircle(color = RoyalGoldBright.copy(alpha = 0.7f), radius = r * 0.75f, center = center, style = Stroke(2.dp.toPx()))
    drawLine(RoyalGoldBright, Offset(center.x - r * 0.9f, center.y), Offset(center.x + r * 0.9f, center.y), strokeWidth = 1.5.dp.toPx())
    drawLine(RoyalGoldBright, Offset(center.x, center.y - r * 0.9f), Offset(center.x, center.y + r * 0.9f), strokeWidth = 1.5.dp.toPx())
    // Trophy cup silhouette
    val cup =
      Path().apply {
        moveTo(center.x - r * 0.36f, center.y - r * 0.35f)
        lineTo(center.x + r * 0.36f, center.y - r * 0.35f)
        lineTo(center.x + r * 0.22f, center.y + r * 0.12f)
        lineTo(center.x + r * 0.08f, center.y + r * 0.26f)
        lineTo(center.x + r * 0.24f, center.y + r * 0.45f)
        lineTo(center.x - r * 0.24f, center.y + r * 0.45f)
        lineTo(center.x - r * 0.08f, center.y + r * 0.26f)
        lineTo(center.x - r * 0.22f, center.y + r * 0.12f)
        close()
      }
    drawPath(cup, brush = RoyalBrushes.goldMetallic145)
    // Progress arc
    val sweep = (cleared / 50f).coerceIn(0.05f, 1f) * 360f
    drawArc(
      color = EmeraldWin,
      startAngle = -90f,
      sweepAngle = sweep,
      useCenter = false,
      topLeft = Offset(center.x - r * 0.88f, center.y - r * 0.88f),
      size = Size(r * 1.76f, r * 1.76f),
      style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@Composable
private fun LobbyBottomDock(
  activeTab: LobbyTab,
  canSpin: Boolean,
  onSelectTab: (LobbyTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  MahoganyGlassCard(
    modifier = modifier.fillMaxWidth().widthIn(max = 440.dp),
    cornerRadius = 22.dp,
    borderWidth = 1.8.dp,
    elevation = 18.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DockTabItem(
        label = stringResource(R.string.lobby_tab_play),
        icon = Icons.Filled.SportsEsports,
        selected = activeTab == LobbyTab.PLAY,
        hasBadge = false,
        onClick = { onSelectTab(LobbyTab.PLAY) },
        testTag = "dock_play_tab",
        modifier = Modifier.weight(1f),
      )
      DockTabItem(
        label = stringResource(R.string.lobby_tab_shop),
        icon = Icons.Filled.Storefront,
        selected = activeTab == LobbyTab.SHOP,
        hasBadge = false,
        onClick = { onSelectTab(LobbyTab.SHOP) },
        testTag = "dock_shop_tab",
        modifier = Modifier.weight(1f),
      )
      DockTabItem(
        label = stringResource(R.string.lobby_tab_rewards),
        icon = Icons.Filled.CardGiftcard,
        selected = activeTab == LobbyTab.REWARDS,
        hasBadge = canSpin,
        onClick = { onSelectTab(LobbyTab.REWARDS) },
        testTag = "dock_rewards_tab",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun DockTabItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  hasBadge: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) 0.90f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "dock_bounce",
    )

  Box(
    modifier =
      modifier
        .height(50.dp)
        .scale(scale)
        .clip(RoundedCornerShape(15.dp))
        .then(
          if (selected) {
            Modifier.background(Color(0xFF8B6508))
              .padding(bottom = 3.5.dp)
              .clip(RoundedCornerShape(13.dp))
              .background(RoyalBrushes.goldMetallic145)
          } else {
            Modifier.background(Color.Transparent)
          }
        )
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = RoyalGold),
          onClick = onClick,
        )
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Box {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (selected) ObsidianBg else TextMuted,
          modifier = Modifier.size(20.dp),
        )
        if (hasBadge) {
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .size(7.dp)
                .clip(CircleShape)
                .background(CrimsonQueen)
          )
        }
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        color = if (selected) ObsidianBg else TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
      )
    }
  }
}
