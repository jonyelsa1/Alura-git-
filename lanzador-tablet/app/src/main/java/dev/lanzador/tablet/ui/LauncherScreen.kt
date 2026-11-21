package dev.lanzador.tablet.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.WallpaperMode
import dev.lanzador.tablet.ui.common.Backdrop
import dev.lanzador.tablet.ui.common.LocalBackdrop
import dev.lanzador.tablet.ui.common.LocalHomeLongPress
import dev.lanzador.tablet.ui.common.WallpaperLayer
import dev.lanzador.tablet.ui.common.rememberBackdrop
import dev.lanzador.tablet.ui.styles.BentoHome
import dev.lanzador.tablet.ui.styles.CristalHome
import dev.lanzador.tablet.ui.styles.EnfoqueHome
import dev.lanzador.tablet.ui.styles.ExpresivoHome
import dev.lanzador.tablet.ui.theme.StyleTheme
import dev.lanzador.tablet.ui.theme.lightBackground

enum class Overlay { NONE, DRAWER, SETTINGS, STYLES, TASK }

/**
 * Raíz del launcher: fondo, pantalla de inicio del estilo elegido y capas
 * (cajón, ajustes, selector de estilo, nueva tarea). No toca Android salvo la
 * ventana; todo lo demás pasa por [actions].
 */
@Composable
fun LauncherScreen(
    state: HomeState,
    actions: LauncherActions,
    initialOverlay: Overlay = Overlay.NONE,
    backdropOverride: Backdrop? = null,
) {
    val style = state.style
    val mode = state.settings.wallpaperMode
    var overlay by rememberSaveable { mutableStateOf(initialOverlay) }
    var drawerKeyboard by remember { mutableStateOf(false) }
    var drawerLetter by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.homePresses) {
        if (state.homePresses > 0) {
            overlay = Overlay.NONE
            focusManager.clearFocus()
        }
    }
    BackHandler(enabled = true) { focusManager.clearFocus() }

    WindowSetup(
        showSystemWallpaper = mode == WallpaperMode.SYSTEM,
        hideNavBar = state.settings.fullScreen,
        darkIcons = when {
            overlay == Overlay.DRAWER || overlay == Overlay.SETTINGS -> lightBackground(style)
            mode == WallpaperMode.SYSTEM -> state.darkTextOnWallpaper && style != LauncherStyle.ENFOQUE
            else -> lightBackground(style)
        },
    )

    val nav = remember {
        HomeNav(
            openDrawer = { keyboard, letter -> drawerKeyboard = keyboard; drawerLetter = letter; overlay = Overlay.DRAWER },
            openSettings = { overlay = Overlay.SETTINGS },
            openStylePicker = { overlay = Overlay.STYLES },
            addTask = { overlay = Overlay.TASK },
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val density = LocalDensity.current.density
        val live = rememberBackdrop(style, mode, state.settings.wallpaperPhoto, IntSize(constraints.maxWidth, constraints.maxHeight), density)
        val backdrop = backdropOverride ?: live
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            WallpaperLayer(style, mode, backdrop)
            StyleTheme(style) {
                Box(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                        .homeGestures(
                            onSwipeUp = { nav.openDrawer(false, null) },
                            onSwipeDown = actions::expandNotifications,
                            onTap = { focusManager.clearFocus() },
                            onLongPress = nav.openStylePicker,
                        ),
                ) {
                    CompositionLocalProvider(LocalHomeLongPress provides nav.openStylePicker) {
                    when (style) {
                        LauncherStyle.CRISTAL -> CristalHome(state, actions, nav, landscape)
                        LauncherStyle.EXPRESIVO -> ExpresivoHome(state, actions, nav, landscape)
                        LauncherStyle.BENTO -> BentoHome(state, actions, nav, landscape)
                        LauncherStyle.ENFOQUE -> EnfoqueHome(state, actions, nav, landscape)
                    }
                    }
                }

                AnimatedVisibility(
                    visible = overlay == Overlay.DRAWER,
                    enter = fadeIn() + slideInVertically { it / 10 },
                    exit = fadeOut() + slideOutVertically { it / 10 },
                ) {
                    AppDrawer(state, actions, drawerKeyboard, drawerLetter, onClose = { overlay = Overlay.NONE })
                }
                TaskEditor(overlay == Overlay.TASK, actions, onClose = { overlay = Overlay.NONE })
                StylePicker(overlay == Overlay.STYLES, state, actions, onMore = { overlay = Overlay.SETTINGS }, onClose = { overlay = Overlay.NONE })
                SettingsPanel(overlay == Overlay.SETTINGS, state, actions, onClose = { overlay = Overlay.NONE })
            }
        }
    }
}

@Composable
private fun WindowSetup(showSystemWallpaper: Boolean, hideNavBar: Boolean, darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        if (showSystemWallpaper) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
            // Pantalla completa: la barra de tareas de la tablet se oculta en el
            // escritorio y aparece unos segundos al deslizar desde el borde inferior.
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hideNavBar) hide(WindowInsetsCompat.Type.navigationBars()) else show(WindowInsetsCompat.Type.navigationBars())
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
            ) { change, amount ->
                total += amount
                change.consume()
            }
        }
        .pointerInput(Unit) { detectTapGestures(onTap = { tap() }, onLongPress = { longPress() }) }
}
