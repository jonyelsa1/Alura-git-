package dev.lanzador.tablet

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.LauncherSettings
import dev.lanzador.tablet.data.WeatherClient
import dev.lanzador.tablet.data.WeatherPlace
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.ShortcutItem

/** Implementación real de las acciones: abre apps, ajustes del sistema, etc. */
class DeviceActions(
    private val activity: Activity,
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
        val visible = prefs.list(kind).filter { it in installed }
        prefs.move(kind, app.key, delta, visible)
    }

    override fun setHidden(app: AppEntry, hidden: Boolean) = prefs.setHidden(app.key, hidden)

    override fun openAppInfo(app: AppEntry) {
        if (!repo.openAppInfo(app)) toast("No se pudo abrir la información de la app")
    }

    override fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .putExtra(Intent.EXTRA_USER, app.user)
        start(intent)
    }

    override fun shortcutsFor(app: AppEntry): List<ShortcutItem> =
        repo.shortcutsFor(app).map { info ->
            ShortcutItem((info.shortLabel ?: info.longLabel ?: info.id).toString(), info)
        }

    override fun startShortcut(item: ShortcutItem) {
        val info = item.handle as? ShortcutInfo ?: return
        if (!repo.startShortcut(info)) toast("No se pudo abrir el acceso directo")
    }

    override fun webSearch(query: String) {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
        start(search, fallback)
    }

    override fun quick(action: QuickAction) {
        when (action) {
            QuickAction.WIFI -> start(Intent(Settings.Panel.ACTION_WIFI), Intent(Settings.ACTION_WIFI_SETTINGS))
            QuickAction.BLUETOOTH -> start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            QuickAction.VOLUME -> start(Intent(Settings.Panel.ACTION_VOLUME), Intent(Settings.ACTION_SOUND_SETTINGS))
            QuickAction.DISPLAY -> start(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            QuickAction.BATTERY -> start(
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            )
            QuickAction.SETTINGS -> start(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Abre el panel de notificaciones. Requiere el permiso normal EXPAND_STATUS_BAR. */
    @SuppressLint("WrongConstant")
    override fun expandNotifications() {
        runCatching {
            val service = activity.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        }
    }

    override fun updateNote(text: String) = prefs.setNote(text)

    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = prefs.updateSettings(transform)

    override suspend fun searchPlaces(query: String): List<WeatherPlace> = WeatherClient.searchPlaces(query)

    override fun refreshWeather() = vm.refreshWeather(force = true)

    override fun openWallpaperPicker() {
        start(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Elegir fondo de pantalla"))
    }

    override fun openDefaultLauncherSettings() {
        start(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    private fun start(vararg intents: Intent) {
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        toast("Esta tablet no tiene esa pantalla de ajustes")
    }

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
}
