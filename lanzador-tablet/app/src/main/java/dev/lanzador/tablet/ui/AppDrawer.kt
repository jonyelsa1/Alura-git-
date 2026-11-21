package dev.lanzador.tablet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.lanzador.tablet.data.AppEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

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

private fun sectionOf(label: String): String {
    val first = normalizeForSearch(label).firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else "#"
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

/** «Frecuentes» arriba y después todas las apps seguidas, sin una cabecera por letra. */
fun buildDrawerItems(apps: List<AppEntry>, frequent: List<AppEntry>): List<DrawerItem> {
    val items = ArrayList<DrawerItem>(apps.size + frequent.size + 2)
    if (frequent.isNotEmpty()) {
        items += DrawerItem.Header("Frecuentes")
        frequent.forEach { items += DrawerItem.App(it, "frecuentes") }
        items += DrawerItem.Header("Todas")
    }
    apps.forEach { items += DrawerItem.App(it, sectionOf(it.label)) }
    return items
}

@Composable
fun AppDrawer(
    state: HomeState,
    actions: LauncherActions,
    focusSearch: Boolean,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
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

    val results = remember(query, state.apps) { filterApps(state.apps, query) }
    val iconSize = state.settings.iconSize.dp.dp
    val scheme = MaterialTheme.colorScheme

    // Tirar hacia abajo con la lista arriba del todo cierra el cajón.
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

    Surface(Modifier.fillMaxSize(), color = scheme.surface, contentColor = scheme.onSurface) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 32.dp, end = 20.dp, top = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Todas las apps", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.apps.size} apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    focusRequester = focusRequester,
                    onGo = {
                        val first = results.firstOrNull()
                        when {
                            first != null -> { actions.launch(first); close() }
                            query.isNotBlank() -> { actions.webSearch(query.trim()); close() }
                        }
                    },
                    modifier = Modifier.widthIn(max = 640.dp).weight(1.4f),
                )
                Spacer(Modifier.width(16.dp))
                FilledTonalIconButton(onClick = close, modifier = Modifier.padding(4.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).nestedScroll(pullToClose)) {
                if (query.isBlank()) {
                    AllAppsGrid(state, actions, iconSize, onLaunched = close)
                } else {
                    SearchResults(results, query, state, actions, iconSize, onLaunched = close)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text("Buscar apps o en la web") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Borrar") }
            }
        },
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHigh,
            unfocusedContainerColor = scheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun AllAppsGrid(
    state: HomeState,
    actions: LauncherActions,
    iconSize: androidx.compose.ui.unit.Dp,
    onLaunched: () -> Unit,
) {
    val items = remember(state.apps, state.frequent) { buildDrawerItems(state.apps, state.frequent) }
    val letters = remember(state.apps) { state.apps.map { sectionOf(it.label) }.distinct() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (state.loadingApps) "Cargando apps…" else "No hay apps que mostrar",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
        }
        return
    }

    Row(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(iconSize + 52.dp),
            state = gridState,
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
                        item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.primary,
                        modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 2.dp),
                    )
                    is DrawerItem.App -> AppTile(
                        app = item.app,
                        iconSize = iconSize,
                        state = state,
                        actions = actions,
                        onLaunched = onLaunched,
                    )
                }
            }
        }
        if (letters.size > 3) {
            LetterRail(
                letters = letters,
                onLetter = { letter ->
                    val index = items.indexOfFirst { it is DrawerItem.App && it.section == letter }
                    if (index >= 0) scope.launch { gridState.scrollToItem(index) }
                },
                modifier = Modifier.width(40.dp).fillMaxHeight().padding(vertical = 8.dp),
            )
        }
    }
}

/** Índice A-Z: tocar o arrastrar salta a la letra. */
@Composable
private fun LetterRail(letters: List<String>, onLetter: (String) -> Unit, modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf<String?>(null) }
    val currentLetters by rememberUpdatedState(letters)
    val currentOnLetter by rememberUpdatedState(onLetter)
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.pointerInput(Unit) {
            fun pick(y: Float) {
                val list = currentLetters
                if (list.isEmpty() || size.height == 0) return
                val i = (y / size.height * list.size).toInt().coerceIn(0, list.lastIndex)
                if (active != list[i]) {
                    active = list[i]
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnLetter(list[i])
                }
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                pick(down.position.y)
                down.consume()
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    pick(change.position.y)
                    change.consume()
                }
                active = null
            }
        },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                letter,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (letter == active) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (letter == active) scheme.primary else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<AppEntry>,
    query: String,
    state: HomeState,
    actions: LauncherActions,
    iconSize: androidx.compose.ui.unit.Dp,
    onLaunched: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(end = 12.dp)) {
        FilledTonalButton(onClick = { actions.webSearch(query.trim()); onLaunched() }) {
            Icon(Icons.Rounded.Public, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Buscar «${query.trim()}» en la web")
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Ninguna app coincide con «${query.trim()}»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(iconSize + 52.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(results, key = { it.key }) { app ->
                    AppTile(app = app, iconSize = iconSize, state = state, actions = actions, onLaunched = onLaunched)
                }
            }
        }
    }
}
