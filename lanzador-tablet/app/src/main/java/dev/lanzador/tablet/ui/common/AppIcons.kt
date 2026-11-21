package dev.lanzador.tablet.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions

/** Contexto del menú de una app: en qué lista está y en qué posición, para «Mover». */
data class TileMenuContext(val kind: AppListKind? = null, val index: Int = -1, val size: Int = 0)

/**
 * Zona pulsable de una app: toque para abrir, pulsación larga para el menú con
 * accesos directos, favoritos, dock, orden, información, ocultar y desinstalar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTarget(
    app: AppEntry,
    state: HomeState,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    menu: TileMenuContext = TileMenuContext(),
    onLaunched: () -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // La zona pulsable va encima y recortada a la forma; el contenido no se recorta
    // para que el contador de avisos pueda sobresalir del icono.
    Box(modifier, contentAlignment = Alignment.Center) {
        content()
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClickLabel = "Abrir ${app.label}",
                    onLongClickLabel = "Opciones de ${app.label}",
                    onClick = { actions.launch(app); onLaunched() },
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); open = true },
                ),
        )
        AppMenu(open, { open = false }, app, app.key in state.favoriteKeys, app.key in state.dockKeys, menu, actions, onLaunched)
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
        shortcuts.forEach { s ->
            MenuEntry(s.label, Icons.Rounded.Bolt) { onDismiss(); actions.startShortcut(s); onLaunched() }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 4.dp))
        MenuEntry(
            if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
            if (isFavorite) Icons.Rounded.StarBorder else Icons.Rounded.Star,
        ) { onDismiss(); actions.toggleFavorite(app) }
        MenuEntry(if (isInDock) "Quitar del dock" else "Fijar en el dock", Icons.Rounded.PushPin) {
            onDismiss(); actions.toggleDock(app)
        }
        val kind = context.kind
        if (kind != null && context.size > 1) {
            @Suppress("DEPRECATION")
            if (context.index > 0) MenuEntry("Mover antes", Icons.Rounded.ChevronLeft) { onDismiss(); actions.move(kind, app, -1) }
            @Suppress("DEPRECATION")
            if (context.index < context.size - 1) MenuEntry("Mover después", Icons.Rounded.ChevronRight) { onDismiss(); actions.move(kind, app, 1) }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        MenuEntry("Información de la app", Icons.Rounded.Info) { onDismiss(); actions.openAppInfo(app) }
        if (kind == null) MenuEntry("Ocultar del cajón", Icons.Rounded.VisibilityOff) { onDismiss(); actions.setHidden(app, true) }
        if (!app.isSystem) MenuEntry("Desinstalar", Icons.Rounded.Delete) { onDismiss(); actions.uninstall(app) }
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

// ---------- Cómo se dibuja el icono en cada estilo ----------

/**
 * Pictograma de la app dentro de un contenedor de tamaño [box]: la capa monocroma
 * si existe (Android 13+), si no la capa delantera, y si no el icono entero.
 */
@Composable
fun AppGlyph(app: AppEntry, box: Dp, tint: Color, monochromeFallback: Boolean = false) {
    val glyph = app.glyph
    val fg = app.foreground
    val gray = if (monochromeFallback) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    when {
        glyph != null -> Image(glyph, contentDescription = null, modifier = Modifier.size(box * 0.72f), colorFilter = ColorFilter.tint(tint))
        fg != null -> Image(fg, contentDescription = null, modifier = Modifier.size(box * 0.92f), colorFilter = gray)
        else -> Image(app.icon, contentDescription = null, modifier = Modifier.size(box * 0.6f).clip(CircleShape), colorFilter = gray)
    }
}

/** Icono de cristal (Cristal). Con [tinted], el cristal toma el color de la app. */
@Composable
fun GlassAppIcon(app: AppEntry, size: Dp, tinted: Boolean = false, badge: Int = 0) {
    Box {
        GlassSurface(Modifier.size(size), shape = RoundedCornerShape(size * 0.32f)) {
            if (tinted) {
                val c = Color(app.tint)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.radialGradient(0f to c.copy(alpha = 0.75f), 0.6f to c.copy(alpha = 0.35f), 1f to c.copy(alpha = 0.08f)),
                    ),
                )
            }
            Box(Modifier.align(Alignment.Center)) { AppGlyph(app, size, Color.White) }
        }
        if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd))
    }
}

/** Icono sobre una forma de color pastel derivado del propio icono (Expresivo). */
@Composable
fun ShapedAppIcon(app: AppEntry, size: Dp, shape: Shape, container: Color? = null, content: Color? = null, badge: Int = 0) {
    val base = Color(app.tint)
    val bg = container ?: lerp(base, Color.White, 0.72f)
    val fg = content ?: lerp(base, Color.Black, 0.62f)
    Box {
        Box(Modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
            AppGlyph(app, size, fg)
        }
        if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd))
    }
}

/** Icono a todo color tal cual lo da el sistema (Bento). */
@Composable
fun ColorAppIcon(app: AppEntry, size: Dp, badge: Int = 0) {
    Box {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(size))
        if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun Badge(count: Int, modifier: Modifier = Modifier, color: Color = Color(0xFFFF3B6B)) {
    Box(
        modifier
            .padding(0.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 9) "9+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
