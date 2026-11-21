package dev.lanzador.tablet.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind

/** Icono + nombre. Toque: abrir. Pulsación larga: menú con accesos directos y opciones. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppEntry,
    iconSize: Dp,
    state: HomeState,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showLabel: Boolean = true,
    menuContext: TileMenuContext = TileMenuContext(),
    onLaunched: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box(modifier) {
        Column(
            Modifier
                .then(if (showLabel) Modifier.fillMaxWidth() else Modifier)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClickLabel = "Abrir ${app.label}",
                    onLongClickLabel = "Opciones de ${app.label}",
                    onClick = {
                        actions.launch(app)
                        onLaunched()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(horizontal = 6.dp, vertical = if (showLabel) 10.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(iconSize))
            if (showLabel) {
                Spacer(Modifier.height(8.dp))
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AppMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            app = app,
            isFavorite = app.key in state.favoriteKeys,
            isInDock = app.key in state.dockKeys,
            context = menuContext,
            actions = actions,
            onLaunched = onLaunched,
        )
    }
}

@Composable
private fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    app: AppEntry,
    isFavorite: Boolean,
    isInDock: Boolean,
    context: TileMenuContext,
    actions: LauncherActions,
    onLaunched: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val shortcuts = remember(expanded, app.key) { if (expanded) actions.shortcutsFor(app) else emptyList() }
        Text(
            app.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        shortcuts.forEach { shortcut ->
            MenuEntry(shortcut.label, Icons.Rounded.Bolt) {
                onDismiss()
                actions.startShortcut(shortcut)
                onLaunched()
            }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 4.dp))
        MenuEntry(
            if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
            if (isFavorite) Icons.Rounded.StarBorder else Icons.Rounded.Star,
        ) { onDismiss(); actions.toggleFavorite(app) }
        MenuEntry(
            if (isInDock) "Quitar del dock" else "Fijar en el dock",
            Icons.Rounded.PushPin,
        ) { onDismiss(); actions.toggleDock(app) }
        val kind = context.kind
        if (kind != null && context.size > 1) {
            val before = if (kind == AppListKind.DOCK) "Mover a la izquierda" else "Mover antes"
            val after = if (kind == AppListKind.DOCK) "Mover a la derecha" else "Mover después"
            if (context.index > 0) {
                @Suppress("DEPRECATION")
                MenuEntry(before, Icons.Rounded.ChevronLeft) { onDismiss(); actions.move(kind, app, -1) }
            }
            if (context.index < context.size - 1) {
                @Suppress("DEPRECATION")
                MenuEntry(after, Icons.Rounded.ChevronRight) { onDismiss(); actions.move(kind, app, +1) }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        MenuEntry("Información de la app", Icons.Rounded.Info) { onDismiss(); actions.openAppInfo(app) }
        if (kind == null) {
            MenuEntry("Ocultar del cajón", Icons.Rounded.VisibilityOff) { onDismiss(); actions.setHidden(app, true) }
        }
        if (!app.isSystem) {
            MenuEntry("Desinstalar", Icons.Rounded.Delete) { onDismiss(); actions.uninstall(app) }
        }
    }
}

@Composable
private fun MenuEntry(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun FavoritesPanel(
    apps: List<AppEntry>,
    state: HomeState,
    actions: LauncherActions,
    iconSize: Dp,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Favoritos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenDrawer) {
                Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Todas las apps")
            }
        }
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.loadingApps) "Cargando apps…"
                    else "Aún no hay favoritos.\nAbre «Todas las apps», mantén pulsada una y elige «Añadir a favoritos».",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(iconSize + 48.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp),
            ) {
                itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                    AppTile(
                        app = app,
                        iconSize = iconSize,
                        state = state,
                        actions = actions,
                        menuContext = TileMenuContext(AppListKind.FAVORITES, index, apps.size),
                    )
                }
            }
        }
    }
}

@Composable
fun Dock(
    apps: List<AppEntry>,
    state: HomeState,
    actions: LauncherActions,
    iconSize: Dp,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dockIcon = iconSize * 0.86f
    val shown = apps.take(10)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = scheme.onSurface,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClickLabel = "Todas las apps", onClick = onOpenDrawer)
                    .padding(6.dp),
            ) {
                Box(
                    Modifier.size(dockIcon).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Apps,
                        contentDescription = "Todas las apps",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(dockIcon * 0.46f),
                    )
                }
            }
            if (shown.isEmpty()) {
                Text(
                    "Mantén pulsada una app y elige «Fijar en el dock»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                VerticalDivider(Modifier.height(dockIcon * 0.6f).padding(horizontal = 8.dp))
                shown.forEachIndexed { index, app ->
                    AppTile(
                        app = app,
                        iconSize = dockIcon,
                        state = state,
                        actions = actions,
                        showLabel = false,
                        menuContext = TileMenuContext(AppListKind.DOCK, index, shown.size),
                    )
                }
            }
        }
    }
}
