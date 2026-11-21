package dev.lanzador.tablet.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

enum class GlassTint { LIGHT, DARK }

/**
 * Superficie de cristal: recorta su trozo del fondo desenfocado (ver [Backdrop]),
 * le pone un velo y un reflejo en el borde. Sin fondo desenfocado (fondo del
 * sistema) usa un velo algo más opaco para que el texto se lea.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(34.dp),
    tint: GlassTint = GlassTint.LIGHT,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    rim: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalBackdrop.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val rimBrush = remember(tint) {
        if (tint == GlassTint.LIGHT) {
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.9f), 0.32f to Color.White.copy(alpha = 0.1f),
                0.58f to Color.White.copy(alpha = 0f), 1f to Color.White.copy(alpha = 0.5f),
            )
        } else {
            Brush.linearGradient(0f to Color.White.copy(alpha = 0.16f), 1f to Color.White.copy(alpha = 0.05f))
        }
    }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInWindow() }
            .clip(shape)
            .drawBehind {
                val img = backdrop.blurred
                if (img != null && backdrop.size.width > 0) {
                    val sx = img.width.toFloat() / backdrop.size.width
                    val sy = img.height.toFloat() / backdrop.size.height
                    val srcX = (origin.x * sx).roundToInt().coerceIn(0, img.width - 1)
                    val srcY = (origin.y * sy).roundToInt().coerceIn(0, img.height - 1)
                    val srcW = (size.width * sx).roundToInt().coerceIn(1, img.width - srcX)
                    val srcH = (size.height * sy).roundToInt().coerceIn(1, img.height - srcY)
                    drawImage(
                        img,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(srcW, srcH),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.High,
                    )
                } else {
                    drawRect(if (tint == GlassTint.LIGHT) Color(0x5C1C1033) else Color(0xB3140E24))
                }
                if (tint == GlassTint.LIGHT) {
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.22f), 0.42f to Color.White.copy(alpha = 0.07f),
                            1f to Color.White.copy(alpha = 0.12f),
                            start = Offset.Zero, end = Offset(size.width, size.height),
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.16f), 1f to Color.Transparent,
                            center = Offset(size.width * 0.12f, 0f), radius = size.maxDimension * 0.7f,
                        ),
                    )
                } else {
                    drawRect(
                        Brush.linearGradient(
                            0f to Color(0x9E281E42), 1f to Color(0x94140E24),
                            start = Offset.Zero, end = Offset(size.width * 0.4f, size.height),
                        ),
                    )
                    drawRect(Color.White.copy(alpha = 0.12f), size = Size(size.width, 1.dp.toPx()))
                }
            }
            .then(if (rim) Modifier.border(1.2.dp, rimBrush, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Forma polar: galleta, trébol, sol… según lóbulos y amplitud. */
class PolarShape(private val lobes: Int, private val amplitude: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(size.width, size.height) / 2f
        return Outline.Generic(polarPath(Offset(size.width / 2f, size.height / 2f), r, lobes, amplitude))
    }
}

/** Arco: muy redondo arriba y poco abajo. */
val ArchShape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 18, bottomEndPercent = 18)

/**
 * Dibuja el contenido en un lienzo de diseño (p. ej. 1280×744 dp) escalado para
 * que quepa en la pantalla real, sea cual sea su densidad.
 */
@Composable
fun DesignCanvas(
    designWidth: Dp,
    designHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        val scale = min(maxWidth / designWidth, maxHeight / designHeight).coerceIn(0.55f, 1.6f)
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * scale, density.fontScale.coerceAtMost(1.1f)),
        ) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}
