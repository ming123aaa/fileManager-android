package com.ohuang.filemanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightBlueColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D1B2A),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFF1565C0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1E4FF),
    onTertiaryContainer = Color(0xFF001D36),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0D1B2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1B2A),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF546E7A),
    inverseSurface = Color(0xFF2D3135),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA0C9FF),
    surfaceTint = Color(0xFF1E88E5),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF3F9),
    surfaceContainerHigh = Color(0xFFE8ECF2),
    surfaceContainerHighest = Color(0xFFE0E5EB),
    surfaceContainerLow = Color(0xFFF5F8FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF0F0F0),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFF757575),
    scrim = Color(0xFF000000),
)

val DarkBlueColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1E4976),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = Color(0xFF42A5F5),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF00497D),
    onTertiaryContainer = Color(0xFFD1E4FF),
    error = Color(0xFFEF5350),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A1929),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF132F4C),
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF90A4AE),
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2D3135),
    inversePrimary = Color(0xFF1E4976),
    surfaceTint = Color(0xFF64B5F6),
    surfaceBright = Color(0xFF1A3A5C),
    surfaceContainer = Color(0xFF1A3A5C),
    surfaceContainerHigh = Color(0xFF254567),
    surfaceContainerHighest = Color(0xFF305075),
    surfaceContainerLow = Color(0xFF132F4C),
    surfaceContainerLowest = Color(0xFF0D253B),
    surfaceDim = Color(0xFF0A1929),
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFFBDBDBD),
    scrim = Color(0xFF000000),
)

@Composable
fun FileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkBlueColorScheme
    } else {
        LightBlueColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
