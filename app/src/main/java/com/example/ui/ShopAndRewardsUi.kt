package com.example.ui

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.game.AiDifficulty
import com.example.game.AuthProvider
import com.example.game.BoardCatalog
import com.example.game.BoardTheme
import com.example.game.StrikerCatalog
import com.example.game.StrikerSkin
import com.example.game.TrickChallengeCatalog
import com.example.game.UiHudState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonQueen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RosewoodDark
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ShopTabContent(
  state: UiHudState,
  onSelectOrBuyStriker: (String) -> Unit,
  onSelectOrBuyBoard: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  MahoganyGlassCard(
    modifier = modifier.fillMaxWidth().widthIn(max = 520.dp),
    cornerRadius = 24.dp,
    elevation = 18.dp,
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.shop_strikers),
          color = RoyalGoldBright,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp,
        )
        Text(
          text = "${state.unlockedStrikerIds.size}/${StrikerCatalog.all.size} Owned",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.testTag("shop_strikers_row"),
      ) {
        items(StrikerCatalog.all, key = { it.id }) { skin ->
          val unlocked = skin.id in state.unlockedStrikerIds
          val equipped = skin.id == state.selectedStrikerId
          StrikerShopCard(
            skin = skin,
            unlocked = unlocked,
            equipped = equipped,
            canAfford = state.coins >= skin.unlockCoins,
            onClick = { onSelectOrBuyStriker(skin.id) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.shop_boards),
          color = RoyalGoldBright,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp,
        )
        Text(
          text = "${state.unlockedBoardIds.size}/${BoardCatalog.all.size} Owned",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.testTag("shop_boards_row"),
      ) {
        items(BoardCatalog.all, key = { it.id }) { board ->
          val unlocked = board.id in state.unlockedBoardIds
          val equipped = board.id == state.selectedBoardId
          BoardShopCard(
            board = board,
            unlocked = unlocked,
            equipped = equipped,
            canAfford = state.coins >= board.unlockCoins,
            onClick = { onSelectOrBuyBoard(board.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun StrikerShopCard(
  skin: StrikerSkin,
  unlocked: Boolean,
  equipped: Boolean,
  canAfford: Boolean,
  onClick: () -> Unit,
) {
  MahoganyGlassCard(
    modifier = Modifier.width(122.dp).testTag("striker_card_${skin.id}"),
    cornerRadius = 18.dp,
    glowColor = if (equipped) RoyalGold else Color(0xFF8C6214),
    glowAlpha = if (equipped) 0.34f else 0.12f,
    borderBrush =
      if (equipped) RoyalBrushes.goldMetallic145
      else Brush.linearGradient(listOf(RoyalGold.copy(alpha = 0.35f), Color.White.copy(alpha = 0.14f))),
    borderWidth = if (equipped) 1.6.dp else 1.dp,
    elevation = if (equipped) 12.dp else 6.dp,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StrikerDiscPreview(skin = skin, modifier = Modifier.size(50.dp))
      Text(
        text = skin.name,
        color = TextPrimary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(text = skin.tagLine, color = TextMuted, fontSize = 9.5.sp, maxLines = 1)
      ShopActionPill(
        unlocked = unlocked,
        equipped = equipped,
        price = skin.unlockCoins,
        canAfford = canAfford,
      )
    }
  }
}

@Composable
private fun BoardShopCard(
  board: BoardTheme,
  unlocked: Boolean,
  equipped: Boolean,
  canAfford: Boolean,
  onClick: () -> Unit,
) {
  val surfCol = remember(board.surfaceMidHex) { Color(AndroidColor.parseColor(board.surfaceMidHex)) }
  val railCol = remember(board.railBaseHex) { Color(AndroidColor.parseColor(board.railBaseHex)) }
  val lineCol = remember(board.lineAccentHex) { Color(AndroidColor.parseColor(board.lineAccentHex)) }

  MahoganyGlassCard(
    modifier = Modifier.width(128.dp).testTag("board_card_${board.id}"),
    cornerRadius = 18.dp,
    glowColor = if (equipped) RoyalGold else Color(0xFF8C6214),
    glowAlpha = if (equipped) 0.34f else 0.12f,
    borderBrush =
      if (equipped) RoyalBrushes.goldMetallic145
      else Brush.linearGradient(listOf(RoyalGold.copy(alpha = 0.35f), Color.White.copy(alpha = 0.14f))),
    borderWidth = if (equipped) 1.6.dp else 1.dp,
    elevation = if (equipped) 12.dp else 6.dp,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Canvas(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))) {
        drawRect(railCol)
        val pad = size.minDimension * 0.14f
        drawRect(
          color = surfCol,
          topLeft = Offset(pad, pad),
          size = Size(size.width - pad * 2f, size.height - pad * 2f),
        )
        drawCircle(
          color = lineCol,
          radius = size.minDimension * 0.18f,
          center = center,
          style = Stroke(width = 2.dp.toPx()),
        )
      }
      Text(
        text = board.name,
        color = TextPrimary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      ShopActionPill(
        unlocked = unlocked,
        equipped = equipped,
        price = board.unlockCoins,
        canAfford = canAfford,
      )
    }
  }
}

@Composable
private fun ShopActionPill(
  unlocked: Boolean,
  equipped: Boolean,
  price: Int,
  canAfford: Boolean,
) {
  Box(
    modifier =
      Modifier.clip(CircleShape)
        .then(
          if (equipped) Modifier.background(RoyalBrushes.goldMetallic145)
          else
            Modifier.background(
              when {
                unlocked -> CyanAccent.copy(alpha = 0.18f)
                canAfford -> AmberGold.copy(alpha = 0.22f)
                else -> Color.White.copy(alpha = 0.08f)
              }
            )
        )
        .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (!unlocked) {
        Icon(
          imageVector = Icons.Filled.MonetizationOn,
          contentDescription = null,
          tint = if (canAfford) RoyalGold else TextMuted,
          modifier = Modifier.size(12.dp),
        )
      }
      Text(
        text =
          when {
            equipped -> stringResource(R.string.label_equipped)
            unlocked -> stringResource(R.string.label_unlocked)
            else -> "$price"
          },
        color =
          when {
            equipped -> ObsidianBg
            unlocked -> CyanAccent
            canAfford -> RoyalGoldBright
            else -> TextMuted
          },
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
      )
    }
  }
}

@Composable
fun StrikerDiscPreview(skin: StrikerSkin, modifier: Modifier = Modifier) {
  val baseLight = remember(skin.baseLightHex) { Color(AndroidColor.parseColor(skin.baseLightHex)) }
  val baseEdge = remember(skin.baseEdgeHex) { Color(AndroidColor.parseColor(skin.baseEdgeHex)) }
  val outerRing = remember(skin.outerRingHex) { Color(AndroidColor.parseColor(skin.outerRingHex)) }
  val innerRing = remember(skin.innerRingHex) { Color(AndroidColor.parseColor(skin.innerRingHex)) }
  val centerDot = remember(skin.centerDotHex) { Color(AndroidColor.parseColor(skin.centerDotHex)) }

  Canvas(modifier = modifier) {
    val radius = size.minDimension / 2f
    drawCircle(
      brush = Brush.radialGradient(colors = listOf(baseLight, baseEdge), center = center, radius = radius),
      radius = radius,
      center = center,
    )
    drawCircle(
      color = outerRing,
      radius = radius * 0.78f,
      center = center,
      style = Stroke(width = radius * 0.14f),
    )
    drawCircle(
      color = innerRing,
      radius = radius * 0.50f,
      center = center,
      style = Stroke(width = radius * 0.10f),
    )
    drawCircle(color = centerDot, radius = radius * 0.18f, center = center)
  }
}

@Composable
fun RewardsTabContent(
  canSpin: Boolean,
  onSpinTick: () -> Unit,
  onClaimReward: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val prizes = remember { listOf(50, 150, 75, 250, 100, 500, 120, 200) }
  val wheelAngle = remember { Animatable(0f) }
  var spinning by remember { mutableStateOf(false) }
  var lastWonPrize by remember { mutableStateOf<Int?>(null) }
  val scope = rememberCoroutineScope()

  MahoganyGlassCard(
    modifier = modifier.fillMaxWidth().widthIn(max = 460.dp),
    cornerRadius = 24.dp,
    glowAlpha = 0.28f,
    elevation = 18.dp,
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.daily_spin_title),
        color = RoyalGoldBright,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
      )
      Text(
        text =
          if (canSpin) stringResource(R.string.daily_spin_ready)
          else stringResource(R.string.daily_spin_claimed),
        color = TextMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
      )

      Box(modifier = Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val sweep = 360f / prizes.size
          rotate(wheelAngle.value) {
            for (i in prizes.indices) {
              val start = i * sweep - 90f - sweep / 2f
              val wedgeCol =
                when {
                  prizes[i] == 500 -> CrimsonQueen
                  i % 2 == 0 -> RosewoodDark
                  else -> RoyalGoldDeep
                }
              drawArc(color = wedgeCol, startAngle = start, sweepAngle = sweep, useCenter = true)
              drawArc(
                color = RoyalGold,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                style = Stroke(width = 1.5.dp.toPx()),
              )
            }
          }
          drawCircle(
            color = RoyalGold,
            radius = size.minDimension / 2f,
            style = Stroke(width = 5.dp.toPx()),
          )
          drawCircle(color = ObsidianCard, radius = 20.dp.toPx())
          drawCircle(color = RoyalGold, radius = 20.dp.toPx(), style = Stroke(width = 3.dp.toPx()))
          val ptr =
            Path().apply {
              moveTo(center.x, 18.dp.toPx())
              lineTo(center.x - 10.dp.toPx(), -2.dp.toPx())
              lineTo(center.x + 10.dp.toPx(), -2.dp.toPx())
              close()
            }
          drawPath(ptr, color = CyanAccent)
        }

        Text(
          text = lastWonPrize?.let { "+$it" } ?: "SPIN",
          color = RoyalGoldBright,
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(50, 150, 250, 500).forEach { p ->
          Surface(
            shape = CircleShape,
            color = ObsidianBg.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
          ) {
            Text(
              text = "$p",
              color = if (p == 500) CrimsonQueen else RoyalGoldBright,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      RoyalMetallicButton(
        onClick = {
          if (!canSpin || spinning) return@RoyalMetallicButton
          spinning = true
          scope.launch {
            val winIdx = Random.nextInt(prizes.size)
            val sweep = 360f / prizes.size
            val targetAngle = wheelAngle.value + 360f * 5f + (360f - winIdx * sweep)
            var lastTickStep = (wheelAngle.value / sweep).toInt()
            wheelAngle.animateTo(
              targetValue = targetAngle,
              animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            ) {
              val curStep = (value / sweep).toInt()
              if (curStep != lastTickStep) {
                lastTickStep = curStep
                onSpinTick()
              }
            }
            val won = prizes[winIdx]
            lastWonPrize = won
            onClaimReward(won)
            spinning = false
          }
        },
        enabled = canSpin && !spinning,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("daily_spin_button"),
      ) {
        Text(
          text = if (spinning) "SPINNING…" else if (canSpin) stringResource(R.string.btn_spin_now) else "CLAIMED TODAY",
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp,
        )
      }
    }
  }
}

@Composable
fun ChallengesPickerDialog(
  highestUnlocked: Int,
  completedChallenges: Set<Int>,
  onSelectChallenge: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    MahoganyGlassCard(
      modifier = Modifier.fillMaxWidth().height(500.dp),
      cornerRadius = 24.dp,
      glowAlpha = 0.28f,
      elevation = 22.dp,
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "50 TRICK-SHOT CHALLENGES",
              color = RoyalGoldBright,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
            )
            Text(
              text = "${completedChallenges.size}/50 Cleared • Earn Bonus Coins",
              color = TextMuted,
              fontSize = 11.sp,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
          columns = GridCells.Fixed(5),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f).testTag("challenges_grid"),
        ) {
          items(TrickChallengeCatalog.all, key = { it.level }) { ch ->
            val unlocked = ch.level <= highestUnlocked
            val done = ch.level in completedChallenges
            val interactionSource = remember { MutableInteractionSource() }
            val scale = if (unlocked) rememberBounceScale(interactionSource) else 1f
            Surface(
              modifier =
                Modifier.height(54.dp)
                  .scale(scale)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = RoyalGold),
                    enabled = unlocked,
                  ) { onSelectChallenge(ch.level) }
                  .alpha(if (unlocked) 1f else 0.45f)
                  .testTag("challenge_tile_${ch.level}"),
              shape = RoundedCornerShape(12.dp),
              color =
                when {
                  done -> EmeraldWin.copy(alpha = 0.20f)
                  unlocked -> RoyalGold.copy(alpha = 0.16f)
                  else -> ObsidianBg.copy(alpha = 0.72f)
                },
              border =
                BorderStroke(
                  1.2.dp,
                  when {
                    done -> EmeraldWin
                    unlocked -> RoyalGold.copy(alpha = 0.75f)
                    else -> Color.White.copy(alpha = 0.1f)
                  },
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(text = "${ch.level}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Icon(
                  imageVector =
                    when {
                      done -> Icons.Filled.CheckCircle
                      unlocked -> Icons.Filled.Star
                      else -> Icons.Filled.Lock
                    },
                  contentDescription = null,
                  tint =
                    when {
                      done -> EmeraldWin
                      unlocked -> RoyalGold
                      else -> TextMuted
                    },
                  modifier = Modifier.size(12.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsDialog(
  state: UiHudState,
  onToggleSound: () -> Unit,
  onToggleMusic: () -> Unit,
  onSensitivityChanged: (Float) -> Unit,
  onDifficultySelected: (AiDifficulty) -> Unit,
  onLinkAccount: () -> Unit,
  onLogOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    MahoganyGlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      glowAlpha = 0.28f,
      elevation = 22.dp,
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.settings_title),
            color = RoyalGoldBright,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary)
          }
        }

        // Royal Account Section
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = ObsidianBg.copy(alpha = 0.72f),
          border = BorderStroke(1.2.dp, RoyalGold.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("settings_account_card"),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "ROYAL ACCOUNT",
              color = RoyalGoldBright,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
            )
            val statusText =
              when (state.authProvider) {
                AuthProvider.GOOGLE -> "Linked with Google • ${state.accountEmail.ifBlank { state.playerName }}"
                AuthProvider.EMAIL -> "Signed In (Email) • ${state.accountEmail}"
                AuthProvider.GUEST -> "Guest Account (Offline) — Link Email to Save Progress"
                AuthProvider.NONE -> "Not Signed In"
              }
            Text(
              text = statusText,
              color = if (state.authProvider == AuthProvider.GUEST) AmberGold else EmeraldWin,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (state.authProvider == AuthProvider.GUEST) {
                RoyalMetallicButton(
                  onClick = onLinkAccount,
                  style = RoyalBevelStyle.GOLD,
                  modifier = Modifier.weight(1f).height(48.dp).testTag("settings_link_account_button"),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                  Text(text = "LINK ACCOUNT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
              }
              RoyalMetallicButton(
                onClick = onLogOut,
                style = RoyalBevelStyle.CRIMSON,
                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_logout_button"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              ) {
                Text(text = "LOG OUT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = stringResource(R.string.setting_sound), color = TextPrimary, fontWeight = FontWeight.Bold)
          Switch(
            checked = state.soundEnabled,
            onCheckedChange = { onToggleSound() },
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = RoyalGold),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = stringResource(R.string.setting_music), color = TextPrimary, fontWeight = FontWeight.Bold)
          Switch(
            checked = state.musicEnabled,
            onCheckedChange = { onToggleMusic() },
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = RoyalGold),
          )
        }

        Column {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.setting_sensitivity), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = "${(state.cameraSensitivity * 100).toInt()}%", color = CyanAccent, fontWeight = FontWeight.Black, fontSize = 13.sp)
          }
          Slider(
            value = state.cameraSensitivity,
            onValueChange = onSensitivityChanged,
            valueRange = 0.6f..1.6f,
            colors = SliderDefaults.colors(thumbColor = RoyalGold, activeTrackColor = RoyalGold),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = stringResource(R.string.setting_ai_difficulty), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiDifficulty.entries.forEach { diff ->
              val selected = state.aiDifficulty == diff
              val interactionSource = remember { MutableInteractionSource() }
              val scale = rememberBounceScale(interactionSource)
              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(48.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                      if (selected) Modifier.background(RoyalBrushes.goldMetallic145)
                      else Modifier.background(ObsidianBg.copy(alpha = 0.7f))
                    )
                    .clickable(
                      interactionSource = interactionSource,
                      indication = ripple(color = RoyalGold),
                    ) { onDifficultySelected(diff) }
                    .testTag("ai_diff_${diff.name.lowercase()}"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = diff.label,
                  color = if (selected) ObsidianBg else TextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ProfileEditDialog(
  currentName: String,
  currentAvatarId: Int,
  onSave: (String, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember(currentName) { mutableStateOf(currentName) }
  var avatar by remember(currentAvatarId) { mutableStateOf(currentAvatarId) }
  val avatars = remember { listOf("👑", "🦁", "🦅", "⚡", "🔥", "🎯") }

  Dialog(onDismissRequest = onDismiss) {
    MahoganyGlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      glowAlpha = 0.28f,
      elevation = 22.dp,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.profile_edit_title),
          color = RoyalGoldBright,
          fontSize = 16.sp,
          fontWeight = FontWeight.Black,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 18) name = it },
          label = { Text("Player Name") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = RoyalGold,
              unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedLabelColor = RoyalGold,
              unfocusedLabelColor = TextMuted,
            ),
          modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
        )
        Text(text = "Select Royal Avatar", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          avatars.forEachIndexed { idx, glyph ->
            val selected = avatar == idx
            val interactionSource = remember { MutableInteractionSource() }
            val scale = rememberBounceScale(interactionSource)
            Surface(
              modifier =
                Modifier.size(48.dp)
                  .scale(scale)
                  .clip(CircleShape)
                  .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = RoyalGold),
                  ) { avatar = idx },
              shape = CircleShape,
              color = if (selected) RoyalGold.copy(alpha = 0.24f) else ObsidianBg,
              border = BorderStroke(1.5.dp, if (selected) RoyalGold else Color.White.copy(alpha = 0.14f)),
            ) {
              Box(contentAlignment = Alignment.Center) { Text(text = glyph, fontSize = 20.sp) }
            }
          }
        }
        RoyalMetallicButton(
          onClick = {
            onSave(name, avatar)
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
        ) {
          Text(text = "SAVE PROFILE", fontWeight = FontWeight.Black)
        }
      }
    }
  }
}
