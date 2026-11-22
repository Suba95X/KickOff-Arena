package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ArenaViewModel
import com.example.game.CpuDifficulty
import com.example.ui.components.Arena3DButton
import com.example.ui.components.ArenaButtonVariant
import com.example.ui.components.ArenaCardContainer
import com.example.ui.components.ArenaLogoText
import com.example.ui.theme.ArenaCyan
import com.example.ui.theme.ArenaGold
import com.example.ui.theme.ArenaLine
import com.example.ui.theme.ArenaMute
import com.example.ui.theme.ArenaTxt
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RajdhaniFamily

@Composable
fun DifficultySelectOverlayScreen(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.leaveToMenu()
    }

    val selected = viewModel.selectedDifficulty

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xDE060A16), Color(0xF602040A))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("difficulty_screen"),
        contentAlignment = Alignment.Center
    ) {
        val isWideLandscape = maxWidth >= 560.dp

        ArenaCardContainer(
            maxWidth = 780.dp,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaLogoText(firstWord = "SELECT", goldWord = "DIFFICULTY", fontSizeSp = 17f)

                    val badgeShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(ArenaCyan.copy(alpha = 0.1f))
                            .border(1.dp, ArenaCyan.copy(alpha = 0.3f), badgeShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VS CPU · 4 TIERS",
                            color = ArenaCyan,
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.9.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .testTag("btnCloseDifficulty")
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, ArenaLine, CircleShape)
                        .clickable { viewModel.leaveToMenu() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = ArenaMute,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 4 Difficulty Cards (4 columns in 16:9 landscape, or 2x2 grid on narrow screens)
            if (isWideLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CpuDifficulty.entries.forEach { tier ->
                        DifficultyTierCard(
                            tier = tier,
                            isSelected = (tier == selected),
                            onSelect = { viewModel.selectDifficulty(tier) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DifficultyTierCard(
                            tier = CpuDifficulty.EASY,
                            isSelected = (selected == CpuDifficulty.EASY),
                            onSelect = { viewModel.selectDifficulty(CpuDifficulty.EASY) },
                            modifier = Modifier.weight(1f)
                        )
                        DifficultyTierCard(
                            tier = CpuDifficulty.MEDIUM,
                            isSelected = (selected == CpuDifficulty.MEDIUM),
                            onSelect = { viewModel.selectDifficulty(CpuDifficulty.MEDIUM) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DifficultyTierCard(
                            tier = CpuDifficulty.HARD,
                            isSelected = (selected == CpuDifficulty.HARD),
                            onSelect = { viewModel.selectDifficulty(CpuDifficulty.HARD) },
                            modifier = Modifier.weight(1f)
                        )
                        DifficultyTierCard(
                            tier = CpuDifficulty.ULTIMATE,
                            isSelected = (selected == CpuDifficulty.ULTIMATE),
                            onSelect = { viewModel.selectDifficulty(CpuDifficulty.ULTIMATE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Arena3DButton(
                    text = "← BACK",
                    onClick = { viewModel.leaveToMenu() },
                    variant = ArenaButtonVariant.GHOST,
                    testTag = "btnDifficultyBack",
                    modifier = Modifier.width(126.dp)
                )

                Arena3DButton(
                    text = "⚽ KICK OFF · ${selected.title} (${selected.pointsMultiplier}x PTS)",
                    onClick = { viewModel.startCpuMatch(selected) },
                    variant = ArenaButtonVariant.GOLD,
                    testTag = "btnDifficultyKickoff",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DifficultyTierCard(
    tier: CpuDifficulty,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val accent = tier.accentColor
    val bgBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.22f),
                Color(0xE60A1328)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xB3091022),
                Color(0xCC060B18)
            )
        )
    }

    Column(
        modifier = modifier
            .testTag("difficulty_${tier.name.lowercase()}")
            .clip(shape)
            .background(bgBrush)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent else Color.White.copy(alpha = 0.1f),
                shape = shape
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Icon + Points Multiplier Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = tier.icon, fontSize = 18.sp)

            val multShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .clip(multShape)
                    .background(accent.copy(alpha = if (isSelected) 0.26f else 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.45f), multShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${tier.pointsMultiplier}x",
                    color = if (isSelected) Color.White else accent,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp
                )
            }
        }

        // Mode Title & Subtitle
        Text(
            text = tier.title,
            color = if (isSelected) Color.White else accent,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = tier.subtitle.uppercase(),
            color = if (isSelected) ArenaGold else ArenaMute,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        // 4-Segment CPU Skill Meter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (barIdx in 1..4) {
                val active = barIdx <= tier.levelBars
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (active) accent
                            else Color.White.copy(alpha = 0.12f)
                        )
                )
            }
        }

        // Compact AI Stat Summary
        Text(
            text = tier.statSummary,
            color = ArenaTxt.copy(alpha = 0.9f),
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )

        // Short Description
        Text(
            text = tier.description,
            color = ArenaMute,
            fontFamily = RajdhaniFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 13.5.sp,
            textAlign = TextAlign.Center,
            minLines = 3,
            maxLines = 3
        )

        // Selected Pill Footer
        val statusShape = RoundedCornerShape(999.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(statusShape)
                .background(
                    if (isSelected) accent
                    else Color.White.copy(alpha = 0.05f)
                )
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSelected) "SELECTED" else "SELECT",
                color = if (isSelected) Color(0xFF060A16) else ArenaMute,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp,
                letterSpacing = 0.9.sp
            )
        }
    }
}
