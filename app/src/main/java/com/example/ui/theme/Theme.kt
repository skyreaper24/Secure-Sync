package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StandardDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = BentoOnPrimary,
    onSecondary = Color(0xFF211F26),
    onBackground = BentoText,
    onSurface = BentoText,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoText,
    outline = BentoBorder
)

private val StandardLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SlateGray,
    tertiary = AccentCyan,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = HighContrastDarkPrimary,
    secondary = HighContrastDarkSecondary,
    tertiary = HighContrastDarkPrimary,
    background = HighContrastDarkBg,
    surface = HighContrastDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastLightPrimary,
    secondary = HighContrastLightSecondary,
    tertiary = HighContrastLightPrimary,
    background = HighContrastLightBg,
    surface = HighContrastLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isHighContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isHighContrast && darkTheme -> HighContrastDarkColorScheme
        isHighContrast && !darkTheme -> HighContrastLightColorScheme
        darkTheme -> StandardDarkColorScheme
        else -> StandardLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
