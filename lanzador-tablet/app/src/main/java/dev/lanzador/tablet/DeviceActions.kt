package dev.lanzador.tablet

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.CalendarEvent
import dev.lanzador.tablet.data.FocusTimer
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.MediaInfo
import dev.lanzador.tablet.data.NotificationHub
import dev.lanzador.tablet.data.NotificationService
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.data.TaskItem
import dev.lanzador.tablet.data.WallpaperMode
import dev.lanzador.tablet.data.WeatherClient
import dev.lanzador.tablet.data.WeatherPlace
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.ShortcutItem

/** Implementación real de las acciones: abre apps, ajustes del sistema, pide permisos, etc. */
class DeviceActions(
    private val activity: MainActivity,
    private val vm: LauncherViewModel,
) : LauncherActions {

    private val repo get() = vm.repository
    private val prefs get() = vm.prefs

    override fun launch(app: AppEntry) {
        if (repo.launch(app)) prefs.recordLaunch(app.key) else toast("No se pudo abrir ${app.label}")
    }

    override fun toggleFavorite(app: AppEntry) = prefs.toggle(AppListKind.FAVORITES, app.key)

    override fun toggleDock(app: AppEntry) = prefs.toggle(AppListKind.DOCK, app.key)

    override fun move(kind: AppListKind, app: AppEntry, delta: Int) {
        val installed = repo.apps.value.mapTo(HashSet()) { it.key }
        prefs.move(kind, app.key, delta, prefs.list(kind).filter { it in installed })
    }

    override fun setHidden(app: AppEntry, hidden: Boolean) = prefs.setHidden(app.key, hidden)

    override fun openAppInfo(app: AppEntry) {
        if (!repo.openAppInfo(app)) toast("No se pudo abrir la información de la app")
    }

    override fun uninstall(app: AppEntry) {
        start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)).putExtra(Intent.EXTRA_USER, app.user))
    }

    override fun shortcutsFor(app: AppEntry): List<ShortcutItem> =
        repo.shortcutsFor(app).map { ShortcutItem((it.shortLabel ?: it.longLabel ?: it.id).toString(), it) }

    override fun startShortcut(item: ShortcutItem) {
        val info = item.handle as? ShortcutInfo ?: return
        if (!repo.startShortcut(info)) toast("No se pudo abrir el acceso directo")
    }

    override fun webSearch(query: String) {
        start(
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))),
        )
    }

    override fun voiceSearch() {
        start(Intent(RecognizerIntent.ACTION_WEB_SEARCH), Intent(Intent.ACTION_VOICE_COMMAND), Intent(Intent.ACTION_ASSIST))
    }

    override fun quick(action: QuickAction) {
        when (action) {
            QuickAction.WIFI -> start(Intent(Settings.Panel.ACTION_WIFI), Intent(Settings.ACTION_WIFI_SETTINGS))
            QuickAction.BLUETOOTH -> start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            QuickAction.VOLUME -> start(Intent(Settings.Panel.ACTION_VOLUME), Intent(Settings.ACTION_SOUND_SETTINGS))
            QuickAction.DISPLAY, QuickAction.ROTATE -> start(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            QuickAction.BATTERY -> start(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            QuickAction.SETTINGS -> start(Intent(Settings.ACTION_SETTINGS))
            QuickAction.TORCH -> vm.toggleTorch()
            QuickAction.DND -> toggleDnd()
        }
    }

    private fun toggleDnd() {
        val nm = activity.getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted) {
            val on = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            vm.refreshDevice()
        } else {
            toast("Permite a Lanzador controlar «No molestar»")
            start(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    /** Abre el panel de notificaciones. Requiere el permiso normal EXPAND_STATUS_BAR. */
    @SuppressLint("WrongConstant")
    override fun expandNotifications() {
        runCatching {
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(activity.getSystemService("statusbar"))
        }
    }

    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = prefs.updateSettings(transform)

    override suspend fun searchPlaces(query: String): List<WeatherPlace> = WeatherClient.searchPlaces(query)

    override fun refreshWeather() = vm.refreshWeather(force = true)

    override fun openWallpaperPicker() {
        start(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Elegir fondo de pantalla"))
    }

    override fun openDefaultLauncherSettings() {
        start(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    override fun setStyle(style: LauncherStyle) = prefs.updateSettings { it.copy(style = style, dailyRotation = false) }

    override fun setWallpaperMode(mode: WallpaperMode) = prefs.updateSettings { it.copy(wallpaperMode = mode) }

    override fun pickWallpaperPhoto() = activity.pickWallpaper()

    override fun request(permission: Permission) {
        when (permission) {
            Permission.CALENDAR -> activity.requestCalendar()
            Permission.PHOTOS -> activity.requestPhotos()
            Permission.NOTIFICATIONS_ACCESS -> if (!BuildConfig.NOTIFICACIONES) openMusicApp() else {
                val component = ComponentName(activity, NotificationService::class.java)
                toast("Activa «Lanzador». Si sale bloqueado: Info de la app ▸ ⋮ ▸ Permitir ajustes restringidos")
                val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
                } else null
                start(*listOfNotNull(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)).toTypedArray())
            }
        }
    }

    override fun openClock() {
        start(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    override fun openCalendar(event: CalendarEvent?) {
        if (event != null) {
            start(
                Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end),
            )
        } else {
            val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").also { ContentUris.appendId(it, System.currentTimeMillis()) }.build()
            start(Intent(Intent.ACTION_VIEW, uri), Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        }
    }

    override fun openPhoto(photos: PhotoUi) {
        if (photos is PhotoUi.Ready) {
            start(Intent(Intent.ACTION_VIEW).setDataAndType(photos.uri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } else {
            start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY))
        }
    }

    override fun openMediaApp(media: MediaInfo) {
        activity.packageManager.getLaunchIntentForPackage(media.packageName)?.let { start(it) }
    }

    override fun openMusicApp() {
        start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
    }

    override fun mediaPlayPause() = NotificationHub.playPause()
    override fun mediaNext() = NotificationHub.next()
    override fun mediaPrevious() = NotificationHub.previous()

    override fun addTask(text: String) = prefs.addTask(text)
    override fun toggleTask(task: TaskItem) = prefs.toggleTask(task.id)
    override fun deleteTask(task: TaskItem) {
        prefs.deleteTask(task.id)
        toast("Tarea borrada")
    }

    override fun timerStart() {
        activity.requestNotificationsOnce()
        FocusTimer.start(activity)
    }

    override fun timerPause() = FocusTimer.pause(activity)
    override fun timerReset() = FocusTimer.reset(activity)
    override fun timerMinutes(minutes: Int) = FocusTimer.setMinutes(activity, minutes)

    private fun start(vararg intents: Intent) {
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        toast("Esta tablet no tiene esa pantalla")
    }

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
