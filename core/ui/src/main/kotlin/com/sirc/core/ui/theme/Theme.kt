package com.sirc.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = SircColors.Accent,
        onPrimary = Color.White,
        secondary = SircColors.Profit,
        background = SircColors.Background,
        surface = SircColors.Surface,
        onBackground = SircColors.OnDark,
        onSurface = SircColors.OnDark,
        surfaceVariant = SircColors.SurfaceHigh,
        onSurfaceVariant = SircColors.OnDarkMuted,
        error = SircColors.NotProfit,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = SircColors.Accent,
        secondary = SircColors.Profit,
    )

@Composable
fun SircTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = SircTypography,
        content = content,
    )
}
