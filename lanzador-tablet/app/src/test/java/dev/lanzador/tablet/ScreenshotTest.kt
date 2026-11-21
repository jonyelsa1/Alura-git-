package dev.lanzador.tablet

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.ThemeMode
import dev.lanzador.tablet.data.WeatherNow
import dev.lanzador.tablet.data.WeatherPlace
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherScreen
import dev.lanzador.tablet.ui.PreviewActions
import dev.lanzador.tablet.ui.buildHomeState
import dev.lanzador.tablet.ui.theme.LauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.text.Collator
import java.util.Locale
import java.time.LocalDateTime

/**
 * Dibuja el launcher a 1920×1200 px (1280×800 dp, densidad 1,5) como la
 * pantalla de la T65Max y guarda PNG en docs/capturas.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "es-rES-w1280dp-h800dp-land-hdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val out = File("../docs/capturas").apply { mkdirs() }
    private val time = LocalDateTime.of(2026, 11, 21, 10, 5)

    @Test
    fun inicioHorizontalOscuro() = shot("inicio-horizontal-oscuro", sampleState(ThemeMode.DARK))

    @Test
    fun inicioHorizontalClaro() = shot("inicio-horizontal-claro", sampleState(ThemeMode.LIGHT), lightWallpaper = true)

    @Test
    fun cajonDeApps() = shot("cajon-de-apps", sampleState(ThemeMode.DARK), drawer = true)

    @Test
    fun ajustes() = shot("ajustes", sampleState(ThemeMode.DARK), settings = true)

    @Test
    @Config(qualifiers = "es-rES-w800dp-h1280dp-port-hdpi")
    fun inicioVertical() = shot("inicio-vertical", sampleState(ThemeMode.DARK))

    private fun shot(
        name: String,
        state: HomeState,
        drawer: Boolean = false,
        settings: Boolean = false,
        lightWallpaper: Boolean = false,
    ) {
        compose.setContent {
            LauncherTheme(state.settings.theme, dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(if (lightWallpaper) LightWallpaper else DarkWallpaper)) {
                    LauncherScreen(
                        state = state.copy(darkTextOnWallpaper = lightWallpaper),
                        actions = PreviewActions,
                        fixedTime = time,
                        initialDrawerOpen = drawer,
                        initialSettingsOpen = settings,
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    private fun sampleState(theme: ThemeMode): HomeState {
        val apps = APPS.mapIndexed { i, (label, color) -> fakeApp(i, label, color) }
            .sortedWith { a, b -> Collator.getInstance(Locale("es", "ES")).compare(a.label, b.label) }
        val key = { label: String -> apps.first { it.label == label }.key }
        return buildHomeState(
            apps = apps,
            favoriteKeys = listOf("YouTube", "Gmail", "Fotos", "Calendario", "Maps", "Keep", "Drive", "WhatsApp", "Cámara", "Reloj")
                .map(key),
            dockKeys = listOf("Chrome", "Claude", "JW Library", "Teléfono", "Play Store", "Ajustes").map(key),
            hiddenKeys = emptySet(),
            usage = mapOf(key("Claude") to 9, key("Chrome") to 7, key("JW Library") to 5, key("YouTube") to 3),
            settings = LauncherSettings(
                theme = theme,
                weatherPlace = WeatherPlace("Madrid", "Comunidad de Madrid, España", 40.4, -3.7),
                firstRunDone = true,
            ),
            note = "Comprar pan y leche\nLlamar al taller antes de las 13:00",
            weather = WeatherUi.Ready(WeatherNow("Madrid", 14, 2, true, 17, 6)),
            battery = BatteryState(82, false),
            nextAlarmMillis = null,
            darkTextOnWallpaper = false,
            homePresses = 0,
            loadingApps = false,
        )
    }

    private fun fakeApp(i: Int, label: String, color: Int): AppEntry = AppEntry(
        key = "demo.app$i/.Main#0",
        label = label,
        packageName = "demo.app$i",
        component = ComponentName("demo.app$i", "demo.app$i.Main"),
        user = Process.myUserHandle(),
        icon = letterIcon(label, color).asImageBitmap(),
        isSystem = false,
        isWorkProfile = false,
    )

    /** Icono de muestra: círculo de color con la inicial (no reproduce logotipos reales). */
    private fun letterIcon(label: String, color: Int): Bitmap {
        val size = 144
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size / 2f, size / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 64f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label.take(1).uppercase(), size / 2f, y, paint)
        return bitmap
    }

    private companion object {
        val DarkWallpaper = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF0E7490)))
        val LightWallpaper = Brush.linearGradient(listOf(Color(0xFFF5F3FF), Color(0xFFE0E7FF), Color(0xFFCFFAFE)))

        val APPS = listOf(
            "Ajustes" to 0xFF607D8B, "Archivos" to 0xFF1E88E5, "Calculadora" to 0xFF5E35B1,
            "Calendario" to 0xFF1976D2, "Cámara" to 0xFF424242, "Chrome" to 0xFF43A047,
            "Claude" to 0xFFD97757, "Contactos" to 0xFF3949AB, "Drive" to 0xFFFBC02D,
            "Duolingo" to 0xFF7CB342, "Fotos" to 0xFFE53935, "Gmail" to 0xFFD32F2F,
            "JW Library" to 0xFF5B3B8C, "Keep" to 0xFFF9A825, "Kindle" to 0xFF37474F,
            "Maps" to 0xFF00897B, "Mensajes" to 0xFF1565C0, "Música" to 0xFFEF6C00,
            "Notas" to 0xFF8D6E63, "Play Store" to 0xFF00ACC1, "Reloj" to 0xFF546E7A,
            "Spotify" to 0xFF2E7D32, "Teléfono" to 0xFF388E3C, "Temporizador" to 0xFFA1887F,
            "Traductor" to 0xFF1E88E5, "WhatsApp" to 0xFF2E7D32, "YouTube" to 0xFFC62828,
            "Zoom" to 0xFF1565C0, "Bienestar digital" to 0xFF00897B, "Galería" to 0xFFAD1457,
        ).map { (label, color) -> label to color.toInt() }
    }
}
