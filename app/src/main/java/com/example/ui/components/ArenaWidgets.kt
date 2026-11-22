package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HighScoreEntity
import com.example.ui.theme.ArenaBlueBottom
import com.example.ui.theme.ArenaBlueShadow
import com.example.ui.theme.ArenaBlueText
import com.example.ui.theme.ArenaBlueTop
import com.example.ui.theme.ArenaCyan
import com.example.ui.theme.ArenaGold
import com.example.ui.theme.ArenaGoldBottom
import com.example.ui.theme.ArenaGoldShadow
import com.example.ui.theme.ArenaGoldText
import com.example.ui.theme.ArenaGoldTop
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaLine
import com.example.ui.theme.ArenaMute
import com.example.ui.theme.ArenaPanel
import com.example.ui.theme.ArenaRed
import com.example.ui.theme.ArenaSilverBottom
import com.example.ui.theme.ArenaSilverShadow
import com.example.ui.theme.ArenaSilverText
import com.example.ui.theme.ArenaSilverTop
import com.example.ui.theme.ArenaTxt
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RajdhaniFamily

enum class ArenaButtonVariant {
    GOLD,
    BLUE,
    SILVER,
    GHOST,
    TOGGLE
}

@Composable
fun ArenaCardContainer(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .shadow(24.dp, shape)
            .clip(shape)
            .background(ArenaPanel)
            .border(1.dp, ArenaLine, shape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
fun ArenaLogoText(
    firstWord: String = "KICKOFF",
    goldWord: String = "ARENA",
    fontSizeSp: Float = 18f,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) {
                append("$firstWord ")
            }
            withStyle(SpanStyle(color = ArenaGold)) {
                append(goldWord)
            }
        },
        fontFamily = OrbitronFamily,
        fontWeight = FontWeight.Black,
        fontSize = fontSizeSp.sp,
        modifier = modifier
    )
}

@Composable
fun Arena3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArenaButtonVariant = ArenaButtonVariant.SILVER,
    testTag: String? = null,
    activeBorderColor: Color? = null,
    fontSizeSp: Float = 11.5f
) {
    val shape = RoundedCornerShape(10.dp)
    val mod = if (testTag != null) modifier.testTag(testTag) else modifier

    when (variant) {
        ArenaButtonVariant.GOLD,
        ArenaButtonVariant.BLUE,
        ArenaButtonVariant.SILVER -> {
            val topCol = when (variant) {
                ArenaButtonVariant.GOLD -> ArenaGoldTop
                ArenaButtonVariant.BLUE -> ArenaBlueTop
                else -> ArenaSilverTop
            }
            val botCol = when (variant) {
                ArenaButtonVariant.GOLD -> ArenaGoldBottom
                ArenaButtonVariant.BLUE -> ArenaBlueBottom
                else -> ArenaSilverBottom
            }
            val shadowCol = when (variant) {
                ArenaButtonVariant.GOLD -> ArenaGoldShadow
                ArenaButtonVariant.BLUE -> ArenaBlueShadow
                else -> ArenaSilverShadow
            }
            val txtCol = when (variant) {
                ArenaButtonVariant.GOLD -> ArenaGoldText
                ArenaButtonVariant.BLUE -> ArenaBlueText
                else -> ArenaSilverText
            }
            Box(
                modifier = mod
                    .heightIn(min = 38.dp)
                    .clip(shape)
                    .background(shadowCol)
                    .clickable(onClick = onClick)
                    .padding(bottom = 3.dp)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(topCol, botCol)))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = txtCol,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSizeSp.sp,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        ArenaButtonVariant.TOGGLE -> {
            Box(
                modifier = mod
                    .heightIn(min = 34.dp)
                    .clip(shape)
                    .background(Color(0xCC0C1830))
                    .border(1.dp, activeBorderColor ?: ArenaCyan, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = ArenaCyan,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        ArenaButtonVariant.GHOST -> {
            Box(
                modifier = mod
                    .heightIn(min = 36.dp)
                    .clip(shape)
                    .background(Color.Transparent)
                    .border(1.dp, activeBorderColor ?: ArenaLine, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (activeBorderColor != null) ArenaTxt else ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSizeSp.sp,
                    letterSpacing = 0.6.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HighScoresBox(
    leftTitle: String,
    rightTitle: String? = null,
    scores: List<HighScoreEntity>,
    highlightId: Long = -1L,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = leftTitle,
                color = ArenaGold,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.4.sp
            )
            if (rightTitle != null) {
                Text(
                    text = rightTitle,
                    color = ArenaCyan,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp
                )
            }
        }

        if (scores.isEmpty()) {
            HorizontalDivider(color = ArenaLine, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records yet. Win a match!",
                    color = ArenaMute,
                    fontFamily = RajdhaniFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        } else {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("#", Modifier.weight(0.6f))
                HeaderCell("PLAYER", Modifier.weight(2.0f))
                HeaderCell("RES", Modifier.weight(0.9f))
                HeaderCell("SCORE", Modifier.weight(1.1f))
                HeaderCell("PTS", Modifier.weight(1.1f), TextAlign.End)
            }
            HorizontalDivider(color = ArenaLine, thickness = 1.dp)

            scores.take(6).forEachIndexed { idx, row ->
                val isHighlighted = row.id.toLong() == highlightId
                val baseColor = if (isHighlighted) ArenaGold else ArenaTxt
                val resColor = when (row.result) {
                    "W" -> ArenaGreen
                    "L" -> ArenaRed
                    else -> ArenaMute
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BodyCell("${idx + 1}", Modifier.weight(0.6f), baseColor, isHighlighted)
                    BodyCell(row.playerName, Modifier.weight(2.0f), baseColor, isHighlighted)
                    BodyCell(row.result, Modifier.weight(0.9f), if (isHighlighted) ArenaGold else resColor, true)
                    BodyCell(row.scoreText, Modifier.weight(1.1f), baseColor, isHighlighted)
                    BodyCell("${row.points}", Modifier.weight(1.1f), baseColor, isHighlighted, TextAlign.End)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.04f), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        color = ArenaMute,
        fontFamily = OrbitronFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
private fun BodyCell(
    text: String,
    modifier: Modifier,
    color: Color,
    bold: Boolean,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        color = color,
        fontFamily = RajdhaniFamily,
        fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
fun FuturisticAudioSliderCard(
    icon: String,
    title: String,
    subtitle: String? = null,
    valuePercent: Int,
    actionButtonText: String,
    onActionClick: () -> Unit,
    onValueChange: (Int) -> Unit,
    sliderTag: String,
    buttonTag: String
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xBF000E1E))
            .border(1.dp, ArenaCyan.copy(alpha = 0.25f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, fontSize = 11.sp)
                Text(
                    text = title,
                    color = ArenaTxt,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = ArenaCyan,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gold value badge
                val badgeShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .widthIn(min = 44.dp)
                        .clip(badgeShape)
                        .background(ArenaGold.copy(alpha = 0.15f))
                        .border(1.dp, ArenaGold.copy(alpha = 0.3f), badgeShape)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$valuePercent%",
                        color = ArenaGold,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }

                // Mini test button
                val btnShape = RoundedCornerShape(7.dp)
                Box(
                    modifier = Modifier
                        .testTag(buttonTag)
                        .heightIn(min = 26.dp)
                        .clip(btnShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color(0x4D8CC8FF), btnShape)
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionButtonText,
                        color = ArenaTxt,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Slider(
            value = valuePercent.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = ArenaGold,
                activeTrackColor = ArenaCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag(sliderTag)
        )
    }
}
