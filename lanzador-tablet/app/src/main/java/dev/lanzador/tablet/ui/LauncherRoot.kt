package dev.lanzador.tablet.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lanzador.tablet.DeviceActions
import dev.lanzador.tablet.LauncherViewModel
import dev.lanzador.tablet.ui.theme.LauncherTheme

/** Conecta el ViewModel con la interfaz, que no sabe nada de Android. */
@Composable
fun LauncherRoot(vm: LauncherViewModel) {
    val apps by vm.repository.apps.collectAsStateWithLifecycle()
    val loaded by vm.repository.loaded.collectAsStateWithLifecycle()
    val favorites by vm.prefs.favorites.collectAsStateWithLifecycle()
    val dock by vm.prefs.dock.collectAsStateWithLifecycle()
    val hidden by vm.prefs.hidden.collectAsStateWithLifecycle()
    val usage by vm.prefs.usage.collectAsStateWithLifecycle()
    val note by vm.prefs.note.collectAsStateWithLifecycle()
    val settings by vm.prefs.settings.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val battery by vm.battery.collectAsStateWithLifecycle()
    val alarm by vm.nextAlarm.collectAsStateWithLifecycle()
    val darkText by vm.darkTextOnWallpaper.collectAsStateWithLifecycle()
    val homePresses by vm.homePresses.collectAsStateWithLifecycle()

    val activity = LocalContext.current as Activity
    val actions = remember(activity, vm) { DeviceActions(activity, vm) }

    val state = remember(
        apps, loaded, favorites, dock, hidden, usage, note, settings, weather, battery, alarm, darkText, homePresses,
    ) {
        buildHomeState(
            apps = apps,
            favoriteKeys = favorites,
            dockKeys = dock,
            hiddenKeys = hidden,
            usage = usage,
            settings = settings,
            note = note,
            weather = weather,
            battery = battery,
            nextAlarmMillis = alarm,
            darkTextOnWallpaper = darkText,
            homePresses = homePresses,
            loadingApps = !loaded,
        )
    }

    LauncherTheme(settings.theme) {
        LauncherScreen(state = state, actions = actions)
    }
}
