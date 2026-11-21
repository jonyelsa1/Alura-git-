package dev.lanzador.tablet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.lanzador.tablet.data.ThemeMode

private val LightFallback = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2DFFF),
    onPrimaryContainer = Color(0xFF16105E),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCEFFA),
    onSecondaryContainer = Color(0xFF06313D),
    tertiary = Color(0xFFB45309),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FB),
    surfaceContainer = Color(0xFFEFEDF6),
    surfaceContainerHigh = Color(0xFFE9E7F0),
    surfaceContainerHighest = Color(0xFFE4E1EA),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFC3C0FF),
    onPrimary = Color(0xFF221C8F),
    primaryContainer = Color(0xFF3B33B5),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFF86D3EA),
    onSecondary = Color(0xFF003543),
    secondaryContainer = Color(0xFF164E5D),
    onSecondaryContainer = Color(0xFFCCEFFA),
    tertiary = Color(0xFFFFB77C),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1EA),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A292F),
    surfaceContainerHighest = Color(0xFF35343A),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
)

/** Material 3 con los colores del fondo de pantalla (Material You) si el sistema los ofrece. */
@Composable
fun LauncherTheme(
    mode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
