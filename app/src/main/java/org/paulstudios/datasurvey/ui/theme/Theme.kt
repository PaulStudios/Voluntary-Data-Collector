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
    primaryContainer = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF018786),
    surface = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFFECECEC),
    background = Color(0xFFF2F2F2),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF3700B3),
    onSecondary = Color(0xFF000000),
    onSecondaryContainer = Color(0xFF3700B3),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF000000),
    onBackground = Color(0xFF000000),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFFEBEE),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF3700B3),
    inverseSurface = Color(0xFF121212),
    onTertiary = Color(0xFFFFFFFF),
    onTertiaryContainer = Color(0xFF018786),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF212121),
    scrim = Color(0x99000000),
    tertiary = Color(0xFF03A9F4),
    tertiaryContainer = Color(0xFFB3E5FC)
)

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF018786),
    surface = Color(0xFF121212),
    surfaceTint = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    onPrimary = Color(0xFF3700B3),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFBB86FC),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    onErrorContainer = Color(0xFFB00020),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF6200EE),
    inverseSurface = Color(0xFFECECEC),
    onTertiary = Color(0xFF000000),
    onTertiaryContainer = Color(0xFFBB86FC),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFF757575),
    scrim = Color(0x99000000),
    tertiary = Color(0xFF03A9F4),
    tertiaryContainer = Color(0xFF018786)
)

@Composable
fun DataSurveyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        LightColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}