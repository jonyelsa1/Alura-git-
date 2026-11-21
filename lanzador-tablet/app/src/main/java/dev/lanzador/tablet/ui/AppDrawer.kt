package dev.lanzador.tablet.ui

import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.ui.common.AppGlyph
import dev.lanzador.tablet.ui.common.AppTarget
import dev.lanzador.tablet.ui.common.ColorAppIcon
import dev.lanzador.tablet.ui.common.GlassAppIcon
import dev.lanzador.tablet.ui.common.LetterRail
import dev.lanzador.tablet.ui.common.LocalBackdrop
import dev.lanzador.tablet.ui.common.PolarShape
import dev.lanzador.tablet.ui.common.ShapedAppIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

private val COMBINING_MARKS = Regex("\\p{Mn}+")

fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

/** Primero las que empiezan por el texto, luego las que tienen una palabra que empieza así, luego el resto. */
fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return emptyList()
    return apps.mapNotNull { app ->
        val label = normalizeForSearch(app.label)
        val score = when {
            label.startsWith(q) -> 0
            label.split(' ', '-', '.', '_').any { it.startsWith(q) } -> 1
            label.contains(q) -> 2
            app.packageName.lowercase(Locale.ROOT).contains(q) -> 3
            else -> -1
        }
        if (score < 0) null else score to app
    }.sortedBy { it.first }.map { it.second }
}

fun sectionOf(label: String): String {
    val first = normalizeForSearch(label).firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else "#"
}

/** Etiquetas automáticas a partir de la categoría que declara cada app. */
enum class AppTag(val label: String) {
    ALL("Todas"), FAVORITES("Favoritas"), SOCIAL("Comunicación"), PRODUCTIVITY("Trabajo"),
    MEDIA("Multimedia"), NEWS("Lectura"), MAPS("Mapas"), GAMES("Juegos");

    fun matches(app: AppEntry, favorites: Set<String>): Boolean = when (this) {
        ALL -> true
        FAVORITES -> app.key in favorites
        SOCIAL -> app.category == ApplicationInfo.CATEGORY_SOCIAL
        PRODUCTIVITY -> app.category == ApplicationInfo.CATEGORY_PRODUCTIVITY
        MEDIA -> app.category in setOf(ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE)
        NEWS -> app.category == ApplicationInfo.CATEGORY_NEWS
        MAPS -> app.category == ApplicationInfo.CATEGORY_MAPS
        GAMES -> app.category == ApplicationInfo.CATEGORY_GAME
    }
}

sealed interface DrawerItem {
    val key: String

    data class Header(val title: String) : DrawerItem {
        override val key get() = "h:$title"
    }

    data class App(val app: AppEntry, val section: String) : DrawerItem {
        override val key get() = "$section:${app.key}"
    }
}

fun buildDrawerItems(apps: List<AppEntry>, frequent: List<AppEntry>): List<DrawerItem> {
    val items = ArrayList<DrawerItem>(apps.size + frequent.size + 2)
    if (frequent.isNotEmpty()) {
        items += DrawerItem.Header("Frecuentes")
        frequent.forEach { items += DrawerItem.App(it, "frecuentes") }
        items += DrawerItem.Header("Todas · A–Z")
    }
    apps.forEach { items += DrawerItem.App(it, sectionOf(it.label)) }
    return items
}

private val ExpresivoShapes: List<Shape> = listOf(CircleShape, RoundedCornerShape(34), PolarShape(9, 0.09f), PolarShape(4, 0.16f))

@Composable
fun AppDrawer(
    state: HomeState,
    actions: LauncherActions,
    focusSearch: Boolean,
    initialLetter: String?,
    onClose: () -> Unit,
) {
    val style = state.style
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf(AppTag.ALL) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val currentOnClose by rememberUpdatedState(onClose)
    val close: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        currentOnClose()
    }
    BackHandler { if (query.isNotEmpty()) query = "" else close() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            delay(150)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    val scheme = MaterialTheme.colorScheme
    val results = remember(query, state.apps) { filterApps(state.apps, query) }
    val pullThreshold = with(LocalDensity.current) { 120.dp.toPx() }
    val pullToClose = remember(pullThreshold) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > pullThreshold) {
                        pulled = 0f
                        close()
                    }
                    return Offset(0f, available.y)
                }
                if (consumed.y != 0f) pulled = 0f
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
        Box(Modifier.fillMaxSize().drawerBackground(style)) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 40.dp, end = 20.dp, top = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    SearchBox(
                        style, query, { query = it }, focusRequester,
                        hint = "Buscar en ${state.apps.size} apps",
                        onGo = {
                            val first = results.firstOrNull()
                            when {
                                first != null -> { actions.launch(first); close() }
                                query.isNotBlank() -> { actions.webSearch(query.trim()); close() }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(fieldColor(style)).clickable(onClickLabel = "Cerrar", onClick = close),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "Cerrar", Modifier.size(26.dp)) }
                }
                if (query.isBlank()) {
                    val tags = remember(state.apps, state.favoriteKeys) {
                        AppTag.entries.filter { t -> t == AppTag.ALL || state.apps.any { t.matches(it, state.favoriteKeys) } }
                    }
                    Row(Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        tags.forEach { t -> TagChip(style, t.label, t == tag) { tag = t } }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.weight(1f).nestedScroll(pullToClose)) {
                    if (query.isBlank()) {
                        val apps = if (tag == AppTag.ALL) state.apps else state.apps.filter { tag.matches(it, state.favoriteKeys) }
                        AllApps(state, actions, apps, if (tag == AppTag.ALL) state.frequent else emptyList(), initialLetter, close)
                    } else {
                        Results(state, actions, results, query, close)
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.drawerBackground(style: LauncherStyle): Modifier {
    val backdrop = LocalBackdrop.current
    return when (style) {
        LauncherStyle.EXPRESIVO -> background(Color(0xFFF7EFE7))
        LauncherStyle.ENFOQUE -> background(Color.Black)
        else -> drawBehind {
            val img = backdrop.blurred
            if (img != null) {
                drawImage(img, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.High)
                drawRect(if (style == LauncherStyle.CRISTAL) Color(0x8C12082F) else Color(0xC20C0818))
            } else {
                drawRect(if (style == LauncherStyle.CRISTAL) Color(0xF01B1238) else Color(0xF515102A))
            }
        }
    }
}

private fun fieldColor(style: LauncherStyle): Color = when (style) {
    LauncherStyle.CRISTAL -> Color.White.copy(alpha = 0.16f)
    LauncherStyle.EXPRESIVO -> Color.White
    LauncherStyle.BENTO -> Color.White.copy(alpha = 0.08f)
    LauncherStyle.ENFOQUE -> Color(0xFF141414)
}

@Composable
private fun SearchBox(
    style: LauncherStyle,
    query: String,
    onChange: (String) -> Unit,
    focusRequester: FocusRequester,
    hint: String,
    onGo: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val text = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, color = scheme.onSurface)
    Row(
        modifier.height(60.dp).clip(RoundedCornerShape(30.dp)).background(fieldColor(style)).padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, Modifier.size(23.dp), tint = if (style == LauncherStyle.EXPRESIVO) scheme.primary else scheme.onSurface)
        Spacer(Modifier.width(14.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = text,
            cursorBrush = SolidColor(if (style == LauncherStyle.ENFOQUE) Color(0xFFE5302A) else scheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(hint, style = text.copy(color = scheme.onSurfaceVariant))
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Borrar", Modifier.size(22.dp))
            }
        } else {
            Text("Intro: buscar en la web", style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant), modifier = Modifier.padding(end = 14.dp))
        }
    }
}

@Composable
private fun TagChip(style: LauncherStyle, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when {
        selected && style == LauncherStyle.CRISTAL -> Color.White to Color(0xFF2A1460)
        selected && style == LauncherStyle.ENFOQUE -> Color(0xFFE5302A) to Color.White
        selected -> scheme.primary to scheme.onPrimary
        else -> fieldColor(style) to scheme.onSurface
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg),
        modifier = Modifier.clip(RoundedCornerShape(19.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp),
    )
}

@Composable
private fun DrawerTile(state: HomeState, actions: LauncherActions, app: AppEntry, index: Int, onLaunched: () -> Unit) {
    val style = state.style
    val badge = state.notifications[app.packageName]?.count ?: 0
    val scheme = MaterialTheme.colorScheme
    if (style == LauncherStyle.ENFOQUE) {
        AppTarget(app, state, actions, shape = RoundedCornerShape(12.dp), onLaunched = onLaunched, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { AppGlyph(app, 40.dp, Color(0xFF8B8B8B), monochromeFallback = true) }
                Spacer(Modifier.width(14.dp))
                Text(app.label, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, color = Color(0xFFE8E8E8)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (badge > 0) {
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE5302A)))
                }
            }
        }
        return
    }
    val iconSize: Dp = when (style) {
        LauncherStyle.EXPRESIVO -> 72.dp
        else -> 64.dp
    }
    val shape: Shape = if (style == LauncherStyle.EXPRESIVO) ExpresivoShapes[index % ExpresivoShapes.size] else RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTarget(app, state, actions, shape = shape, onLaunched = onLaunched) {
            when (style) {
                LauncherStyle.CRISTAL -> GlassAppIcon(app, iconSize, tinted = true, badge = badge)
                LauncherStyle.EXPRESIVO -> ShapedAppIcon(app, iconSize, shape, badge = badge)
                else -> ColorAppIcon(app, iconSize, badge = badge)
            }
        }
        Text(
            app.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
    }
}

private fun columnsFor(style: LauncherStyle) =
    if (style == LauncherStyle.ENFOQUE) GridCells.Adaptive(280.dp) else GridCells.Adaptive(if (style == LauncherStyle.EXPRESIVO) 124.dp else 112.dp)

@Composable
private fun AllApps(
    state: HomeState,
    actions: LauncherActions,
    apps: List<AppEntry>,
    frequent: List<AppEntry>,
    initialLetter: String?,
    onLaunched: () -> Unit,
) {
    val style = state.style
    val scheme = MaterialTheme.colorScheme
    val items = remember(apps, frequent) { buildDrawerItems(apps, frequent) }
    val letters = remember(apps) { apps.map { sectionOf(it.label) }.distinct() }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    fun indexOf(letter: String) = items.indexOfFirst { it is DrawerItem.App && it.section == letter }

    LaunchedEffect(initialLetter, items) {
        if (initialLetter != null) {
            val i = indexOf(initialLetter)
            if (i >= 0) grid.scrollToItem(i)
        }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (state.loadingApps) "Cargando apps…" else "No hay apps con esta etiqueta", style = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurfaceVariant))
        }
        return
    }

    Row(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = columnsFor(style),
            state = grid,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(
                items = items,
                key = { it.key },
                span = { if (it is DrawerItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                contentType = { if (it is DrawerItem.Header) 0 else 1 },
            ) { item ->
                when (item) {
                    is DrawerItem.Header -> Text(
                        item.title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = scheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 4.dp),
                    )
                    is DrawerItem.App -> DrawerTile(state, actions, item.app, items.indexOf(item), onLaunched)
                }
            }
        }
        if (letters.size > 3) {
            val (idle, active) = when (style) {
                LauncherStyle.ENFOQUE -> Color(0xFF4A4A4A) to Color(0xFFE5302A)
                LauncherStyle.EXPRESIVO -> Color(0xFF7A7287) to Color(0xFF6F43E0)
                LauncherStyle.BENTO -> Color(0xFFA99FC4) to Color(0xFFFFB547)
                LauncherStyle.CRISTAL -> Color.White.copy(alpha = 0.7f) to Color.White
            }
            LetterRail(
                letters,
                Modifier.width(40.dp).fillMaxHeight().padding(vertical = 10.dp),
                idle = idle, active = active,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                onPick = { l -> val i = indexOf(l); if (i >= 0) scope.launch { grid.scrollToItem(i) } },
            )
        }
    }
}

@Composable
private fun Results(state: HomeState, actions: LauncherActions, results: List<AppEntry>, query: String, onLaunched: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(end = 20.dp)) {
        Row(
            Modifier.padding(top = 4.dp).clip(RoundedCornerShape(24.dp)).background(fieldColor(state.style))
                .clickable { actions.webSearch(query.trim()); onLaunched() }.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Public, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Buscar «${query.trim()}» en la web", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, color = scheme.onSurface))
        }
        Spacer(Modifier.height(10.dp))
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ninguna app coincide con «${query.trim()}»", style = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurfaceVariant))
            }
        } else {
            LazyVerticalGrid(columns = columnsFor(state.style), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(results, key = { it.key }) { app -> DrawerTile(state, actions, app, results.indexOf(app), onLaunched) }
            }
        }
    }
}
