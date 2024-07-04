package org.paulstudios.datasurvey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryContainer = Color(0xFFD1C4E9),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFFB2EBF2),
    surface = Color.White,
    surfaceTint = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFFE0E0E0),
    background = Color.White,
    onPrimary = Color.Black,
    onPrimaryContainer = Color.Black,
    onSecondary = Color.Black,
    onSecondaryContainer = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    onBackground = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White,
    onErrorContainer = Color(0xFFFFEBEE), // Provide a value for errorContainer
    inverseOnSurface = Color.Black,
    inversePrimary = Color.Black,
    inverseSurface = Color.White,
    onTertiary = Color.Black,
    onTertiaryContainer = Color.White,
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF212121),
    scrim = Color(0x99000000),
    tertiary = Color(0xFF03A9F4),
    tertiaryContainer = Color(0xFFB3E5FC)
)

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFFB2EBF2),
    surface = Color.Black,
    surfaceTint = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    background = Color.Black,
    onPrimary = Color.White,
    onPrimaryContainer = Color.Black,
    onSecondary = Color.White,
    onSecondaryContainer = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    onBackground = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.White,
    onErrorContainer = Color(0xFFB00020), // Provide a value for errorContainer
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
    inverseSurface = Color.Black,
    onTertiary = Color.White,
    onTertiaryContainer = Color.Black,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFF757575),
    scrim = Color(0x99000000),
    tertiary = Color(0xFF03A9F4),
    tertiaryContainer = Color(0xFFB3E5FC)
)

@Composable
fun DataSurveyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = ColorScheme(
            primary = colors.primary,
            primaryContainer = colors.primaryContainer,
            secondary = colors.secondary,
            secondaryContainer = colors.secondaryContainer,
            surface = colors.surface,
            surfaceTint = colors.surfaceTint,
            surfaceVariant = colors.surfaceVariant,
            background = colors.background,
            onPrimary = colors.onPrimary,
            onPrimaryContainer = colors.onPrimaryContainer,
            onSecondary = colors.onSecondary,
            onSecondaryContainer = colors.onSecondaryContainer,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            onBackground = colors.onBackground,
            error = colors.error,
            onError = colors.onError,
            onErrorContainer = colors.onErrorContainer,
            errorContainer = colors.errorContainer,
            inverseOnSurface = colors.inverseOnSurface,
            inversePrimary = colors.inversePrimary,
            inverseSurface = colors.inverseSurface,
            onTertiary = colors.onTertiary,
            onTertiaryContainer = colors.onTertiaryContainer,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
            scrim = colors.scrim,
            tertiary = colors.tertiary,
            tertiaryContainer = colors.tertiaryContainer
        ),
        content = content
    )
}
