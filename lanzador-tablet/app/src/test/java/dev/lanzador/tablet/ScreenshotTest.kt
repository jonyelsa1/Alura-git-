package dev.lanzador.tablet

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntSize
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.CalendarEvent
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.DeviceToggles
import dev.lanzador.tablet.data.HourForecast
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.MediaInfo
import dev.lanzador.tablet.data.NotifSummary
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.data.TaskItem
import dev.lanzador.tablet.data.TimerState
import dev.lanzador.tablet.data.WallpaperMode
import dev.lanzador.tablet.data.WeatherNow
import dev.lanzador.tablet.data.WeatherPlace
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherScreen
import dev.lanzador.tablet.ui.Overlay
import dev.lanzador.tablet.ui.PreviewActions
import dev.lanzador.tablet.ui.buildHomeState
import dev.lanzador.tablet.ui.common.LocalFixedTime
import dev.lanzador.tablet.ui.common.makeBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.text.Collator
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Dibuja el launcher a 1920×1200 px (1280×800 dp, densidad 1,5) como la
 * pantalla de la T65Max y guarda PNG en docs/capturas. Datos de muestra.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "es-rES-w1280dp-h800dp-land-hdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val out = File("../docs/capturas").apply { mkdirs() }
    private val time = LocalDateTime.of(2026, 11, 21, 10, 5)
    private val zone = ZoneId.systemDefault()
    private fun at(h: Int, m: Int, plusDays: Long = 0) = time.toLocalDate().plusDays(plusDays).atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    @Test fun cristal() = shot("cristal", LauncherStyle.CRISTAL)
    @Test fun cristalCajon() = shot("cristal-cajon", LauncherStyle.CRISTAL, Overlay.DRAWER)
    @Test fun expresivo() = shot("expresivo", LauncherStyle.EXPRESIVO)
    @Test fun expresivoCajon() = shot("expresivo-cajon", LauncherStyle.EXPRESIVO, Overlay.DRAWER)
    @Test fun bento() = shot("bento", LauncherStyle.BENTO)
    @Test fun bentoCajon() = shot("bento-cajon", LauncherStyle.BENTO, Overlay.DRAWER)
    @Test fun enfoque() = shot("enfoque", LauncherStyle.ENFOQUE)
    @Test fun enfoqueCajon() = shot("enfoque-cajon", LauncherStyle.ENFOQUE, Overlay.DRAWER)
    @Test fun selectorDeEstilo() = shot("selector-estilo", LauncherStyle.CRISTAL, Overlay.STYLES)
    @Test fun ajustes() = shot("ajustes", LauncherStyle.BENTO, Overlay.SETTINGS)

    @Test @Config(qualifiers = "es-rES-w800dp-h1280dp-port-hdpi")
    fun cristalVertical() = shot("cristal-vertical", LauncherStyle.CRISTAL, w = 1200, h = 1920)

    @Test @Config(qualifiers = "es-rES-w800dp-h1280dp-port-hdpi")
    fun expresivoVertical() = shot("expresivo-vertical", LauncherStyle.EXPRESIVO, w = 1200, h = 1920)

    private fun shot(name: String, style: LauncherStyle, overlay: Overlay = Overlay.NONE, w: Int = 1920, h: Int = 1200) {
        val state = sampleState(style)
        val backdrop = makeBackdrop(style, WallpaperMode.STYLE, null, IntSize(w, h), 1.5f)
        compose.setContent {
            CompositionLocalProvider(LocalFixedTime provides time) {
                LauncherScreen(state, PreviewActions, initialOverlay = overlay, backdropOverride = backdrop)
            }
        }
        compose.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    private fun sampleState(style: LauncherStyle): HomeState {
        val collator = Collator.getInstance(Locale("es", "ES"))
        val apps = APPS.mapIndexed { i, (label, color, cat) -> fakeApp(i, label, color, cat) }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        val key = { label: String -> apps.first { it.label == label }.key }
        val pkg = { label: String -> apps.first { it.label == label }.packageName }
        return buildHomeState(
            apps = apps,
            favoriteKeys = listOf("Teléfono", "Chrome", "Claude", "JW Library", "Temporizador", "Fotos", "YouTube", "WhatsApp", "Ajustes").map(key),
            dockKeys = listOf("WhatsApp", "Gmail", "Calendario", "Cámara", "Notas", "Música").map(key),
            hiddenKeys = emptySet(),
            usage = mapOf(key("Claude") to 9, key("Chrome") to 7, key("JW Library") to 5, key("YouTube") to 3, key("Temporizador") to 3, key("WhatsApp") to 2),
            settings = LauncherSettings(style = style, weatherPlace = WeatherPlace("Madrid", "Comunidad de Madrid, España", 40.4, -3.7), firstRunDone = true),
            style = style,
            weather = WeatherUi.Ready(
                WeatherNow(
                    "Madrid", 14, 2, true, 17, 6,
                    listOf(10 to 14, 11 to 15, 12 to 16, 13 to 17, 14 to 17, 15 to 16, 16 to 14, 17 to 12, 18 to 11).mapIndexed { i, (hr, t) ->
                        HourForecast(hr, t, listOf(2, 0, 0, 2, 2, 3, 51, 61, 3)[i], true)
                    },
                ),
            ),
            battery = BatteryState(82, false),
            nextAlarmMillis = at(7, 0, 1),
            calendar = CalendarUi.Ready(
                listOf(
                    CalendarEvent(1, "Llamar al taller", at(12, 30), at(13, 0), false, 0xFFFFD166.toInt()),
                    CalendarEvent(2, "Compra semanal", at(17, 0), at(18, 0), false, 0xFF7CFFB2.toInt()),
                    CalendarEvent(3, "Reunión", at(19, 30), at(21, 0), false, 0xFF8AB4FF.toInt()),
                ),
                CalendarEvent(4, "Desayuno con Marta", at(10, 0, 1), at(11, 0, 1), false, 0xFFFF8A80.toInt()),
            ),
            media = MediaInfo(pkg("Música"), "Clair de lune", "Claude Debussy", albumArt().asImageBitmap(), true, 302_000, 134_000, SystemClock.elapsedRealtime(), 0f),
            notifications = mapOf(pkg("WhatsApp") to NotifSummary(pkg("WhatsApp"), 2, "Marta", "¿Quedamos mañana a las 10?"), pkg("Gmail") to NotifSummary(pkg("Gmail"), 3, "Banco", "Tu extracto de noviembre")),
            notificationAccess = true,
            photos = PhotoUi.Ready("Hace un año", "21 de noviembre de 2025 · 14 fotos", landscapePhoto().asImageBitmap(), Uri.EMPTY),
            device = DeviceToggles(wifiOn = true, bluetoothOn = true, dndOn = true, torchOn = false, torchAvailable = true, autoRotate = false, storageUsedPercent = 64),
            tasks = listOf(
                TaskItem(1, "Llamar al taller", true),
                TaskItem(2, "Comprar pan y leche", false),
                TaskItem(3, "Preparar la reunión", false),
                TaskItem(4, "Regar las plantas", false),
            ),
            timer = TimerState(25 * 60_000L, 0, 15 * 60_000L + 31_000L),
            darkTextOnWallpaper = false,
            homePresses = 0,
            loadingApps = false,
        )
    }

    private fun fakeApp(i: Int, label: String, color: Int, category: Int): AppEntry {
        val withGlyph = i % 3 != 2
        return AppEntry(
            key = "demo.app$i/.Main#0",
            label = label,
            packageName = "demo.app$i",
            component = ComponentName("demo.app$i", "demo.app$i.Main"),
            user = Process.myUserHandle(),
            icon = letterIcon(label, color, background = true).asImageBitmap(),
            glyph = if (withGlyph) letterIcon(label, android.graphics.Color.WHITE, background = false).asImageBitmap() else null,
            foreground = if (!withGlyph) letterIcon(label, color, background = false, ring = true).asImageBitmap() else null,
            tint = color,
            category = category,
            isSystem = false,
            isWorkProfile = false,
        )
    }

    /** Icono de muestra: inicial sobre círculo de color o sola (como capa monocroma). No reproduce logotipos reales. */
    private fun letterIcon(label: String, color: Int, background: Boolean, ring: Boolean = false): Bitmap {
        val size = 144
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (background) {
            paint.color = color
            canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size / 2f, size / 2f, paint)
        }
        if (ring) {
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f
            canvas.drawCircle(size / 2f, size / 2f, size * 0.3f, paint)
            paint.style = Paint.Style.FILL
        }
        paint.color = if (background) android.graphics.Color.WHITE else color
        paint.textSize = if (background) 64f else 54f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label.take(1).uppercase(), size / 2f, y, paint)
        return bitmap
    }

    private fun albumArt(): Bitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 256f, 256f, intArrayOf(0xFF1E3A8A.toInt(), 0xFF6D28D9.toInt(), 0xFFF0ABFC.toInt()), null, Shader.TileMode.CLAMP) }
        Canvas(b).apply {
            drawRect(0f, 0f, 256f, 256f, p)
            drawCircle(180f, 80f, 34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF7D6.toInt() })
        }
        return b
    }

    private fun landscapePhoto(): Bitmap {
        val w = 720
        val h = 400
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF3B82F6.toInt(), 0xFFF9A8D4.toInt(), 0xFFFDE68A.toInt()), null, Shader.TileMode.CLAMP) })
        c.drawCircle(w * 0.72f, h * 0.55f, 46f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF1C1.toInt() })
        val hill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF14532D.toInt() }
        c.drawOval(RectF(-120f, h * 0.62f, w * 0.7f, h * 1.6f), hill)
        hill.color = 0xFF166534.toInt()
        c.drawOval(RectF(w * 0.35f, h * 0.7f, w * 1.3f, h * 1.7f), hill)
        return b
    }

    private companion object {
        val APPS = listOf(
            Triple("Ajustes", 0xFF607D8B, -1), Triple("Archivos", 0xFF1E88E5, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Bienestar digital", 0xFF00897B, -1), Triple("Calculadora", 0xFF5E35B1, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Calendario", 0xFF1976D2, ApplicationInfo.CATEGORY_PRODUCTIVITY), Triple("Cámara", 0xFF546E7A, ApplicationInfo.CATEGORY_IMAGE),
            Triple("Chrome", 0xFF43A047, -1), Triple("Claude", 0xFFD97757, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Contactos", 0xFF3949AB, ApplicationInfo.CATEGORY_SOCIAL), Triple("Drive", 0xFFF9A825, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Duolingo", 0xFF7CB342, -1), Triple("Fotos", 0xFFE53935, ApplicationInfo.CATEGORY_IMAGE),
            Triple("Gmail", 0xFFD32F2F, ApplicationInfo.CATEGORY_SOCIAL), Triple("JW Library", 0xFF6B4FA0, ApplicationInfo.CATEGORY_NEWS),
            Triple("Keep", 0xFFF9A825, ApplicationInfo.CATEGORY_PRODUCTIVITY), Triple("Kindle", 0xFF455A64, ApplicationInfo.CATEGORY_NEWS),
            Triple("Mapas", 0xFF00897B, ApplicationInfo.CATEGORY_MAPS), Triple("Mensajes", 0xFF1565C0, ApplicationInfo.CATEGORY_SOCIAL),
            Triple("Música", 0xFFEF6C00, ApplicationInfo.CATEGORY_AUDIO), Triple("Notas", 0xFF8D6E63, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Play Store", 0xFF00ACC1, -1), Triple("Radio", 0xFFAD1457, ApplicationInfo.CATEGORY_AUDIO),
            Triple("Reloj", 0xFF607D8B, -1), Triple("Spotify", 0xFF2E7D32, ApplicationInfo.CATEGORY_AUDIO),
            Triple("Teléfono", 0xFF43A047, ApplicationInfo.CATEGORY_SOCIAL), Triple("Temporizador", 0xFFC99A5B, ApplicationInfo.CATEGORY_PRODUCTIVITY),
            Triple("Traductor", 0xFF1E88E5, ApplicationInfo.CATEGORY_PRODUCTIVITY), Triple("WhatsApp", 0xFF2E7D32, ApplicationInfo.CATEGORY_SOCIAL),
            Triple("YouTube", 0xFFC62828, ApplicationInfo.CATEGORY_VIDEO), Triple("Zoom", 0xFF1565C0, ApplicationInfo.CATEGORY_SOCIAL),
        ).map { (l, c, cat) -> Triple(l, c.toInt(), cat) }
    }
}
