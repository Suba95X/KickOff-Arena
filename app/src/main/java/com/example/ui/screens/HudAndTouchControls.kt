package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ArenaScreenState
import com.example.game.ArenaViewModel
import com.example.game.MatchRole
import com.example.ui.components.parseHexColor
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCyan
import com.example.ui.theme.ArenaFire
import com.example.ui.theme.ArenaGold
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaLine
import com.example.ui.theme.ArenaMute
import com.example.ui.theme.ArenaRed
import com.example.ui.theme.ArenaTxt
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RajdhaniFamily
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dedicated top broadcast bar placed ABOVE the match pitch so it NEVER overlaps the gameplay field.
 * - Left: Live match status pill or active power-up boost timers
 * - Center: Sleek compact broadcast scoreboard (Team 0 | Clock | Team 1)
 * - Right: Top-side Settings & quick control options (Auto-Rotate, Settings, Sound, Pause)
 */
@Composable
fun MatchTopHeaderBar(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !viewModel.isPaused && !viewModel.showSettingsModal) {
        viewModel.togglePause()
    }

    // Observe frameTick for live clock and boost pill countdown updates
    val _tick = viewModel.frameTick
    val sec = max(0, ceil(viewModel.matchTime).toInt())
    val clockText = when {
        viewModel.screenState == ArenaScreenState.COUNTDOWN -> "READY"
        viewModel.suddenDeath -> "SUDDEN"
        else -> "${sec / 60}:${String.format(Locale.US, "%02d", sec % 60)}"
    }
    val modeLabel = when (viewModel.role) {
        MatchRole.HOST -> "PIN ${viewModel.hostPin}"
        MatchRole.GUEST -> "PIN ${viewModel.joinPin}"
        else -> "CPU · ${viewModel.selectedDifficulty.title}"
    }
    val modeDotColor = when {
        viewModel.suddenDeath -> ArenaRed
        viewModel.role == MatchRole.CPU -> viewModel.selectedDifficulty.accentColor
        else -> ArenaGreen
    }
    val mySide = if (viewModel.role == MatchRole.GUEST) 1 else 0
    val name0 = viewModel.teamName0.uppercase() + if (mySide == 0) " ★" else ""
    val name1 = viewModel.teamName1.uppercase() + if (mySide == 1) " ★" else ""
    val isMuted = viewModel.settings.muted || viewModel.settings.masterVolume == 0
    val localPlayer = viewModel.players[viewModel.localId]
    val hasActiveBoost = localPlayer.buffSpeed > 0f || localPlayer.buffKick > 0f

    val team0Accent = remember(viewModel.settings.kitColorHex) {
        parseHexColor(viewModel.settings.kitColorHex, ArenaBlue)
    }
    val team1Accent = remember(viewModel.settings.kitColorHex) {
        if (viewModel.settings.kitColorHex.equals("#ff5364", ignoreCase = true)) {
            ArenaBlue
        } else {
            ArenaRed
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060B18), Color(0xFF0B142A))
                )
            )
            .testTag("hud")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // LEFT ZONE: Active Power-Up Boosts or Compact Live Mode / Difficulty Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("boostBoard"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasActiveBoost) {
                    if (localPlayer.buffSpeed > 0f) {
                        BoostPill(
                            label = "⚡ SPEED",
                            remainingSec = localPlayer.buffSpeed,
                            maxSec = localPlayer.maxSpeedBuff,
                            accentColor = ArenaCyan,
                            gradientColors = listOf(Color(0xFF00F0FF), Color(0xFF7CF9FF))
                        )
                    }
                    if (localPlayer.buffKick > 0f) {
                        BoostPill(
                            label = "🔥 POWER",
                            remainingSec = localPlayer.buffKick,
                            maxSec = localPlayer.maxKickBuff,
                            accentColor = ArenaFire,
                            gradientColors = listOf(Color(0xFFFF4500), Color(0xFFFFB300))
                        )
                    }
                } else {
                    val liveShape = RoundedCornerShape(999.dp)
                    Row(
                        modifier = Modifier
                            .clip(liveShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, modeDotColor.copy(alpha = 0.3f), liveShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(modeDotColor)
                        )
                        Text(
                            text = modeLabel,
                            color = ArenaTxt.copy(alpha = 0.85f),
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // CENTER ZONE: Sleek Compact Broadcast Scoreboard (self-sizing, never overlaps side buttons)
            val boardShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(34.dp)
                    .clip(boardShape)
                    .background(Color(0xFF050914))
                    .border(1.dp, ArenaLine, boardShape)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .testTag("scoreboard"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Team 0 Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(team0Accent)
                    )
                    Text(
                        text = name0,
                        color = ArenaTxt,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 86.dp)
                    )
                    ScoreBadge(score = viewModel.score0, accent = team0Accent)
                }

                // Center Clock Pill
                val clockShape = RoundedCornerShape(8.dp)
                Box(
                    modifier = Modifier
                        .widthIn(min = 66.dp)
                        .fillMaxHeight()
                        .clip(clockShape)
                        .background(
                            if (viewModel.suddenDeath) {
                                ArenaRed.copy(alpha = 0.18f)
                            } else {
                                ArenaGold.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (viewModel.suddenDeath) {
                                ArenaRed.copy(alpha = 0.45f)
                            } else {
                                ArenaGold.copy(alpha = 0.3f)
                            },
                            shape = clockShape
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = clockText,
                        color = if (viewModel.suddenDeath) ArenaRed else ArenaGold,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.6.sp
                    )
                }

                // Team 1 Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    ScoreBadge(score = viewModel.score1, accent = team1Accent)
                    Text(
                        text = name1,
                        color = ArenaTxt,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 86.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(team1Accent)
                    )
                }
            }

            // RIGHT ZONE: Top-Side Settings & Options Cluster (outside the match pitch!)
            val clusterShape = RoundedCornerShape(11.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(clusterShape)
                    .background(Color(0xCC050914))
                    .border(1.dp, ArenaLine.copy(alpha = 0.85f), clusterShape)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActionIcon(
                    icon = if (viewModel.settings.autoRotateEnabled) {
                        Icons.Rounded.ScreenRotation
                    } else {
                        Icons.Rounded.ScreenLockLandscape
                    },
                    contentDescription = "Toggle Auto-Rotate",
                    tint = if (viewModel.settings.autoRotateEnabled) ArenaCyan else ArenaMute,
                    highlight = viewModel.settings.autoRotateEnabled,
                    testTag = "btnRotToggle",
                    onClick = { viewModel.toggleAutoRotate() }
                )
                TopBarActionIcon(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings Hub",
                    tint = ArenaTxt,
                    highlight = false,
                    testTag = "btnQuickSettings",
                    onClick = { viewModel.openSettingsModal() }
                )
                TopBarActionIcon(
                    icon = if (isMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = if (isMuted) "Unmute Sound" else "Mute Sound",
                    tint = if (isMuted) ArenaRed else ArenaTxt,
                    highlight = isMuted,
                    highlightColor = ArenaRed,
                    testTag = "btnMute",
                    onClick = { viewModel.toggleMute() }
                )
                TopBarActionIcon(
                    icon = Icons.Rounded.Pause,
                    contentDescription = "Pause Match",
                    tint = ArenaGold,
                    highlight = true,
                    highlightColor = ArenaGold,
                    testTag = "btnPause",
                    onClick = { viewModel.togglePause() }
                )
            }
        }

        // Crisp subtle hairline divider separating top bar from clean pitch area below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ArenaCyan.copy(alpha = 0.35f),
                            ArenaLine,
                            ArenaCyan.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun ScoreBadge(
    score: Int,
    accent: Color
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .widthIn(min = 26.dp)
            .height(24.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            color = accent,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TopBarActionIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    highlight: Boolean,
    testTag: String,
    onClick: () -> Unit,
    highlightColor: Color = ArenaCyan
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .testTag(testTag)
            .clip(shape)
            .background(
                if (highlight) highlightColor.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (highlight) highlightColor.copy(alpha = 0.45f)
                else Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun MatchBottomHint(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val hintShape = RoundedCornerShape(999.dp)
        Box(
            modifier = Modifier
                .clip(hintShape)
                .background(Color(0xD9060A16))
                .border(1.dp, ArenaLine, hintShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Left Stick to steer · Tap SHOOT & SPRINT",
                color = ArenaTxt,
                fontFamily = RajdhaniFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun BoostPill(
    label: String,
    remainingSec: Float,
    maxSec: Float,
    accentColor: Color,
    gradientColors: List<Color>
) {
    val shape = RoundedCornerShape(8.dp)
    val pct = (remainingSec / maxSec).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(shape)
            .background(Color(0xEB050914))
            .border(1.dp, accentColor.copy(alpha = 0.85f), shape)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = accentColor,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Black,
                fontSize = 7.5.sp
            )
            Text(
                text = String.format(Locale.US, "%.1fs", remainingSec),
                color = Color.White,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(gradientColors))
            )
        }
    }
}

@Composable
fun TouchControlsOverlay(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseSizeDp = 132.dp
    val knobSizeDp = 56.dp
    val maxKnobDistPx = with(density) { 42.dp.toPx() }
    val baseSizePx = with(density) { baseSizeDp.toPx() }

    var zoneSize by remember { mutableStateOf(IntSize.Zero) }
    var isTouchingStick by remember { mutableStateOf(false) }
    var floatingOriginPx by remember { mutableStateOf(Offset.Zero) }
    var knobOffsetPx by remember { mutableStateOf(Offset.Zero) }

    val isFloating = viewModel.settings.joystickFloating

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("touch_controls")
    ) {
        // Left Analog Joystick Zone (#joyZone)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.46f)
                .fillMaxHeight(0.86f)
                .onGloballyPositioned { zoneSize = it.size }
                .pointerInput(isFloating) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isTouchingStick = true
                        val defaultCenter = Offset(
                            x = with(density) { 16.dp.toPx() } + baseSizePx / 2f,
                            y = zoneSize.height - with(density) { 12.dp.toPx() } - baseSizePx / 2f
                        )
                        val origin = if (isFloating) down.position else defaultCenter
                        floatingOriginPx = origin

                        fun updateFromPos(pos: Offset) {
                            val dx = pos.x - origin.x
                            val dy = pos.y - origin.y
                            val d = hypot(dx, dy)
                            val clampedD = min(d, maxKnobDistPx)
                            val ang = atan2(dy, dx)
                            knobOffsetPx = Offset(cos(ang) * clampedD, sin(ang) * clampedD)
                            if (d < 10f) {
                                viewModel.joyX = 0f
                                viewModel.joyY = 0f
                                viewModel.joyActive = false
                            } else {
                                val amt = clampedD / maxKnobDistPx
                                viewModel.joyX = cos(ang) * amt
                                viewModel.joyY = sin(ang) * amt
                                viewModel.joyActive = true
                            }
                        }

                        updateFromPos(down.position)

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                change.consume()
                                updateFromPos(change.position)
                            }
                        } while (event.changes.any { it.pressed })

                        isTouchingStick = false
                        knobOffsetPx = Offset.Zero
                        viewModel.joyX = 0f
                        viewModel.joyY = 0f
                        viewModel.joyActive = false
                    }
                }
                .testTag("joyZone")
        ) {
            val baseAlpha = when {
                isTouchingStick -> 1f
                isFloating -> 0.28f
                else -> 0.65f
            }
            val baseModifier = if (isFloating && isTouchingStick) {
                Modifier.offset {
                    IntOffset(
                        x = (floatingOriginPx.x - baseSizePx / 2f).roundToInt(),
                        y = (floatingOriginPx.y - baseSizePx / 2f).roundToInt()
                    )
                }
            } else {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp)
            }

            // Joystick Base (#joyBase)
            Box(
                modifier = baseModifier
                    .size(baseSizeDp)
                    .alpha(baseAlpha)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color(0x80060B18),
                                Color(0x2E8CC8FF)
                            )
                        )
                    )
                    .border(
                        width = 2.5.dp,
                        color = if (isTouchingStick) ArenaCyan else Color(0x738CC8FF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Joystick Knob (#knob)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = knobOffsetPx.x.roundToInt(),
                                y = knobOffsetPx.y.roundToInt()
                            )
                        }
                        .size(knobSizeDp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFB5CDE8),
                                    Color(0xFF6989B5)
                                )
                            )
                        )
                        .border(3.dp, Color.White, CircleShape)
                )
            }
        }

        // Right Action Buttons: SPRINT (#sprBtn) and SHOOT (#kickBtn)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
        ) {
            // SPRINT Button (#sprBtn) positioned cleanly above SHOOT button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 98.dp)
                    .size(66.dp)
                    .testTag("sprBtn")
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD8F5FF),
                                Color(0xFF3AA7FF),
                                Color(0xFF0069B5)
                            )
                        )
                    )
                    .border(3.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            viewModel.sprintPressed = true
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            viewModel.sprintPressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Text(
                        text = "SPRINT",
                        color = Color(0xFF001F3B),
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }

            // SHOOT Button (#kickBtn)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(88.dp)
                    .testTag("kickBtn")
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF5B8),
                                Color(0xFFFFAA00),
                                Color(0xFFD46E00)
                            )
                        )
                    )
                    .border(3.5.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            viewModel.triggerShoot()
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚽", fontSize = 22.sp)
                    Text(
                        text = "SHOOT",
                        color = Color(0xFF261200),
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ArenaToastBanner(
    toastMessage: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toastMessage.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .clip(shape)
                .background(ArenaRed.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .testTag("toast")
        ) {
            Text(
                text = toastMessage,
                color = Color.White,
                fontFamily = RajdhaniFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
