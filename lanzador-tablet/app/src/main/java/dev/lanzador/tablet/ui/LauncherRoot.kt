package dev.lanzador.tablet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lanzador.tablet.DeviceActions
import dev.lanzador.tablet.LauncherViewModel
import dev.lanzador.tablet.MainActivity
import dev.lanzador.tablet.data.NotificationHub

/** Conecta el ViewModel con la interfaz, que no sabe nada de Android. */
@Composable
fun LauncherRoot(vm: LauncherViewModel) {
    val apps by vm.repository.apps.collectAsStateWithLifecycle()
    val loaded by vm.repository.loaded.collectAsStateWithLifecycle()
    val favorites by vm.prefs.favorites.collectAsStateWithLifecycle()
    val dock by vm.prefs.dock.collectAsStateWithLifecycle()
    val hidden by vm.prefs.hidden.collectAsStateWithLifecycle()
    val usage by vm.prefs.usage.collectAsStateWithLifecycle()
    val settings by vm.prefs.settings.collectAsStateWithLifecycle()
    val tasks by vm.prefs.tasks.collectAsStateWithLifecycle()
    val timer by vm.prefs.timer.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val battery by vm.battery.collectAsStateWithLifecycle()
    val alarm by vm.nextAlarm.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val access by vm.notificationAccess.collectAsStateWithLifecycle()
    val media by NotificationHub.media.collectAsStateWithLifecycle()
    val notifications by NotificationHub.notifications.collectAsStateWithLifecycle()
    val darkText by vm.darkTextOnWallpaper.collectAsStateWithLifecycle()
    val homePresses by vm.homePresses.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    val activity = LocalContext.current as MainActivity
    val actions = remember(activity, vm) { DeviceActions(activity, vm) }

    val state = buildHomeState(
        apps = apps,
        favoriteKeys = favorites,
        dockKeys = dock,
        hiddenKeys = hidden,
        usage = usage,
        settings = settings,
        style = settings.effectiveStyle(today),
        weather = weather,
        battery = battery,
        nextAlarmMillis = alarm,
        calendar = calendar,
        media = media,
        notifications = notifications,
        notificationAccess = access,
        photos = photos,
        device = device,
        tasks = tasks,
        timer = timer,
        darkTextOnWallpaper = darkText,
        homePresses = homePresses,
        loadingApps = !loaded,
        mediaSupported = dev.lanzador.tablet.BuildConfig.NOTIFICACIONES,
    )
    LauncherScreen(state = state, actions = actions)
}
