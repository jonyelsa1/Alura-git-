package dev.lanzador.tablet.data

import android.content.ComponentName
import android.content.Intent
import android.os.BatteryManager
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** Una actividad lanzable (una "app" del cajón). */
@Immutable
data class AppEntry(
    /** Identificador estable: componente + número de serie del perfil. */
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    val isSystem: Boolean,
    val isWorkProfile: Boolean,
)

enum class IconSize(val dp: Int, val label: String) {
    SMALL(60, "Pequeño"),
    MEDIUM(72, "Mediano"),
    LARGE(88, "Grande"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
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
    val iconSize: IconSize = IconSize.MEDIUM,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val showWeather: Boolean = true,
    val showNote: Boolean = true,
    val showQuickSettings: Boolean = true,
    val weatherPlace: WeatherPlace? = null,
    val firstRunDone: Boolean = false,
)

@Immutable
data class WeatherNow(
    val place: String,
    val temperature: Int,
    val code: Int,
    val isDay: Boolean,
    val max: Int,
    val min: Int,
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
