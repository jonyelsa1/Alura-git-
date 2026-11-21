package dev.lanzador.tablet.ui

import androidx.compose.runtime.Immutable
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.WeatherPlace
import dev.lanzador.tablet.data.WeatherUi

@Immutable
data class HomeState(
    /** Apps visibles (sin las ocultas), en orden alfabético. */
    val apps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val dock: List<AppEntry> = emptyList(),
    val frequent: List<AppEntry> = emptyList(),
    val hidden: List<AppEntry> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val dockKeys: Set<String> = emptySet(),
    val settings: LauncherSettings = LauncherSettings(),
    val note: String = "",
    val weather: WeatherUi = WeatherUi.NotSet,
    val battery: BatteryState? = null,
    val nextAlarmMillis: Long? = null,
    val darkTextOnWallpaper: Boolean = false,
    val homePresses: Int = 0,
    val loadingApps: Boolean = false,
)

fun buildHomeState(
    apps: List<AppEntry>,
    favoriteKeys: List<String>,
    dockKeys: List<String>,
    hiddenKeys: Set<String>,
    usage: Map<String, Int>,
    settings: LauncherSettings,
    note: String,
    weather: WeatherUi,
    battery: BatteryState?,
    nextAlarmMillis: Long?,
    darkTextOnWallpaper: Boolean,
    homePresses: Int,
    loadingApps: Boolean,
): HomeState {
    val byKey = apps.associateBy { it.key }
    val visible = apps.filter { it.key !in hiddenKeys }
    val frequent = usage.entries
        .filter { it.value >= 2 && it.key !in hiddenKeys }
        .sortedByDescending { it.value }
        .mapNotNull { byKey[it.key] }
        .take(6)
    return HomeState(
        apps = visible,
        favorites = favoriteKeys.mapNotNull { byKey[it] },
        dock = dockKeys.mapNotNull { byKey[it] },
        frequent = frequent,
        hidden = apps.filter { it.key in hiddenKeys },
        favoriteKeys = favoriteKeys.toSet(),
        dockKeys = dockKeys.toSet(),
        settings = settings,
        note = note,
        weather = weather,
        battery = battery,
        nextAlarmMillis = nextAlarmMillis,
        darkTextOnWallpaper = darkTextOnWallpaper,
        homePresses = homePresses,
        loadingApps = loadingApps,
    )
}

enum class QuickAction(val label: String) {
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    VOLUME("Sonido"),
    DISPLAY("Pantalla"),
    BATTERY("Batería"),
    SETTINGS("Ajustes"),
}

/** Acceso directo de una app; [handle] es el ShortcutInfo real en el dispositivo. */
data class ShortcutItem(val label: String, val handle: Any?)

/** Todo lo que la interfaz puede pedir. Los valores por defecto no hacen nada (útil en vistas previas). */
interface LauncherActions {
    fun launch(app: AppEntry) {}
    fun toggleFavorite(app: AppEntry) {}
    fun toggleDock(app: AppEntry) {}
    fun move(kind: AppListKind, app: AppEntry, delta: Int) {}
    fun setHidden(app: AppEntry, hidden: Boolean) {}
    fun openAppInfo(app: AppEntry) {}
    fun uninstall(app: AppEntry) {}
    fun shortcutsFor(app: AppEntry): List<ShortcutItem> = emptyList()
    fun startShortcut(item: ShortcutItem) {}
    fun webSearch(query: String) {}
    fun quick(action: QuickAction) {}
    fun expandNotifications() {}
    fun updateNote(text: String) {}
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {}
    suspend fun searchPlaces(query: String): List<WeatherPlace> = emptyList()
    fun refreshWeather() {}
    fun openWallpaperPicker() {}
    fun openDefaultLauncherSettings() {}
}

object PreviewActions : LauncherActions
