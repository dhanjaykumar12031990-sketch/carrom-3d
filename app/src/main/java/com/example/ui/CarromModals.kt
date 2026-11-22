package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameMode
import com.example.game.UiHudState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun GameOverOverlay(
  state: UiHudState,
  onPlayAgain: () -> Unit,
  onNextChallenge: () -> Unit,
  onMainMenu: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(ObsidianBg.copy(alpha = 0.85f)).padding(18.dp),
    contentAlignment = Alignment.Center,
  ) {
    MahoganyGlassCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 385.dp),
      cornerRadius = 24.dp,
      glowAlpha = 0.30f,
      elevation = 22.dp,
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(shape = CircleShape, color = RoyalGold.copy(alpha = 0.18f), border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.5f))) {
          Text(
            text = stringResource(R.string.match_complete),
            color = RoyalGold,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          )
        }

        if (state.isNewHighScore) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = CircleShape,
            color = AmberGold.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
            modifier = Modifier.testTag("new_high_score_badge"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = stringResource(R.string.new_high_score_badge),
                color = AmberGold,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = state.winnerTitle, fontSize = 23.sp, fontWeight = FontWeight.Black, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = state.winnerSubtitle, fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))

        if (state.rewardCoinsEarned > 0) {
          Surface(
            shape = CircleShape,
            color = EmeraldWin.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "+${state.rewardCoinsEarned} ROYAL COINS EARNED",
                color = EmeraldWin,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        if (state.mode == GameMode.DESI_ADDA_FFA) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..3) {
              SummaryScoreBox(
                label = "P${i + 1}",
                score = state.scores[i],
                color = if (i % 2 == 0) CyanAccent else RoyalGold,
                modifier = Modifier.weight(1f),
              )
            }
          }
        } else if (state.mode != GameMode.TRICK_CHALLENGE) {
          val s1 = if (state.mode == GameMode.DESI_ADDA_2V2) state.scores[0] + state.scores[2] else state.scores[0]
          val s2 = if (state.mode == GameMode.DESI_ADDA_2V2) state.scores[1] + state.scores[3] else state.scores[1]
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryScoreBox(
              label = if (state.mode == GameMode.DESI_ADDA_2V2) "TEAM A" else state.playerName,
              score = s1,
              color = CyanAccent,
              modifier = Modifier.weight(1f),
            )
            SummaryScoreBox(
              label = if (state.mode == GameMode.DESI_ADDA_2V2) "TEAM B" else stringResource(R.string.label_ai),
              score = s2,
              color = RoyalGold,
              modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val clearedChallenge =
          state.mode == GameMode.TRICK_CHALLENGE &&
            state.rewardCoinsEarned > 0 &&
            state.challengeLevel < 50

        if (clearedChallenge) {
          RoyalMetallicButton(
            onClick = onNextChallenge,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("next_challenge_button"),
          ) {
            Text(text = stringResource(R.string.btn_next_challenge), color = ObsidianBg, fontWeight = FontWeight.Black)
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        RoyalMetallicButton(
          onClick = onPlayAgain,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("play_again_button"),
        ) {
          Text(text = stringResource(R.string.btn_play_again), color = ObsidianBg, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onMainMenu,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("main_menu_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.2.dp, RoyalGold.copy(alpha = 0.45f)),
        ) {
          Text(text = stringResource(R.string.btn_main_menu), color = TextPrimary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SummaryScoreBox(label: String, score: Int, color: Color, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = ObsidianBg.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.30f)),
  ) {
    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = label, color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      Spacer(modifier = Modifier.height(3.dp))
      Text(text = score.toString(), color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
  }
}
