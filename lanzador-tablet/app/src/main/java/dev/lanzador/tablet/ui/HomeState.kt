package dev.lanzador.tablet.ui

import androidx.compose.runtime.Immutable
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.CalendarEvent
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.DeviceToggles
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.MediaInfo
import dev.lanzador.tablet.data.NotifSummary
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.data.TaskItem
import dev.lanzador.tablet.data.TimerState
import dev.lanzador.tablet.data.WallpaperMode
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
    val style: LauncherStyle = LauncherStyle.CRISTAL,
    val weather: WeatherUi = WeatherUi.NotSet,
    val battery: BatteryState? = null,
    val nextAlarmMillis: Long? = null,
    val calendar: CalendarUi = CalendarUi.NoPermission,
    val media: MediaInfo? = null,
    val notifications: Map<String, NotifSummary> = emptyMap(),
    val notificationAccess: Boolean = false,
    /** false en la versión ligera, que no incluye el lector de notificaciones. */
    val mediaSupported: Boolean = true,
    val photos: PhotoUi = PhotoUi.NoPermission,
    val device: DeviceToggles = DeviceToggles(),
    val tasks: List<TaskItem> = emptyList(),
    val timer: TimerState = TimerState(25 * 60_000L, 0, 25 * 60_000L),
    val darkTextOnWallpaper: Boolean = false,
    val homePresses: Int = 0,
    val loadingApps: Boolean = false,
) {
    val todayEvents: List<CalendarEvent> get() = (calendar as? CalendarUi.Ready)?.today.orEmpty()
    val tomorrowEvent: CalendarEvent? get() = (calendar as? CalendarUi.Ready)?.tomorrow
    val nextEvent: CalendarEvent?
        get() = todayEvents.firstOrNull { !it.allDay && it.begin > System.currentTimeMillis() } ?: todayEvents.firstOrNull { !it.allDay }
}

fun buildHomeState(
    apps: List<AppEntry>,
    favoriteKeys: List<String>,
    dockKeys: List<String>,
    hiddenKeys: Set<String>,
    usage: Map<String, Int>,
    settings: LauncherSettings,
    style: LauncherStyle,
    weather: WeatherUi,
    battery: BatteryState?,
    nextAlarmMillis: Long?,
    calendar: CalendarUi,
    media: MediaInfo?,
    notifications: Map<String, NotifSummary>,
    notificationAccess: Boolean,
    photos: PhotoUi,
    device: DeviceToggles,
    tasks: List<TaskItem>,
    timer: TimerState,
    darkTextOnWallpaper: Boolean,
    homePresses: Int,
    loadingApps: Boolean,
    mediaSupported: Boolean = true,
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
        style = style,
        weather = weather,
        battery = battery,
        nextAlarmMillis = nextAlarmMillis,
        calendar = calendar,
        media = media,
        notifications = notifications,
        notificationAccess = notificationAccess,
        photos = photos,
        device = device,
        tasks = tasks,
        timer = timer,
        darkTextOnWallpaper = darkTextOnWallpaper,
        homePresses = homePresses,
        loadingApps = loadingApps,
        mediaSupported = mediaSupported,
    )
}

enum class QuickAction(val label: String) {
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    DND("Silencio"),
    TORCH("Linterna"),
    ROTATE("Girar"),
    VOLUME("Sonido"),
    DISPLAY("Pantalla"),
    BATTERY("Batería"),
    SETTINGS("Ajustes"),
}

enum class Permission { CALENDAR, PHOTOS, NOTIFICATIONS_ACCESS }

/** Acceso directo de una app; [handle] es el ShortcutInfo real en el dispositivo. */
data class ShortcutItem(val label: String, val handle: Any?)

/** Todo lo que la interfaz puede pedir. Los valores por defecto no hacen nada (vistas previas y pruebas). */
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
    fun voiceSearch() {}
    fun quick(action: QuickAction) {}
    fun expandNotifications() {}
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {}
    suspend fun searchPlaces(query: String): List<WeatherPlace> = emptyList()
    fun refreshWeather() {}
    fun openWallpaperPicker() {}
    fun openDefaultLauncherSettings() {}
    fun setStyle(style: LauncherStyle) {}
    fun setWallpaperMode(mode: WallpaperMode) {}
    fun pickWallpaperPhoto() {}
    fun request(permission: Permission) {}
    fun openClock() {}
    fun openCalendar(event: CalendarEvent?) {}
    fun openPhoto(photos: PhotoUi) {}
    fun openMediaApp(media: MediaInfo) {}
    fun openMusicApp() {}
    fun mediaPlayPause() {}
    fun mediaNext() {}
    fun mediaPrevious() {}
    fun addTask(text: String) {}
    fun toggleTask(task: TaskItem) {}
    fun deleteTask(task: TaskItem) {}
    fun timerStart() {}
    fun timerPause() {}
    fun timerReset() {}
    fun timerMinutes(minutes: Int) {}
}

object PreviewActions : LauncherActions
