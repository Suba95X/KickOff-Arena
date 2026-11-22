package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.ArenaScreenState
import com.example.game.ArenaViewModel
import com.example.ui.components.PitchCanvas
import com.example.ui.screens.ArenaToastBanner
import com.example.ui.screens.DifficultySelectOverlayScreen
import com.example.ui.screens.GameOverOverlayScreen
import com.example.ui.screens.LobbyOverlayScreen
import com.example.ui.screens.MatchBottomHint
import com.example.ui.screens.MatchTopHeaderBar
import com.example.ui.screens.MenuOverlayScreen
import com.example.ui.screens.PauseOverlayScreen
import com.example.ui.screens.RotateNoticeOverlay
import com.example.ui.screens.SettingsModalOverlay
import com.example.ui.screens.TouchControlsOverlay
import com.example.ui.theme.ArenaBg
import com.example.ui.theme.KickoffArenaTheme

private const val TARGET_ASPECT_RATIO = 16f / 9f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveLandscapeBars()
        setContent {
            KickoffArenaTheme {
                KickoffArenaApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveLandscapeBars()
        }
    }

    private fun configureImmersiveLandscapeBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun KickoffArenaApp(
    viewModel: ArenaViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val highScores by viewModel.highScores.collectAsStateWithLifecycle()
    val autoRotateEnabled = viewModel.settings.autoRotateEnabled

    // Launch in landscape and keep auto-rotate (sensorLandscape) synchronized with user preference
    LaunchedEffect(activity, autoRotateEnabled) {
        activity?.requestedOrientation = if (autoRotateEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBg),
        contentAlignment = Alignment.Center
    ) {
        val isPortrait = maxHeight > maxWidth
        val applyVirtualRot = viewModel.forceVirtualLandscape && isPortrait

        // Effective logical bounds after optional virtual landscape rotation
        val availW = if (applyVirtualRot) maxHeight else maxWidth
        val availH = if (applyVirtualRot) maxWidth else maxHeight

        // Exact 16:9 calibrated viewport fitting cleanly within the display
        val stageW = min(availW, availH * TARGET_ASPECT_RATIO)
        val stageH = stageW / TARGET_ASPECT_RATIO

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (applyVirtualRot) {
                        Modifier.virtualLandscapeRotation()
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Centered 16:9 Arena Stage
            Box(
                modifier = Modifier
                    .size(width = stageW, height = stageH)
                    .background(ArenaBg)
                    .windowInsetsPadding(WindowInsets.displayCutout)
            ) {
                val screen = viewModel.screenState
                val inMatch = screen.isInMatch()

                // Dedicated Column: Top Header Bar sits ABOVE the match screen so the pitch stays 100% clean
                Column(modifier = Modifier.fillMaxSize()) {
                    if (inMatch) {
                        MatchTopHeaderBar(viewModel = viewModel)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Clean, unobstructed 60fps Football Pitch Canvas
                        PitchCanvas(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live Match Touch Controls & auto-hiding bottom tip
                        if (inMatch) {
                            TouchControlsOverlay(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MatchBottomHint(
                                visible = viewModel.showBottomHint,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // Menu Screen Overlay (#menu)
                if (screen == ArenaScreenState.MENU) {
                    MenuOverlayScreen(
                        viewModel = viewModel,
                        highScores = highScores
                    )
                }

                // CPU Difficulty Selection Screen (EASY / MEDIUM / HARD / ULTIMATE)
                if (screen == ArenaScreenState.DIFFICULTY_SELECT) {
                    DifficultySelectOverlayScreen(
                        viewModel = viewModel
                    )
                }

                // Match Lobby Screen Overlay (#lobby)
                if (screen == ArenaScreenState.LOBBY) {
                    LobbyOverlayScreen(
                        viewModel = viewModel
                    )
                }

                // Pause Screen Overlay (#pause)
                if (inMatch && viewModel.isPaused) {
                    PauseOverlayScreen(
                        viewModel = viewModel
                    )
                }

                // Game Over Screen Overlay (#over)
                if (screen == ArenaScreenState.OVER) {
                    GameOverOverlayScreen(
                        viewModel = viewModel,
                        highScores = highScores
                    )
                }

                // Settings & Audio Hub Modal (#settingsModal)
                if (viewModel.showSettingsModal) {
                    SettingsModalOverlay(
                        viewModel = viewModel
                    )
                }

                // Rotate to Landscape Notice (#rotNotice)
                if (viewModel.showRotateNotice) {
                    RotateNoticeOverlay(
                        onRequestLandscape = {
                            viewModel.setAutoRotate(true)
                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            viewModel.dismissRotateNotice()
                        },
                        onForceVirtualLandscape = {
                            viewModel.enableForceVirtualLandscape()
                        },
                        onDismiss = {
                            viewModel.dismissRotateNotice()
                        }
                    )
                }

                // Toast Banner (#toast) positioned neatly below the top bar
                ArenaToastBanner(
                    toastMessage = viewModel.toastMessage,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 52.dp)
                )
            }
        }
    }
}

private fun Modifier.virtualLandscapeRotation(): Modifier = this.layout { measurable, constraints ->
    val swappedConstraints = Constraints(
        minWidth = constraints.minHeight,
        maxWidth = constraints.maxHeight,
        minHeight = constraints.minWidth,
        maxHeight = constraints.maxWidth
    )
    val placeable = measurable.measure(swappedConstraints)
    layout(constraints.maxWidth, constraints.maxHeight) {
        val x = (constraints.maxWidth - placeable.width) / 2
        val y = (constraints.maxHeight - placeable.height) / 2
        placeable.placeWithLayer(x, y) {
            rotationZ = 90f
        }
    }
}
