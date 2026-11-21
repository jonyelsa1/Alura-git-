package dev.lanzador.tablet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.lanzador.tablet.R
import dev.lanzador.tablet.data.LauncherStyle

// ---------- Tipografías (todas OFL, incluidas en res/font) ----------

private fun variable(res: Int, weights: IntProgression): FontFamily =
    FontFamily(weights.map { Font(res, FontWeight(it)) })

val Inter = variable(R.font.inter, 100..900 step 100)
val Outfit = variable(R.font.outfit, 100..900 step 100)
val SpaceGrotesk = variable(R.font.space_grotesk, 300..700 step 100)
val RobotoFlex = variable(R.font.roboto_flex, 100..1000 step 100)

@OptIn(ExperimentalTextApi::class)
val RobotoFlexWide = FontFamily(
    (500..1000 step 100).map { w ->
        Font(R.font.roboto_flex, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(125f)))
    },
)

@OptIn(ExperimentalTextApi::class)
val DotoRound = FontFamily(
    Font(R.font.doto, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", 100f))),
)

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

// ---------- Colores de cada estilo (menús, cajón y ajustes) ----------

private val CristalScheme = darkColorScheme(
    primary = Color(0xFFE4D7FF), onPrimary = Color(0xFF2A1460),
    primaryContainer = Color(0xFF4B2E99), onPrimaryContainer = Color(0xFFEDE4FF),
    secondary = Color(0xFF8AB4FF), secondaryContainer = Color(0xFF2B2A63), onSecondaryContainer = Color(0xFFE2E6FF),
    surface = Color(0xFF1B1238), onSurface = Color.White, onSurfaceVariant = Color(0xCCFFFFFF),
    surfaceContainerLowest = Color(0xFF120B28), surfaceContainerLow = Color(0xFF1B1238), surfaceContainer = Color(0xFF241A47),
    surfaceContainerHigh = Color(0xFF2E2356), surfaceContainerHighest = Color(0xFF392D66),
    outline = Color(0x66FFFFFF), outlineVariant = Color(0x33FFFFFF),
)

private val ExpresivoScheme = lightColorScheme(
    primary = Color(0xFF6F43E0), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF2A0E6B),
    secondary = Color(0xFF8A5A00), secondaryContainer = Color(0xFFFFE08A), onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF2E6B3C), tertiaryContainer = Color(0xFFC4EED0), onTertiaryContainer = Color(0xFF0B3D1D),
    surface = Color(0xFFFFF8F1), onSurface = Color(0xFF1D1B20), onSurfaceVariant = Color(0xFF4A4458),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF3EA), surfaceContainer = Color(0xFFF5ECE2),
    surfaceContainerHigh = Color(0xFFEFE5DA), surfaceContainerHighest = Color(0xFFE9DFD3),
    outline = Color(0xFF7A7287), outlineVariant = Color(0xFFD9CFC4),
)

private val BentoScheme = darkColorScheme(
    primary = Color(0xFFFFB547), onPrimary = Color(0xFF2B1600),
    primaryContainer = Color(0xFF4A3410), onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFB9A6FF), secondaryContainer = Color(0xFF332A55), onSecondaryContainer = Color(0xFFE9E3FB),
    surface = Color(0xFF15102A), onSurface = Color(0xFFF4F0FF), onSurfaceVariant = Color(0xFFA99FC4),
    surfaceContainerLowest = Color(0xFF0C0818), surfaceContainerLow = Color(0xFF15102A), surfaceContainer = Color(0xFF1E1734),
    surfaceContainerHigh = Color(0xFF282042), surfaceContainerHighest = Color(0xFF322950),
    outline = Color(0x33FFFFFF), outlineVariant = Color(0x1FFFFFFF),
)

private val EnfoqueScheme = darkColorScheme(
    primary = Color(0xFFE5302A), onPrimary = Color.White,
    primaryContainer = Color(0xFF3A0D0B), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFBDBDBD), secondaryContainer = Color(0xFF262626), onSecondaryContainer = Color(0xFFF2F2F2),
    surface = Color(0xFF0A0A0A), onSurface = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFF8B8B8B),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0E0E0E), surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C), surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFF3A3A3A), outlineVariant = Color(0xFF242424),
)

fun schemeFor(style: LauncherStyle): ColorScheme = when (style) {
    LauncherStyle.CRISTAL -> CristalScheme
    LauncherStyle.EXPRESIVO -> ExpresivoScheme
    LauncherStyle.BENTO -> BentoScheme
    LauncherStyle.ENFOQUE -> EnfoqueScheme
}

fun familyFor(style: LauncherStyle): FontFamily = when (style) {
    LauncherStyle.CRISTAL -> Inter
    LauncherStyle.EXPRESIVO -> RobotoFlex
    LauncherStyle.BENTO -> Inter
    LauncherStyle.ENFOQUE -> SpaceGrotesk
}

/** ¿Los iconos de la barra de estado deben ser oscuros en este estilo? */
fun lightBackground(style: LauncherStyle) = style == LauncherStyle.EXPRESIVO

@Composable
fun StyleTheme(style: LauncherStyle, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeFor(style), typography = Typography().withFamily(familyFor(style)), content = content)
}
