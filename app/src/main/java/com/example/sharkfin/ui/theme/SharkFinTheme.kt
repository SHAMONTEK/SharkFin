package com.example.sharkfin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SharkGold,
    onPrimary = SharkBase,
    secondary = SharkGoldSoft,
    onSecondary = SharkBase,
    tertiary = SharkPositive,
    background = SharkBase,
    surface = SharkSurface,
    onBackground = SharkTextPrimary,
    onSurface = SharkTextPrimary,
    error = SharkNegative,
    outline = SharkBorderMedium
)

@Composable
fun SharkFinTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SharkTypography,
        content = content
    )
}
