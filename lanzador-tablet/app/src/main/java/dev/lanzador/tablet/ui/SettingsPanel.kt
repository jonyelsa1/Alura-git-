package dev.lanzador.tablet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lanzador.tablet.data.IconSize
import dev.lanzador.tablet.data.ThemeMode
import dev.lanzador.tablet.data.WeatherPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Panel lateral derecho con los ajustes del launcher. */
@Composable
fun SettingsPanel(
    visible: Boolean,
    state: HomeState,
    actions: LauncherActions,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            BackHandler(onBack = onClose)
            Surface(
                modifier = Modifier.fillMaxHeight().widthIn(max = 480.dp).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                SettingsContent(state, actions, onClose)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(state: HomeState, actions: LauncherActions, onClose: () -> Unit) {
    val settings = state.settings
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ajustes del lanzador", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar") }
        }

        Section("Apariencia")
        Label("Tema")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.theme == mode,
                    onClick = { actions.updateSettings { it.copy(theme = mode) } },
                    label = { Text(mode.label) },
                )
            }
        }
        Label("Tamaño de los iconos")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconSize.entries.forEach { size ->
                FilterChip(
                    selected = settings.iconSize == size,
                    onClick = { actions.updateSettings { it.copy(iconSize = size) } },
                    label = { Text(size.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SwitchRow("Tarjeta del tiempo", settings.showWeather) { v -> actions.updateSettings { it.copy(showWeather = v) } }
        SwitchRow("Nota rápida", settings.showNote) { v -> actions.updateSettings { it.copy(showNote = v) } }
        SwitchRow("Accesos rápidos (Wi-Fi, sonido…)", settings.showQuickSettings) { v ->
            actions.updateSettings { it.copy(showQuickSettings = v) }
        }

        Section("Tiempo")
        WeatherPlacePicker(state, actions)

        Section("Apps ocultas")
        if (state.hidden.isEmpty()) {
            Text(
                "Ninguna. Para ocultar una, mantenla pulsada en «Todas las apps» y elige «Ocultar del cajón».",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        } else {
            state.hidden.forEach { app ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.setHidden(app, false) }) { Text("Mostrar") }
                }
            }
        }

        Section("Sistema")
        ActionRow(Icons.Rounded.Home, "Pantalla de inicio predeterminada", "Elige Lanzador como launcher", actions::openDefaultLauncherSettings)
        ActionRow(Icons.Rounded.Wallpaper, "Fondo de pantalla", "Los colores del lanzador se adaptan al fondo", actions::openWallpaperPicker)

        Section("Gestos")
        Text(
            "Desliza hacia arriba: todas las apps.\n" +
                "Desliza hacia abajo: notificaciones.\n" +
                "Mantén pulsado el fondo: estos ajustes.\n" +
                "Mantén pulsada una app: accesos directos, favoritos, dock y más.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text("Lanzador 1.0", style = MaterialTheme.typography.labelMedium, color = scheme.outline)
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
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = scheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(
            place?.let { listOf(it.name, it.detail).filter(String::isNotBlank).joinToString(", ") } ?: "Sin ubicación",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (place != null) {
            TextButton(onClick = { actions.updateSettings { it.copy(weatherPlace = null) } }) { Text("Quitar") }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Ciudad") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
        )
        Spacer(Modifier.width(10.dp))
        Button(onClick = search, enabled = query.isNotBlank() && !searching) {
            if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
            else Text("Buscar")
        }
    }
    message?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.error, modifier = Modifier.padding(top = 8.dp))
    }
    results.forEach { result ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    actions.updateSettings { it.copy(weatherPlace = result) }
                    results = emptyList()
                    query = ""
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column {
                Text(result.name, style = MaterialTheme.typography.bodyLarge)
                if (result.detail.isNotBlank()) {
                    Text(result.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
    Text(
        "Datos de Open-Meteo.com (CC BY 4.0). Se actualizan cada 20 minutos como mucho.",
        style = MaterialTheme.typography.bodySmall,
        color = scheme.outline,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, size = 40)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
