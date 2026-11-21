package dev.lanzador.tablet

import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.CalendarEvent
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.MediaInfo
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.LauncherScreen
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.common.Backdrop
import dev.lanzador.tablet.ui.common.LocalFixedTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/** Registra lo que la interfaz pide, sin tocar Android. */
private class Recorder : LauncherActions {
    val calls = mutableListOf<String>()
    var styleSet: LauncherStyle? = null
    val calendarOpened get() = calls.count { it == "openCalendar" }
    override fun openCalendar(event: CalendarEvent?) { calls += "openCalendar" }
    override fun setStyle(style: LauncherStyle) { calls += "setStyle"; styleSet = style }
    override fun request(permission: Permission) { calls += "request:$permission" }
    override fun openClock() { calls += "openClock" }
    override fun launch(app: AppEntry) { calls += "launch" }
    override fun quick(action: QuickAction) { calls += "quick:$action" }
    override fun openMediaApp(media: MediaInfo) { calls += "openMediaApp" }
    override fun openMusicApp() { calls += "openMusicApp" }
    override fun refreshWeather() { calls += "refreshWeather" }
    override fun webSearch(query: String) { calls += "webSearch" }
    override fun voiceSearch() { calls += "voiceSearch" }
    override fun mediaPlayPause() { calls += "mediaPlayPause" }
    override fun timerStart() { calls += "timerStart" }
    override fun openPhoto(photos: PhotoUi) { calls += "openPhoto" }
    override fun expandNotifications() { calls += "expandNotifications" }
    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) { calls += "updateSettings" }
}

/**
 * Comprueba los dos fallos vistos en la tablet:
 * 1. Mantener pulsado un widget abría el widget al soltar en vez del selector de estilo.
 * 2. Las apps abiertas desde el launcher quedaban encima de él al pulsar Inicio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-rES-w1280dp-h800dp-land-hdpi")
class BehaviorTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val event = CalendarEvent(7, "Reunión de prueba", now + 2 * 3_600_000L, now + 3 * 3_600_000L, false, 0xFF8AB4FF.toInt())

    private fun state(style: LauncherStyle) = HomeState(
        style = style,
        settings = LauncherSettings(style = style, firstRunDone = true),
        calendar = CalendarUi.Ready(listOf(event), null),
    )

    private fun show(style: LauncherStyle, actions: LauncherActions) {
        compose.setContent {
            CompositionLocalProvider(LocalFixedTime provides LocalDateTime.of(2026, 11, 21, 10, 5)) {
                LauncherScreen(state(style), actions, backdropOverride = Backdrop.None)
            }
        }
    }

    @Test
    fun enfoque_mantenerPulsadoElEvento_abreSelectorYNoElCalendario() {
        val rec = Recorder()
        show(LauncherStyle.ENFOQUE, rec)
        compose.onNodeWithText("REUNIÓN DE PRUEBA", substring = true).performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Estilo de hoy").assertIsDisplayed()
        assertEquals("No debe abrir el calendario al soltar", 0, rec.calendarOpened)
    }

    @Test
    fun enfoque_tocarElEvento_abreElCalendario() {
        val rec = Recorder()
        show(LauncherStyle.ENFOQUE, rec)
        compose.onNodeWithText("REUNIÓN DE PRUEBA", substring = true).performClick()
        compose.waitForIdle()
        assertEquals(1, rec.calendarOpened)
        assertEquals(0, compose.onAllNodesWithContentDescription("Elegido").fetchSemanticsNodes().size)
    }

    @Test
    fun cristal_mantenerPulsadaLaAgenda_abreSelector() {
        val rec = Recorder()
        show(LauncherStyle.CRISTAL, rec)
        compose.onNodeWithText("Reunión de prueba").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Estilo de hoy").assertIsDisplayed()
        assertEquals(0, rec.calendarOpened)
    }

    @Test
    fun botonPaleta_abreSelector_yElegirEstiloLoCambia() {
        val rec = Recorder()
        var style by mutableStateOf(LauncherStyle.CRISTAL)
        compose.setContent {
            CompositionLocalProvider(LocalFixedTime provides LocalDateTime.of(2026, 11, 21, 10, 5)) {
                LauncherScreen(state(style), rec, backdropOverride = Backdrop.None)
            }
        }
        for (s in LauncherStyle.entries) {
            style = s
            rec.styleSet = null
            compose.waitForIdle()
            compose.onAllNodesWithContentDescription("Cambiar estilo").onFirst().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Estilo de hoy").assertIsDisplayed()
            compose.onNodeWithText("Bento").performClick()
            compose.waitForIdle()
            assertEquals("Desde $s", LauncherStyle.BENTO, rec.styleSet)
        }
    }

    /** Ningún estilo debe abrir nada solo por dibujarse (el fallo de Enfoque abría el calendario). */
    @Test
    fun dibujarLosEstilos_noAbreNadaSinTocar() {
        val rec = Recorder()
        var current by mutableStateOf(state(LauncherStyle.CRISTAL))
        compose.setContent {
            CompositionLocalProvider(LocalFixedTime provides LocalDateTime.of(2026, 11, 21, 10, 5)) {
                LauncherScreen(current, rec, backdropOverride = Backdrop.None)
            }
        }
        val calendars = listOf(CalendarUi.Ready(listOf(event), null), CalendarUi.NoPermission, CalendarUi.Ready(emptyList(), null))
        for (style in LauncherStyle.entries) for (cal in calendars) for (access in listOf(false, true)) for (supported in listOf(true, false)) {
            current = state(style).copy(calendar = cal, notificationAccess = access, mediaSupported = supported, nextAlarmMillis = now + 3_600_000L)
            compose.waitForIdle()
            current = current.copy(battery = BatteryState(50, true))
            compose.waitForIdle()
        }
        assertEquals("Acciones lanzadas sin tocar nada", emptyList<String>(), rec.calls)
    }

    @Test
    fun lasAppsQueAbreElLauncher_vanEnSuPropiaTarea() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val vm = ViewModelProvider(activity)[LauncherViewModel::class.java]
        val actions = DeviceActions(activity, vm)
        val shadow = shadowOf(activity)

        val acciones = listOf<Pair<String, () -> Unit>>(
            "calendario (evento)" to { actions.openCalendar(event) },
            "calendario (hoy)" to { actions.openCalendar(null) },
            "reloj" to { actions.openClock() },
            "Wi-Fi" to { actions.quick(QuickAction.WIFI) },
            "ajustes" to { actions.quick(QuickAction.SETTINGS) },
            "búsqueda web" to { actions.webSearch("tiempo") },
            "música" to { actions.openMusicApp() },
        )
        for ((nombre, accion) in acciones) {
            accion()
            val started: Intent? = shadow.nextStartedActivity
            assertNotNull("No se abrió nada para $nombre", started)
            assertTrue("$nombre debe abrirse con FLAG_ACTIVITY_NEW_TASK", started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        }
    }
}
