package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HighScoreEntity
import com.example.game.ArenaViewModel
import com.example.game.MatchRole
import com.example.ui.components.Arena3DButton
import com.example.ui.components.ArenaButtonVariant
import com.example.ui.components.ArenaCardContainer
import com.example.ui.components.ArenaLogoText
import com.example.ui.components.FuturisticAudioSliderCard
import com.example.ui.components.HighScoresBox
import com.example.ui.components.parseHexColor
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCyan
import com.example.ui.theme.ArenaFire
import com.example.ui.theme.ArenaGold
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaLine
import com.example.ui.theme.ArenaMute
import com.example.ui.theme.ArenaPinkAlert
import com.example.ui.theme.ArenaRed
import com.example.ui.theme.ArenaTxt
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RajdhaniFamily

@Composable
fun MenuOverlayScreen(
    viewModel: ArenaViewModel,
    highScores: List<HighScoreEntity>,
    modifier: Modifier = Modifier
) {
    val settings = viewModel.settings
    val stickText = if (settings.joystickFloating) {
        "STICK: FLOATING (AUTO-POSITION)"
    } else {
        "STICK: FIXED (LOWER LEFT)"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xD9060A16), Color(0xF502040A))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth >= 560.dp
        ArenaCardContainer(
            maxWidth = 760.dp,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Header: KICKOFF ARENA + MOBILE LANDSCAPE MATCH badge + Rotate button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArenaLogoText(firstWord = "KICKOFF", goldWord = "ARENA", fontSizeSp = 18f)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), badgeShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (settings.autoRotateEnabled) {
                                "16:9 LANDSCAPE · AUTO-ROTATE"
                            } else {
                                "16:9 LANDSCAPE · LOCKED"
                            },
                            color = ArenaMute,
                            fontFamily = RajdhaniFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }

                    // Orientation helper button to preview or trigger landscape rotation
                    val rotBtnShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .testTag("btnOpenRotateNotice")
                            .size(28.dp)
                            .clip(rotBtnShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, ArenaLine, rotBtnShape)
                            .clickable { viewModel.openRotateNotice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⤢",
                            color = ArenaCyan,
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MenuLeftColumn(
                        viewModel = viewModel,
                        stickText = stickText,
                        modifier = Modifier.weight(1.05f)
                    )
                    MenuRightColumn(
                        highScores = highScores,
                        modifier = Modifier.weight(0.95f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MenuLeftColumn(
                        viewModel = viewModel,
                        stickText = stickText,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MenuRightColumn(
                        highScores = highScores,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuLeftColumn(
    viewModel: ArenaViewModel,
    stickText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "PLAYER NICKNAME",
            color = ArenaMute,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.5.sp,
            letterSpacing = 1.5.sp
        )

        val inputShape = RoundedCornerShape(10.dp)
        BasicTextField(
            value = viewModel.nicknameInput,
            onValueChange = { viewModel.onNicknameChange(it) },
            singleLine = true,
            textStyle = TextStyle(
                color = ArenaTxt,
                fontFamily = RajdhaniFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(ArenaGold),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nick_input"),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp)
                        .clip(inputShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, ArenaLine, inputShape)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    innerTextField()
                }
            }
        )

        Arena3DButton(
            text = "⚽ PLAY VS CPU (SOLO ACTION)",
            onClick = { viewModel.openCpuDifficultySelect() },
            variant = ArenaButtonVariant.GOLD,
            testTag = "btnCpu",
            modifier = Modifier.fillMaxWidth()
        )

        Arena3DButton(
            text = "🌐 MULTIPLAYER (HOST / JOIN PIN)",
            onClick = { viewModel.toggleMultiplayerPanel() },
            variant = ArenaButtonVariant.BLUE,
            testTag = "btnMultiToggle",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = viewModel.showMultiplayerPanel) {
            val panelShape = RoundedCornerShape(10.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Arena3DButton(
                    text = "HOST MATCH (GET PIN)",
                    onClick = { viewModel.startHostLobby() },
                    variant = ArenaButtonVariant.BLUE,
                    testTag = "btnHost",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinShape = RoundedCornerShape(10.dp)
                    BasicTextField(
                        value = viewModel.pinInput,
                        onValueChange = { viewModel.onPinInputChange(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = ArenaTxt,
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(ArenaGold),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pinIn"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                                    .clip(pinShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, ArenaLine, pinShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.pinInput.isEmpty()) {
                                    Text(
                                        text = "PIN",
                                        color = ArenaMute.copy(alpha = 0.6f),
                                        fontFamily = OrbitronFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 3.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Arena3DButton(
                        text = "JOIN",
                        onClick = { viewModel.joinMatchWithPin() },
                        variant = ArenaButtonVariant.SILVER,
                        testTag = "btnJoin",
                        modifier = Modifier.width(88.dp)
                    )
                }
            }
        }

        Arena3DButton(
            text = stickText,
            onClick = { viewModel.toggleJoystickMode() },
            variant = ArenaButtonVariant.TOGGLE,
            testTag = "btnJoyModeMenu",
            modifier = Modifier.fillMaxWidth()
        )

        Arena3DButton(
            text = "⚙️ SETTINGS HUB (AUDIO & CONTROLS)",
            onClick = { viewModel.openSettingsModal() },
            variant = ArenaButtonVariant.GHOST,
            fontSizeSp = 10.5f,
            testTag = "btnSettingsHubMenu",
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.menuMsg.isNotEmpty()) {
            Text(
                text = viewModel.menuMsg,
                color = ArenaPinkAlert,
                fontFamily = RajdhaniFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MenuRightColumn(
    highScores: List<HighScoreEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HighScoresBox(
            leftTitle = "🏆 ARENA HIGH SCORES",
            rightTitle = "TOP PLAYERS",
            scores = highScores
        )

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ArenaMute)) {
                    append("Left Stick to steer · Tap ")
                }
                withStyle(SpanStyle(color = ArenaGold, fontWeight = FontWeight.ExtraBold)) {
                    append("SHOOT ⚽")
                }
                withStyle(SpanStyle(color = ArenaMute)) {
                    append(" & ")
                }
                withStyle(SpanStyle(color = ArenaBlue, fontWeight = FontWeight.ExtraBold)) {
                    append("SPRINT ⚡\n")
                }
                withStyle(SpanStyle(color = ArenaMute)) {
                    append("Collect ")
                }
                withStyle(SpanStyle(color = ArenaCyan, fontWeight = FontWeight.ExtraBold)) {
                    append("⚡ SPEED")
                }
                withStyle(SpanStyle(color = ArenaMute)) {
                    append(" & ")
                }
                withStyle(SpanStyle(color = ArenaFire, fontWeight = FontWeight.ExtraBold)) {
                    append("🔥 SUPER KICK")
                }
                withStyle(SpanStyle(color = ArenaMute)) {
                    append(" power-ups!")
                }
            },
            fontFamily = RajdhaniFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LobbyOverlayScreen(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.leaveToMenu()
    }

    val pinText = if (viewModel.role == MatchRole.HOST) viewModel.hostPin else viewModel.joinPin
    val startBtnText = if (viewModel.opponentConnectedInLobby) "START MATCH" else "PLAY VS CPU"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xD9060A16), Color(0xF502040A))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ArenaCardContainer(
            maxWidth = 420.dp,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag("lobby_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArenaLogoText(firstWord = "MATCH", goldWord = "LOBBY", fontSizeSp = 19f)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SHARE THIS 4-DIGIT PIN",
                    color = ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = pinText,
                    color = ArenaGold,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Text(
                    text = viewModel.lobbyMsg,
                    color = if (viewModel.opponentConnectedInLobby) ArenaGreen else ArenaMute,
                    fontFamily = RajdhaniFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Arena3DButton(
                        text = startBtnText,
                        onClick = { viewModel.startFromLobby() },
                        variant = ArenaButtonVariant.GOLD,
                        testTag = "btnStart",
                        modifier = Modifier.weight(1f)
                    )
                    Arena3DButton(
                        text = "CANCEL",
                        onClick = { viewModel.leaveToMenu() },
                        variant = ArenaButtonVariant.GHOST,
                        testTag = "btnBack",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlayScreen(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setPausedState(false)
    }

    val stickText = if (viewModel.settings.joystickFloating) {
        "STICK: FLOATING (AUTO-POSITION)"
    } else {
        "STICK: FIXED (LOWER LEFT)"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xD9060A16), Color(0xF502040A))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ArenaCardContainer(
            maxWidth = 420.dp,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag("pause_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PAUSED",
                    color = ArenaGold,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )

                if (viewModel.role == MatchRole.CPU) {
                    val diff = viewModel.selectedDifficulty
                    val diffShape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .clip(diffShape)
                            .background(diff.accentColor.copy(alpha = 0.14f))
                            .border(1.dp, diff.accentColor.copy(alpha = 0.45f), diffShape)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${diff.icon} VS CPU · ${diff.title}",
                            color = diff.accentColor,
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Arena3DButton(
                    text = "RESUME",
                    onClick = { viewModel.setPausedState(false) },
                    variant = ArenaButtonVariant.GOLD,
                    testTag = "btnResume",
                    modifier = Modifier.fillMaxWidth()
                )

                Arena3DButton(
                    text = "⚙️ AUDIO & GAME SETTINGS",
                    onClick = { viewModel.openSettingsModal() },
                    variant = ArenaButtonVariant.BLUE,
                    testTag = "btnSettingsPause",
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.role == MatchRole.CPU) {
                    Arena3DButton(
                        text = "🎯 CHANGE DIFFICULTY (${viewModel.selectedDifficulty.title})",
                        onClick = { viewModel.openCpuDifficultySelect() },
                        variant = ArenaButtonVariant.TOGGLE,
                        testTag = "btnChangeDifficultyPause",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Arena3DButton(
                    text = stickText,
                    onClick = { viewModel.toggleJoystickMode() },
                    variant = ArenaButtonVariant.TOGGLE,
                    testTag = "btnJoyModePause",
                    modifier = Modifier.fillMaxWidth()
                )

                Arena3DButton(
                    text = "RESTART MATCH",
                    onClick = { viewModel.rematch() },
                    variant = ArenaButtonVariant.SILVER,
                    testTag = "btnRestart",
                    modifier = Modifier.fillMaxWidth()
                )

                Arena3DButton(
                    text = "QUIT TO MENU",
                    onClick = { viewModel.leaveToMenu() },
                    variant = ArenaButtonVariant.GHOST,
                    testTag = "btnQuit",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GameOverOverlayScreen(
    viewModel: ArenaViewModel,
    highScores: List<HighScoreEntity>,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.leaveToMenu()
    }

    val res = viewModel.lastMatchResult
    val resTitle = when (res) {
        "W" -> "VICTORY!"
        "L" -> "DEFEAT"
        else -> "DRAW"
    }
    val resColor = when (res) {
        "W" -> ArenaGreen
        "L" -> ArenaRed
        else -> ArenaGold
    }
    val mySide = if (viewModel.role == MatchRole.GUEST) 1 else 0
    val myScore = if (mySide == 0) viewModel.score0 else viewModel.score1
    val opScore = if (mySide == 0) viewModel.score1 else viewModel.score0
    val myName = if (mySide == 0) viewModel.teamName0 else viewModel.teamName1
    val opName = if (mySide == 0) viewModel.teamName1 else viewModel.teamName0
    val subSummary = "$myName  $myScore : $opScore  $opName"
    val rematchText = if (viewModel.role == MatchRole.GUEST) "REQUEST REMATCH" else "REMATCH"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xD9060A16), Color(0xF502040A))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth >= 540.dp
        ArenaCardContainer(
            maxWidth = 760.dp,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag("over_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resTitle,
                    color = resColor,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
                val diffTag = if (viewModel.role == MatchRole.CPU) {
                    " · ${viewModel.selectedDifficulty.title}"
                } else ""
                Text(
                    text = subSummary + diffTag,
                    color = ArenaMute,
                    fontFamily = RajdhaniFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameOverLeftColumn(
                        points = viewModel.lastEarnedPoints,
                        rematchText = rematchText,
                        onRematch = { viewModel.rematch() },
                        onMenu = { viewModel.leaveToMenu() },
                        modifier = Modifier.weight(1.05f)
                    )
                    Box(modifier = Modifier.weight(0.95f)) {
                        HighScoresBox(
                            leftTitle = "MATCH LEADERBOARD",
                            scores = highScores,
                            highlightId = viewModel.lastSavedScoreId
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameOverLeftColumn(
                        points = viewModel.lastEarnedPoints,
                        rematchText = rematchText,
                        onRematch = { viewModel.rematch() },
                        onMenu = { viewModel.leaveToMenu() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HighScoresBox(
                        leftTitle = "MATCH LEADERBOARD",
                        scores = highScores,
                        highlightId = viewModel.lastSavedScoreId
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverLeftColumn(
    points: Int,
    rematchText: String,
    onRematch: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "+$points PTS",
            color = ArenaGold,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Arena3DButton(
            text = rematchText,
            onClick = onRematch,
            variant = ArenaButtonVariant.GOLD,
            testTag = "btnRematch",
            modifier = Modifier.fillMaxWidth()
        )
        Arena3DButton(
            text = "MENU",
            onClick = onMenu,
            variant = ArenaButtonVariant.GHOST,
            testTag = "btnMenu2",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsModalOverlay(
    viewModel: ArenaViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeSettingsModal()
    }

    val settings = viewModel.settings
    val isMuted = settings.muted || settings.masterVolume == 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD902040A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.closeSettingsModal() }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        ArenaCardContainer(
            maxWidth = 540.dp,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Prevent backdrop dismiss when clicking inside modal
                )
                .verticalScroll(rememberScrollState())
                .testTag("settingsModal")
        ) {
            // Header: SETTINGS & AUDIO HUB + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArenaLogoText(firstWord = "SETTINGS &", goldWord = "AUDIO HUB", fontSizeSp = 16f)

                Box(
                    modifier = Modifier
                        .testTag("btnCloseSettings")
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, ArenaLine, CircleShape)
                        .clickable { viewModel.closeSettingsModal() },
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

            // Audio Volume Sliders Section
            Text(
                text = "🔊 AUDIO VOLUME SLIDERS",
                color = ArenaGold,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FuturisticAudioSliderCard(
                    icon = "🔊",
                    title = "MASTER VOLUME",
                    valuePercent = settings.masterVolume,
                    actionButtonText = if (isMuted) "UNMUTE" else "MUTE",
                    onActionClick = { viewModel.toggleMute() },
                    onValueChange = { viewModel.setVolume("master", it) },
                    sliderTag = "rangeVolMaster",
                    buttonTag = "btnQuickMuteToggle"
                )

                FuturisticAudioSliderCard(
                    icon = "⚽",
                    title = "SFX VOLUME",
                    subtitle = "(Kicks, Tackles, Pickups)",
                    valuePercent = settings.sfxVolume,
                    actionButtonText = "TEST SFX",
                    onActionClick = { viewModel.testSound("superkick") },
                    onValueChange = { viewModel.setVolume("sfx", it) },
                    sliderTag = "rangeVolSfx",
                    buttonTag = "testSfx"
                )

                FuturisticAudioSliderCard(
                    icon = "🏟️",
                    title = "STADIUM CROWD CHEER",
                    subtitle = "(Ambient Loop)",
                    valuePercent = settings.crowdLoopVolume,
                    actionButtonText = "TEST CROWD",
                    onActionClick = { viewModel.testSound("crowd_preview") },
                    onValueChange = { viewModel.setVolume("crowdLoop", it) },
                    sliderTag = "rangeVolCrowdLoop",
                    buttonTag = "testCrowd"
                )

                FuturisticAudioSliderCard(
                    icon = "📢",
                    title = "WHISTLE & REFEREE",
                    subtitle = "(Match Signals)",
                    valuePercent = settings.whistleVolume,
                    actionButtonText = "TEST WHISTLE",
                    onActionClick = { viewModel.testSound("whistle") },
                    onValueChange = { viewModel.setVolume("crowd", it) },
                    sliderTag = "rangeVolCrowd",
                    buttonTag = "testWhistle"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Controls & Gameplay Customization
            Text(
                text = "🎮 GAMEPLAY & CONTROLS",
                color = ArenaCyan,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Orientation & Auto-Rotate (16:9 Landscape)
                Text(
                    text = "SCREEN ORIENTATION (16:9 LANDSCAPE)",
                    color = ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Arena3DButton(
                        text = "Auto-Rotate (Sensor)",
                        onClick = { viewModel.setAutoRotate(true) },
                        variant = ArenaButtonVariant.GHOST,
                        activeBorderColor = if (settings.autoRotateEnabled) ArenaCyan else null,
                        fontSizeSp = 9.5f,
                        testTag = "orient_auto_rotate",
                        modifier = Modifier.weight(1f)
                    )
                    Arena3DButton(
                        text = "Lock Landscape",
                        onClick = { viewModel.setAutoRotate(false) },
                        variant = ArenaButtonVariant.GHOST,
                        activeBorderColor = if (!settings.autoRotateEnabled) ArenaCyan else null,
                        fontSizeSp = 9.5f,
                        testTag = "orient_lock_landscape",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Steering Sensitivity
                Text(
                    text = "STEERING SENSITIVITY",
                    color = ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sensOptions = listOf(
                        0.8f to "0.8x Smooth",
                        1.0f to "1.0x Normal",
                        1.4f to "1.4x Fast"
                    )
                    sensOptions.forEach { (valFloat, label) ->
                        val selected = kotlin.math.abs(settings.sensitivity - valFloat) < 0.05f
                        Arena3DButton(
                            text = label,
                            onClick = { viewModel.setSensitivity(valFloat) },
                            variant = ArenaButtonVariant.GHOST,
                            activeBorderColor = if (selected) ArenaCyan else null,
                            fontSizeSp = 9.5f,
                            testTag = "sens_$valFloat",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Jersey Number
                Text(
                    text = "JERSEY NUMBER",
                    color = ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("7", "9", "10", "11").forEach { num ->
                        val selected = settings.jerseyNumber == num
                        Arena3DButton(
                            text = "#$num",
                            onClick = { viewModel.setJerseyNumber(num) },
                            variant = ArenaButtonVariant.GHOST,
                            activeBorderColor = if (selected) ArenaCyan else null,
                            fontSizeSp = 11f,
                            testTag = "jersey_$num",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Team Kit Color
                Text(
                    text = "TEAM KIT COLOR",
                    color = ArenaMute,
                    fontFamily = OrbitronFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val kitColors = listOf(
                        "#3aa7ff" to "Blue",
                        "#ffd23f" to "Gold",
                        "#2fd28a" to "Green",
                        "#ff5364" to "Red"
                    )
                    kitColors.forEach { (hex, name) ->
                        val selected = settings.kitColorHex.equals(hex, ignoreCase = true)
                        val swatchShape = RoundedCornerShape(6.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("kit_$name")
                                .clip(swatchShape)
                                .background(parseHexColor(hex, ArenaBlue))
                                .border(
                                    width = 2.dp,
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = swatchShape
                                )
                                .clickable { viewModel.setKitColor(hex) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Arena3DButton(
                text = "SAVE & RETURN TO MATCH",
                onClick = { viewModel.closeSettingsModal() },
                variant = ArenaButtonVariant.GOLD,
                testTag = "btnSaveSettings",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RotateNoticeOverlay(
    onRequestLandscape: () -> Unit,
    onForceVirtualLandscape: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "phoneSpin")
    val spinProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinProgress"
    )

    val rotDegrees = when {
        spinProgress < 0.2f -> 0f
        spinProgress < 0.5f -> ((spinProgress - 0.2f) / 0.3f) * 90f
        spinProgress < 0.75f -> 90f
        else -> (1f - ((spinProgress - 0.75f) / 0.25f)) * 90f
    }
    val borderColor = if (spinProgress in 0.45f..0.8f) ArenaGold else ArenaCyan

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFA0E1836), Color(0xFC03050C))
                )
            )
            .padding(20.dp)
            .testTag("rotNotice"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .size(width = 64.dp, height = 110.dp)
                    .graphicsLayer { rotationZ = rotDegrees }
                    .border(3.dp, borderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚽", fontSize = 22.sp)
            }

            Text(
                text = "ROTATE TO LANDSCAPE",
                color = Color.White,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Kickoff Arena is calibrated for mobile landscape football. Rotate your phone sideways!",
                color = ArenaMute,
                fontFamily = RajdhaniFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val mainBtnShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btnReqLandscape")
                        .clip(mainBtnShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD23F), Color(0xFFFF9500))
                            )
                        )
                        .clickable(onClick = onRequestLandscape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ROTATE / FULLSCREEN",
                        color = Color(0xFF080C18),
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                val forceBtnShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btnForceVirtual")
                        .clip(forceBtnShape)
                        .background(ArenaCyan.copy(alpha = 0.1f))
                        .border(1.dp, ArenaCyan, forceBtnShape)
                        .clickable(onClick = onForceVirtualLandscape)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FORCE HORIZONTAL VIEW",
                        color = ArenaCyan,
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
