package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KickoffColorScheme = darkColorScheme(
    primary = ArenaGold,
    onPrimary = ArenaGoldText,
    secondary = ArenaBlue,
    onSecondary = ArenaBlueText,
    tertiary = ArenaCyan,
    background = ArenaBg,
    onBackground = ArenaTxt,
    surface = ArenaPanel,
    onSurface = ArenaTxt,
    surfaceVariant = ArenaOuterBg,
    onSurfaceVariant = ArenaMute,
    error = ArenaRed,
    outline = ArenaLine
)

@Composable
fun KickoffArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KickoffColorScheme,
        typography = Typography,
        content = content
    )
}
