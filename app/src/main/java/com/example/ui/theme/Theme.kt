package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    onPrimary = Color(0xFF050505),
    secondary = ElegantDarkSecondary,
    onSecondary = Color.White,
    tertiary = ElegantDarkTertiary,
    onTertiary = Color.White,
    background = ElegantDarkBg,
    onBackground = ElegantDarkPrimary,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkPrimary,
    error = CrimsonRed,
    onError = Color.White,
    outline = ElegantDarkOutline
)

private val LightColorScheme = DarkColorScheme


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
