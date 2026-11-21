package dev.lanzador.tablet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.R
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.LauncherStyle
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.data.WallpaperMode
import dev.lanzador.tablet.data.WeatherPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun thumbnailFor(style: LauncherStyle): Int = when (style) {
    LauncherStyle.CRISTAL -> R.drawable.estilo_cristal
    LauncherStyle.EXPRESIVO -> R.drawable.estilo_expresivo
    LauncherStyle.BENTO -> R.drawable.estilo_bento
    LauncherStyle.ENFOQUE -> R.drawable.estilo_enfoque
}

private val noRipple: MutableInteractionSource
    @Composable get() = remember { MutableInteractionSource() }

@Composable
private fun StyleCard(style: LauncherStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, big: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(6.dp)) {
        Box {
            Image(
                painterResource(thumbnailFor(style)), contentDescription = style.label, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(18.dp))
                    .border(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(18.dp)),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle, "Elegido", tint = scheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(scheme.surface),
                )
            }
        }
        Text(style.label, style = MaterialTheme.typography.titleMedium.copy(fontSize = if (big) 20.sp else 16.sp), modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        Text(style.description, style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant), modifier = Modifier.padding(start = 4.dp), maxLines = 2)
    }
}

/** Selector rápido de estilo (mantener pulsado el fondo). */
@Composable
fun StylePicker(visible: Boolean, state: HomeState, actions: LauncherActions, onMore: () -> Unit, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = 0.96f), exit = fadeOut()) {
        BackHandler(onBack = onClose)
        val scheme = MaterialTheme.colorScheme
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(noRipple, null, onClick = onClose)
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                Modifier.widthIn(max = 1040.dp).fillMaxWidth().clickable(noRipple, null) {},
                shape = RoundedCornerShape(32.dp), color = scheme.surfaceContainerHigh, contentColor = scheme.onSurface,
            ) {
                Column(Modifier.padding(28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Estilo de hoy", style = MaterialTheme.typography.headlineSmall)
                            Text("Toca uno para cambiar al momento", style = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurfaceVariant))
                        }
                        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar") }
                    }
                    Spacer(Modifier.height(18.dp))
                    BoxWithConstraints {
                        val perRow = if (maxWidth > 700.dp) 4 else 2
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LauncherStyle.entries.chunked(perRow).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { s ->
                                        StyleCard(s, s == state.style, { actions.setStyle(s); onClose() }, Modifier.weight(1f), big = true)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(state.settings.dailyRotation, { v -> actions.updateSettings { it.copy(dailyRotation = v) } })
                        Spacer(Modifier.width(12.dp))
                        Text("Un estilo distinto cada día", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = onMore) { Text("Más ajustes") }
                    }
                }
            }
        }
    }
}

/** Añadir una tarea a «Para hoy». */
@Composable
fun TaskEditor(visible: Boolean, actions: LauncherActions, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = 0.96f), exit = fadeOut()) {
        BackHandler(onBack = onClose)
        var text by remember { mutableStateOf("") }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
        val scheme = MaterialTheme.colorScheme
        val submit = { if (text.isNotBlank()) actions.addTask(text); onClose() }
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(noRipple, null, onClick = onClose)
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(top = 40.dp).clickable(noRipple, null) {},
                shape = RoundedCornerShape(28.dp), color = scheme.surfaceContainerHigh, contentColor = scheme.onSurface,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Nueva tarea", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(14.dp))
                    BasicTextField(
                        text, { text = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerHighest)
                            .padding(horizontal = 18.dp, vertical = 16.dp).focusRequester(focus),
                        decorationBox = { inner ->
                            Box { if (text.isEmpty()) Text("Por ejemplo: comprar pan", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, color = scheme.onSurfaceVariant)); inner() }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClose) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = submit, enabled = text.isNotBlank()) { Text("Añadir") }
                    }
                }
            }
        }
    }
}

/** Panel lateral derecho con todos los ajustes. */
@Composable
fun SettingsPanel(visible: Boolean, state: HomeState, actions: LauncherActions, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(noRipple, null, onClick = onClose))
        }
        AnimatedVisibility(
            visible, modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            BackHandler(onBack = onClose)
            Surface(
                Modifier.fillMaxHeight().widthIn(max = 520.dp).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface,
            ) { SettingsContent(state, actions, onClose) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(state: HomeState, actions: LauncherActions, onClose: () -> Unit) {
    val s = state.settings
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
                .imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Personalizar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar") }
            }

            Section("Estilo")
            LauncherStyle.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { st -> StyleCard(st, st == state.style, { actions.setStyle(st) }, Modifier.weight(1f)) }
                }
            }
            SwitchRow("Un estilo distinto cada día", "Cristal, Expresivo, Bento y Enfoque, por turnos", s.dailyRotation) { v ->
                actions.updateSettings { it.copy(dailyRotation = v) }
            }

            Section("Fondo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperMode.entries.forEach { m ->
                    FilterChip(selected = s.wallpaperMode == m, onClick = { if (m == WallpaperMode.PHOTO && s.wallpaperPhoto == null) actions.pickWallpaperPhoto() else actions.setWallpaperMode(m) }, label = { Text(m.label) })
                }
            }
            Text(
                when (s.wallpaperMode) {
                    WallpaperMode.STYLE -> "Cada estilo trae su fondo. Los cristales lo desenfocan."
                    WallpaperMode.PHOTO -> "Tu foto de fondo, con los cristales desenfocándola."
                    WallpaperMode.SYSTEM -> "El fondo de Android. Los cristales se ven translúcidos, sin desenfoque."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant), modifier = Modifier.padding(top = 8.dp),
            )
            if (s.wallpaperMode == WallpaperMode.PHOTO) {
                TextButton(onClick = actions::pickWallpaperPhoto) { Text("Elegir otra foto") }
            }

            Section("Tiempo")
            WeatherPlacePicker(state, actions)

            Section("Temporizador de enfoque")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 25, 45, 60).forEach { m ->
                    FilterChip(selected = s.timerMinutes == m, onClick = { actions.timerMinutes(m) }, label = { Text("$m min") })
                }
            }

            Section("Permisos")
            PermissionRow("Calendario", "Eventos de hoy y mañana", state.calendar !is CalendarUi.NoPermission) { actions.request(Permission.CALENDAR) }
            PermissionRow("Fotos", "Recuerdos de hace un año (Bento)", state.photos !is PhotoUi.NoPermission) { actions.request(Permission.PHOTOS) }
            if (state.mediaSupported) {
                PermissionRow("Música y avisos", "Lo que suena y contadores de notificaciones", state.notificationAccess) { actions.request(Permission.NOTIFICATIONS_ACCESS) }
                Text(
                    "Si «Música y avisos» aparece bloqueado: Información de la app ▸ menú ⋮ ▸ Permitir ajustes restringidos, y vuelve a intentarlo.",
                    style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant), modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Text(
                    "Versión ligera: sin lector de notificaciones, así que no muestra la música que suena ni los contadores de avisos. Es lo que permite instalarla sin que Play Protect la bloquee.",
                    style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant), modifier = Modifier.padding(top = 6.dp),
                )
            }

            Section("Apps ocultas")
            if (state.hidden.isEmpty()) {
                Text("Ninguna. Para ocultar una, mantenla pulsada en el cajón.", style = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurfaceVariant))
            } else {
                state.hidden.forEach { app ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(app.icon, null, Modifier.size(36.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { actions.setHidden(app, false) }) { Text("Mostrar") }
                    }
                }
            }

            Section("Sistema")
            ActionRow(Icons.Rounded.Home, "Pantalla de inicio predeterminada", "Elige Lanzador como launcher", actions::openDefaultLauncherSettings)
            ActionRow(Icons.Rounded.Wallpaper, "Fondo de pantalla de Android", "Se usa con «Fondo: del sistema»", actions::openWallpaperPicker)

            Section("Gestos")
            Text(
                "Desliza hacia arriba: todas las apps.\nDesliza hacia abajo: notificaciones.\nMantén pulsado el fondo: cambiar de estilo.\nMantén pulsada una app: accesos directos, favoritos, dock y más.\nMantén pulsada una tarea: borrarla.",
                style = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurfaceVariant),
            )
            Spacer(Modifier.height(24.dp))
            Text("Lanzador ${dev.lanzador.tablet.BuildConfig.VERSION_NAME} · Tiempo de Open-Meteo.com (CC BY 4.0)", style = MaterialTheme.typography.labelMedium.copy(color = scheme.outline))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary))
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        if (granted) Text("Activado", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary))
        else FilledTonalButton(onClick = onRequest) { Text("Permitir") }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun WeatherPlacePicker(state: HomeState, actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    val place = state.settings.weatherPlace
    val search: () -> Unit = {
        if (query.isNotBlank() && !searching) {
            scope.launch {
                searching = true
                message = null
                results = try {
                    actions.searchPlaces(query).also { if (it.isEmpty()) message = "No encontré «$query»" }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = "Sin conexión. Prueba de nuevo."
                    emptyList()
                }
                searching = false
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.LocationOn, null, tint = scheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(place?.let { listOf(it.name, it.detail).filter(String::isNotBlank).joinToString(", ") } ?: "Sin ubicación", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (place != null) TextButton(onClick = { actions.updateSettings { it.copy(weatherPlace = null) } }) { Text("Quitar") }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true,
            placeholder = { Text("Ciudad") }, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }),
        )
        Spacer(Modifier.width(10.dp))
        Button(onClick = search, enabled = query.isNotBlank() && !searching) {
            if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = scheme.onPrimary) else Text("Buscar")
        }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(color = scheme.error), modifier = Modifier.padding(top = 8.dp)) }
    results.forEach { r ->
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(14.dp))
                .clickable { actions.updateSettings { it.copy(weatherPlace = r) }; results = emptyList(); query = "" }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(r.name, style = MaterialTheme.typography.bodyLarge)
            if (r.detail.isNotBlank()) Text(r.detail, style = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurfaceVariant))
        }
    }
}
