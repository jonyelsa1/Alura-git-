package dev.lanzador.tablet.ui.common

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.WallpaperMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Fondo ya dibujado y su versión desenfocada, del tamaño de la ventana. */
@Immutable
class Backdrop(val image: ImageBitmap?, val blurred: ImageBitmap?, val size: IntSize) {
    companion object {
        val None = Backdrop(null, null, IntSize.Zero)
    }
}

val LocalBackdrop = staticCompositionLocalOf { Backdrop.None }

/**
 * Prepara el fondo (el del estilo o la foto elegida) y una copia desenfocada.
 * Se hace una vez por cambio de estilo, foto o tamaño: los cristales solo
 * recortan su trozo de la copia, así que no hay desenfoque en cada fotograma.
 */
@Composable
fun rememberBackdrop(
    style: LauncherStyle,
    mode: WallpaperMode,
    photo: String?,
    size: IntSize,
    density: Float,
): Backdrop {
    val backdrop by produceState(Backdrop.None, style, mode, photo, size, density) {
        if (mode == WallpaperMode.SYSTEM || size.width <= 0 || size.height <= 0) {
            value = Backdrop.None
            return@produceState
        }
        value = withContext(Dispatchers.Default) { makeBackdrop(style, mode, photo, size, density) }
    }
    return backdrop
}

/** Versión bloqueante (para pruebas o fuera de la composición). */
fun makeBackdrop(style: LauncherStyle, mode: WallpaperMode, photo: String?, size: IntSize, density: Float): Backdrop {
    if (mode == WallpaperMode.SYSTEM || size.width <= 0 || size.height <= 0) return Backdrop.None
    val image = (if (mode == WallpaperMode.PHOTO && photo != null) decodePhoto(photo, size) else null)
        ?: renderStyleWallpaper(style, size, density)
    val saturation = if (style == LauncherStyle.ENFOQUE || style == LauncherStyle.EXPRESIVO) 1f else 1.7f
    return Backdrop(image.asImageBitmap(), blurredCopy(image, saturation).asImageBitmap(), size)
}

/** Dibuja el fondo: la imagen preparada o, mientras se prepara, el del estilo en vivo. */
@Composable
fun WallpaperLayer(style: LauncherStyle, mode: WallpaperMode, backdrop: Backdrop, modifier: Modifier = Modifier) {
    val image = backdrop.image
    when {
        mode == WallpaperMode.SYSTEM -> Unit
        image != null -> Image(image, contentDescription = null, modifier = modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        else -> Canvas(modifier.fillMaxSize()) { drawStyleWallpaper(style) }
    }
}

fun renderStyleWallpaper(style: LauncherStyle, size: IntSize, density: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
    CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, canvas, Size(size.width.toFloat(), size.height.toFloat())) {
        drawStyleWallpaper(style)
    }
    return bitmap
}

private fun decodePhoto(path: String, target: IntSize): Bitmap? = runCatching {
    val source = ImageDecoder.createSource(File(path))
    val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val scale = max(target.width.toFloat() / info.size.width, target.height.toFloat() / info.size.height)
        if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
    }
    // Recorte centrado al tamaño de la ventana + velo suave para leer el texto.
    val out = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(out)
    val scale = max(target.width.toFloat() / decoded.width, target.height.toFloat() / decoded.height)
    val w = decoded.width * scale
    val h = decoded.height * scale
    val left = (target.width - w) / 2f
    val top = (target.height - h) / 2f
    c.drawBitmap(decoded, null, android.graphics.RectF(left, top, left + w, top + h), Paint(Paint.FILTER_BITMAP_FLAG))
    val veil = Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, target.height.toFloat(),
            intArrayOf(0x55000000, 0x1A000000, 0x66000000), floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
    c.drawRect(0f, 0f, target.width.toFloat(), target.height.toFloat(), veil)
    out
}.getOrNull()

/** Reduce a 1/8 por pasos, satura y aplica tres pasadas de desenfoque de caja. */
private fun blurredCopy(src: Bitmap, saturation: Float): Bitmap {
    var current = src
    repeat(3) {
        val next = Bitmap.createScaledBitmap(current, max(1, current.width / 2), max(1, current.height / 2), true)
        if (current !== src) current.recycle()
        current = next
    }
    val out = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) }) }
    android.graphics.Canvas(out).drawBitmap(current, null, Rect(0, 0, out.width, out.height), paint)
    if (current !== src) current.recycle()
    boxBlur(out, radius = 3, passes = 3)
    return out
}

private fun boxBlur(bitmap: Bitmap, radius: Int, passes: Int) {
    val w = bitmap.width
    val h = bitmap.height
    val a = IntArray(w * h)
    val b = IntArray(w * h)
    bitmap.getPixels(a, 0, w, 0, 0, w, h)
    repeat(passes) {
        blurLine(a, b, w, h, radius, horizontal = true)
        blurLine(b, a, w, h, radius, horizontal = false)
    }
    bitmap.setPixels(a, 0, w, 0, 0, w, h)
}

private fun blurLine(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val div = 2 * r + 1
    for (line in 0 until lines) {
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        fun at(i: Int): Int {
            val k = i.coerceIn(0, len - 1)
            return if (horizontal) src[line * w + k] else src[k * w + line]
        }
        for (i in -r..r) {
            val p = at(i)
            sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (i in 0 until len) {
            val idx = if (horizontal) line * w + i else i * w + line
            dst[idx] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val out = at(i - r)
            val inn = at(i + r + 1)
            sa += (inn ushr 24) - (out ushr 24)
            sr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sg += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sb += (inn and 0xFF) - (out and 0xFF)
        }
    }
}

// ---------- Fondos de cada estilo, dibujados en proporción a la ventana ----------

fun DrawScope.drawStyleWallpaper(style: LauncherStyle) = when (style) {
    LauncherStyle.CRISTAL -> cristalWallpaper()
    LauncherStyle.EXPRESIVO -> expresivoWallpaper()
    LauncherStyle.BENTO -> bentoWallpaper()
    LauncherStyle.ENFOQUE -> enfoqueWallpaper()
}

private fun DrawScope.glow(color: Color, cx: Float, cy: Float, radius: Float, strength: Float = 1f) {
    val c = Offset(cx, cy)
    drawCircle(
        Brush.radialGradient(
            0f to color.copy(alpha = 0.95f * strength),
            0.45f to color.copy(alpha = 0.5f * strength),
            1f to color.copy(alpha = 0f),
            center = c, radius = radius,
        ),
        radius = radius, center = c,
    )
}

/** Esfera brillante: degradado radial con el punto de luz arriba a la izquierda. */
private fun DrawScope.orb(cx: Float, cy: Float, r: Float, vararg stops: Pair<Float, Color>) {
    val c = Offset(cx, cy)
    drawCircle(
        Brush.radialGradient(*stops, center = Offset(cx - 0.36f * r, cy - 0.46f * r), radius = 2.02f * r),
        radius = r, center = c,
    )
}

private fun DrawScope.cristalWallpaper() {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.radialGradient(
            0f to Color(0xFF40207F), 0.46f to Color(0xFF170B3D), 1f to Color(0xFF070519),
            center = Offset(w * 0.72f, h * 0.08f), radius = max(w, h) * 1.05f,
        ),
    )
    glow(Color(0xFFFF3D8B), w * 0.86f, h * 0.15f, w * 0.27f)
    glow(Color(0xFF2F6BFF), w * 0.09f, h * 0.76f, w * 0.28f)
    glow(Color(0xFFFF8A3D), w * 0.57f, h * 1.02f, w * 0.25f)
    // Anillo de colores
    val ringCenter = Offset(w * 0.5f, h * 0.66f)
    val ringW = w * 0.172f
    val ringH = w * 0.141f
    rotate(-18f, ringCenter) {
        drawOval(
            Brush.sweepGradient(
                listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF22D3EE), Color(0xFF7C3AED)),
                center = ringCenter,
            ),
            topLeft = Offset(ringCenter.x - ringW, ringCenter.y - ringH),
            size = Size(ringW * 2, ringH * 2),
            style = Stroke(width = w * 0.02f),
        )
    }
    orb(w * 0.531f, h * 0.2225f, w * 0.047f, 0f to Color.White, 0.07f to Color(0xFFFFF4C7), 0.28f to Color(0xFFFFC93D), 0.62f to Color(0xFFF07A12), 0.95f to Color(0xFF6B2205))
    orb(w * 0.855f, h * 0.55f, w * 0.148f, 0f to Color.White, 0.05f to Color(0xFFFFD6EE), 0.2f to Color(0xFFFF5AA8), 0.5f to Color(0xFFD01C6B), 0.86f to Color(0xFF4A0B45))
    orb(w * 0.191f, h * 0.844f, w * 0.105f, 0f to Color.White, 0.05f to Color(0xFFC8F7FF), 0.24f to Color(0xFF2DD4F0), 0.6f to Color(0xFF1F4FD8), 0.92f to Color(0xFF0A1142))
}

private fun DrawScope.expresivoWallpaper() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFFF7EFE7))
    drawPath(polarPath(Offset(w * 0.89f, h * 0.1f), w * 0.242f, 8, 0.09f), Color(0xFFFFE0CF))
    drawCircle(Color(0xFFC9E8CF), radius = w * 0.203f, center = Offset(w * 0.0625f, h * 1.05f))
    val pillCenter = Offset(w * 0.719f, h * 0.9125f)
    rotate(38f, pillCenter) {
        val pw = w * 0.14f
        val ph = w * 0.406f
        drawRoundRect(
            Color(0xFFE7DCFF),
            topLeft = Offset(pillCenter.x - pw / 2, pillCenter.y - ph / 2),
            size = Size(pw, ph),
            cornerRadius = CornerRadius(pw / 2),
        )
    }
}

private fun DrawScope.bentoWallpaper() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF0C0818))
    drawRect(Brush.radialGradient(0f to Color(0xFF3A1F78), 1f to Color.Transparent, center = Offset(w * 0.12f, 0f), radius = w * 0.7f))
    drawRect(Brush.radialGradient(0f to Color(0xFF5A2A14), 1f to Color.Transparent, center = Offset(w, h), radius = w * 0.6f))
    drawRect(Brush.radialGradient(0f to Color(0xFF1F2A6B), 1f to Color.Transparent, center = Offset(w * 0.7f, h * 0.3f), radius = w * 0.5f))
    glow(Color(0xFF6D3DF5), w * 0.273f, h * 0.3375f, w * 0.22f, 0.55f)
    glow(Color(0xFFFF8A2B), w * 0.78f, h * 0.76f, w * 0.19f, 0.42f)
    glow(Color(0xFF2F8CFF), w * 0.88f, h * 0.21f, w * 0.16f, 0.4f)
}

private fun DrawScope.enfoqueWallpaper() {
    val w = size.width
    val h = size.height
    drawRect(Color.Black)
    val step = 18.dp.toPx()
    val dot = 1.dp.toPx()
    val color = Color.White.copy(alpha = 0.075f)
    var y = step / 2
    while (y < h) {
        var x = step / 2
        while (x < w) {
            drawCircle(color, dot, Offset(x, y))
            x += step
        }
        y += step
    }
    drawRect(Brush.radialGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f), center = Offset(w * 0.3f, h * 0.4f), radius = max(w, h) * 0.85f))
}

/** r(θ) = R·(1 + a·cos(nθ)): formas onduladas de Material 3 Expressive. */
fun polarPath(center: Offset, radius: Float, lobes: Int, amplitude: Float): Path {
    val path = Path()
    val base = radius / (1 + amplitude)
    val steps = 360
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val r = base * (1 + amplitude * cos(lobes * t))
        val x = center.x + r * cos(t)
        val y = center.y + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
