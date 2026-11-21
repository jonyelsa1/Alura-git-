package dev.lanzador.tablet.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.lanzador.tablet.data.AppListKind
import java.time.LocalDateTime

/**
 * Pantalla completa del launcher. Sin estado propio de Android: todo llega en
 * [state] y sale por [actions], así se puede dibujar en pruebas y vistas previas.
 */
@Composable
fun LauncherScreen(
    state: HomeState,
    actions: LauncherActions,
    fixedTime: LocalDateTime? = null,
    initialDrawerOpen: Boolean = false,
    initialSettingsOpen: Boolean = false,
) {
    var drawerOpen by rememberSaveable { mutableStateOf(initialDrawerOpen) }
    var drawerWantsKeyboard by remember { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(initialSettingsOpen) }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.homePresses) {
        if (state.homePresses > 0) {
            drawerOpen = false
            settingsOpen = false
            noteOpen = false
            focusManager.clearFocus()
        }
    }

    // En el escritorio «Atrás» no hace nada (un launcher no se cierra).
    BackHandler(enabled = true) { focusManager.clearFocus() }

    val darkSurfaces = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val overlayOpen = drawerOpen || settingsOpen || noteOpen
    SystemBarIcons(darkIcons = if (overlayOpen) !darkSurfaces else state.darkTextOnWallpaper)

    val onWallpaper = if (state.darkTextOnWallpaper) Color(0xFF15151B) else Color.White

    val openDrawer: (Boolean) -> Unit = { keyboard ->
        drawerWantsKeyboard = keyboard
        drawerOpen = true
    }

    Box(Modifier.fillMaxSize()) {
        HomeContent(
            state = state,
            actions = actions,
            fixedTime = fixedTime,
            onWallpaper = onWallpaper,
            openDrawer = openDrawer,
            openSettings = { settingsOpen = true },
            openNote = { noteOpen = true },
            modifier = Modifier.homeGestures(
                onSwipeUp = { openDrawer(false) },
                onSwipeDown = actions::expandNotifications,
                onTap = { focusManager.clearFocus() },
                onLongPress = { settingsOpen = true },
            ),
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            AppDrawer(
                state = state,
                actions = actions,
                focusSearch = drawerWantsKeyboard,
                onClose = { drawerOpen = false },
            )
        }

        NoteEditor(
            visible = noteOpen,
            initial = state.note,
            onChange = actions::updateNote,
            onClose = { noteOpen = false },
        )

        SettingsPanel(
            visible = settingsOpen,
            state = state,
            actions = actions,
            onClose = { settingsOpen = false },
        )
    }
}

@Composable
private fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}

@Composable
private fun Modifier.homeGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
): Modifier {
    val up by rememberUpdatedState(onSwipeUp)
    val down by rememberUpdatedState(onSwipeDown)
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }
    return this
        .pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragCancel = { total = 0f },
                onDragEnd = {
                    when {
                        total < -threshold -> up()
                        total > threshold -> down()
                    }
                },
            ) { change, dragAmount ->
                total += dragAmount
                change.consume()
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onTap = { tap() }, onLongPress = { longPress() })
        }
}

@Composable
private fun HomeContent(
    state: HomeState,
    actions: LauncherActions,
    fixedTime: LocalDateTime?,
    onWallpaper: Color,
    openDrawer: (Boolean) -> Unit,
    openSettings: () -> Unit,
    openNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            // Sin el teclado: si aparece, se superpone en lugar de aplastar el escritorio.
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
    ) {
        val landscape = maxWidth > maxHeight
        val roomy = maxWidth >= 1000.dp || maxHeight >= 1000.dp
        val gutter = if (roomy) 40.dp else 24.dp
        val gap = if (roomy) 20.dp else 14.dp
        val bigClock = (if (landscape) maxHeight else maxWidth) >= 700.dp
        val iconSize = state.settings.iconSize.dp.dp

        val clock = @Composable { m: Modifier ->
            ClockBlock(
                now = rememberNow(fixedTime),
                battery = state.battery,
                nextAlarmMillis = state.nextAlarmMillis,
                color = onWallpaper,
                big = bigClock,
                modifier = m,
            )
        }
        val weather = @Composable { m: Modifier ->
            WeatherCard(
                weather = state.weather,
                onClick = { if (state.settings.weatherPlace == null) openSettings() else actions.refreshWeather() },
                modifier = m,
            )
        }
        val note = @Composable { m: Modifier -> NoteCard(text = state.note, onClick = openNote, modifier = m) }
        val searchRow = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchPill(onClick = { openDrawer(true) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                RoundIconAction(
                    icon = Icons.Rounded.Settings,
                    description = "Ajustes del lanzador",
                    onClick = openSettings,
                )
            }
        }
        val favorites = @Composable { m: Modifier ->
            FavoritesPanel(
                apps = state.favorites,
                state = state,
                actions = actions,
                iconSize = iconSize,
                onOpenDrawer = { openDrawer(false) },
                modifier = m,
            )
        }
        val dock = @Composable { m: Modifier ->
            Dock(
                apps = state.dock,
                state = state,
                actions = actions,
                iconSize = iconSize,
                onOpenDrawer = { openDrawer(false) },
                modifier = m,
            )
        }

        if (landscape) {
            Column(Modifier.fillMaxSize().padding(horizontal = gutter, vertical = gap)) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    Column(
                        Modifier.weight(0.42f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        clock(Modifier)
                        if (state.settings.showNote) {
                            if (state.settings.showWeather) weather(Modifier.fillMaxWidth())
                            note(Modifier.fillMaxWidth().weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                            if (state.settings.showWeather) weather(Modifier.fillMaxWidth())
                        }
                    }
                    Column(
                        Modifier.weight(0.58f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        searchRow()
                        favorites(Modifier.fillMaxWidth().weight(1f))
                        if (state.settings.showQuickSettings) QuickSettingsRow(onAction = actions::quick)
                    }
                }
                Spacer(Modifier.height(gap))
                dock(Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = gutter, vertical = gap),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                clock(Modifier.padding(top = 8.dp))
                if (state.settings.showWeather || state.settings.showNote) {
                    Row(
                        Modifier.fillMaxWidth().height(cardRowHeight(roomy)),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        if (state.settings.showWeather) weather(Modifier.weight(1f).fillMaxHeight())
                        if (state.settings.showNote) note(Modifier.weight(1f).fillMaxHeight())
                    }
                }
                searchRow()
                favorites(Modifier.fillMaxWidth().weight(1f))
                if (state.settings.showQuickSettings) QuickSettingsRow(onAction = actions::quick)
                dock(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private fun cardRowHeight(roomy: Boolean): Dp = if (roomy) 168.dp else 140.dp

/** Contexto del menú de una app: en qué lista está y en qué posición, para «Mover antes/después». */
data class TileMenuContext(
    val kind: AppListKind? = null,
    val index: Int = -1,
    val size: Int = 0,
)
