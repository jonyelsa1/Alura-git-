package dev.lanzador.tablet.data

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import java.time.LocalDate

/** Una actividad lanzable (una "app" del cajón). */
@Immutable
data class AppEntry(
    /** Identificador estable: componente + número de serie del perfil. */
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    /** Icono completo, a color. */
    val icon: ImageBitmap,
    /** Capa monocroma del icono adaptativo (Android 13+), en blanco sobre transparente. */
    val glyph: ImageBitmap?,
    /** Capa delantera del icono adaptativo, a color y sin el fondo. */
    val foreground: ImageBitmap?,
    /** Color dominante del icono, para tintar el cristal. */
    val tint: Int,
    /** ApplicationInfo.category */
    val category: Int,
    val isSystem: Boolean,
    val isWorkProfile: Boolean,
)

enum class LauncherStyle(val label: String, val description: String) {
    CRISTAL("Cristal", "Widgets transparentes sobre un fondo vivo"),
    EXPRESIVO("Expresivo", "Formas onduladas y colores pastel"),
    BENTO("Bento", "Bloques de cristal oscuro con acento ámbar"),
    ENFOQUE("Enfoque", "Negro, reloj de puntos y apps en lista"),
}

enum class WallpaperMode(val label: String) {
    STYLE("Del estilo"),
    PHOTO("Mi foto"),
    SYSTEM("Del sistema"),
}

@Immutable
data class WeatherPlace(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun encode(): String = listOf(name, detail, latitude, longitude).joinToString("\t")

    companion object {
        fun decode(raw: String?): WeatherPlace? {
            val parts = raw?.split('\t') ?: return null
            if (parts.size != 4) return null
            val lat = parts[2].toDoubleOrNull() ?: return null
            val lon = parts[3].toDoubleOrNull() ?: return null
            return WeatherPlace(parts[0], parts[1], lat, lon)
        }
    }
}

@Immutable
data class LauncherSettings(
    val style: LauncherStyle = LauncherStyle.CRISTAL,
    /** Un estilo distinto cada día, en orden. */
    val dailyRotation: Boolean = false,
    val wallpaperMode: WallpaperMode = WallpaperMode.STYLE,
    /** Ruta de la foto elegida como fondo (copiada dentro de la app). */
    val wallpaperPhoto: String? = null,
    val weatherPlace: WeatherPlace? = null,
    val timerMinutes: Int = 25,
    val firstRunDone: Boolean = false,
) {
    fun effectiveStyle(today: LocalDate = LocalDate.now()): LauncherStyle =
        if (dailyRotation) LauncherStyle.entries[today.dayOfYear % LauncherStyle.entries.size] else style
}

@Immutable
data class HourForecast(val hour: Int, val temperature: Int, val code: Int, val isDay: Boolean)

@Immutable
data class WeatherNow(
    val place: String,
    val temperature: Int,
    val code: Int,
    val isDay: Boolean,
    val max: Int,
    val min: Int,
    val hourly: List<HourForecast> = emptyList(),
)

sealed interface WeatherUi {
    data object NotSet : WeatherUi
    data class Loading(val place: String) : WeatherUi
    data class Ready(val data: WeatherNow) : WeatherUi
    data class Error(val place: String) : WeatherUi
}

@Immutable
data class BatteryState(val level: Int, val charging: Boolean) {
    companion object {
        fun from(intent: Intent): BatteryState? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level < 0 || scale <= 0) return null
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            return BatteryState(level * 100 / scale, charging)
        }
    }
}

@Immutable
data class CalendarEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
)

sealed interface CalendarUi {
    data object NoPermission : CalendarUi
    data class Ready(val today: List<CalendarEvent>, val tomorrow: CalendarEvent?) : CalendarUi
}

@Immutable
data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    /** SystemClock.elapsedRealtime() del momento en que se midió [positionMs]. */
    val positionAt: Long,
    val speed: Float,
)

@Immutable
data class NotifSummary(val packageName: String, val count: Int, val title: String?, val text: String?)

sealed interface PhotoUi {
    data object NoPermission : PhotoUi
    data object Empty : PhotoUi
    data class Ready(val title: String, val subtitle: String, val cover: ImageBitmap, val uri: Uri) : PhotoUi
}

@Immutable
data class DeviceToggles(
    val wifiOn: Boolean = false,
    val bluetoothOn: Boolean = false,
    val dndOn: Boolean = false,
    val torchOn: Boolean = false,
    val torchAvailable: Boolean = false,
    val autoRotate: Boolean = false,
    val storageUsedPercent: Int = 0,
)

@Immutable
data class TaskItem(val id: Long, val text: String, val done: Boolean)

/** Temporizador de enfoque. Si [endAt] > 0 está en marcha; si no, quedan [remainingMs]. */
@Immutable
data class TimerState(val durationMs: Long, val endAt: Long, val remainingMs: Long) {
    val running: Boolean get() = endAt > 0
    fun remainingAt(now: Long): Long = if (running) (endAt - now).coerceAtLeast(0) else remainingMs
}
