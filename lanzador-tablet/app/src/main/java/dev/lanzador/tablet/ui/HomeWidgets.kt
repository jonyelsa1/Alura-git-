package dev.lanzador.tablet.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.data.weatherDescription
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val CardShape = RoundedCornerShape(28.dp)

/** Tarjeta translúcida sobre el fondo de pantalla. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(20.dp),
            content = content,
        )
    }
}

@Composable
fun rememberNow(fixed: LocalDateTime?): LocalDateTime {
    if (fixed != null) return fixed
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            }
        }
    }
    return now
}

private fun greeting(hour: Int) = when (hour) {
    in 6..13 -> "Buenos días"
    in 14..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
fun ClockBlock(
    now: LocalDateTime,
    battery: BatteryState?,
    nextAlarmMillis: Long?,
    color: Color,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val is24 = remember(context) { DateFormat.is24HourFormat(context) }
    val time = now.format(DateTimeFormatter.ofPattern(if (is24) "H:mm" else "h:mm", locale))
    val amPm = if (is24) null else now.format(DateTimeFormatter.ofPattern("a", locale))
    val date = remember(now.toLocalDate(), locale) { formatSkeleton(now, locale, "EEEEdMMMM", "EEEE d MMMM") }
    val shadow = if (color.isLight()) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 16f) else null
    val clockSize = if (big) 124.sp else 92.sp

    Column(modifier) {
        Text(
            greeting(now.hour),
            style = MaterialTheme.typography.titleLarge.copy(shadow = shadow),
            color = color.copy(alpha = 0.9f),
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                time,
                style = TextStyle(
                    fontSize = clockSize,
                    lineHeight = clockSize * 1.04f,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-3).sp,
                    shadow = shadow,
                ),
                color = color,
            )
            if (amPm != null) {
                Text(
                    amPm,
                    style = MaterialTheme.typography.headlineSmall.copy(shadow = shadow),
                    color = color,
                    modifier = Modifier.padding(start = 10.dp, bottom = 20.dp),
                )
            }
        }
        Text(
            date,
            style = MaterialTheme.typography.headlineSmall.copy(shadow = shadow, fontWeight = FontWeight.Normal),
            color = color,
        )
        val alarmText = nextAlarmMillis?.let {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            formatSkeleton(dt, locale, if (is24) "EEEHmm" else "EEEhmma", "EEE H:mm")
        }
        if (battery != null || alarmText != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (battery != null) {
                    StatusChip(
                        icon = if (battery.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                        text = "${battery.level} %" + if (battery.charging) " · cargando" else "",
                        color = color,
                        shadow = shadow,
                    )
                }
                if (alarmText != null) {
                    StatusChip(Icons.Rounded.Alarm, alarmText, color, shadow)
                }
            }
        }
    }
}

private fun formatSkeleton(dt: LocalDateTime, locale: Locale, skeleton: String, fallback: String): String {
    val text = runCatching {
        dt.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }.getOrElse { dt.format(DateTimeFormatter.ofPattern(fallback, locale)) }
    return text.replaceFirstChar { it.titlecase(locale) }
}

private fun Color.isLight(): Boolean = (red + green + blue) / 3f > 0.5f

@Composable
private fun StatusChip(icon: ImageVector, text: String, color: Color, shadow: Shadow?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.titleMedium.copy(shadow = shadow), color = color.copy(alpha = 0.9f))
    }
}

fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> Icons.Rounded.WbCloudy
    3 -> Icons.Rounded.Cloud
    45, 48 -> Icons.Rounded.Dehaze
    in 51..57 -> Icons.Rounded.Grain
    in 61..67, in 80..82 -> Icons.Rounded.Umbrella
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.FlashOn
    else -> Icons.Rounded.Cloud
}

@Composable
fun WeatherCard(weather: WeatherUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier, onClick = onClick) {
        when (weather) {
            WeatherUi.NotSet -> Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.LocationOn)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Tiempo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Toca para elegir tu ciudad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            is WeatherUi.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(16.dp))
                Text("Cargando el tiempo de ${weather.place}…", style = MaterialTheme.typography.bodyLarge)
            }
            is WeatherUi.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Cloud)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Sin datos del tiempo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${weather.place} · toca para reintentar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            is WeatherUi.Ready -> BoxWithConstraints {
                val w = weather.data
                val icon = @Composable {
                    Icon(
                        weatherIcon(w.code, w.isDay),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(52.dp),
                    )
                }
                val details = "${weatherDescription(w.code)} · ${w.max}° / ${w.min}°"
                if (maxWidth >= 420.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon()
                        Spacer(Modifier.width(16.dp))
                        Text("${w.temperature}°", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.place, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            icon()
                            Spacer(Modifier.width(14.dp))
                            Text("${w.temperature}°", style = MaterialTheme.typography.displaySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(w.place, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, size: Int = 44) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun NoteCard(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Nota rápida", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        if (text.isBlank()) {
            Text(
                "Toca para apuntar algo: la lista de la compra, un recado, una idea.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
        } else {
            Text(text, style = MaterialTheme.typography.bodyLarge, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Editor de la nota en un panel arriba, para que el teclado no lo tape. */
@Composable
fun NoteEditor(visible: Boolean, initial: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = 0.96f), exit = fadeOut()) {
        BackHandler(onBack = onClose)
        var text by remember { mutableStateOf(initial) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
        val scheme = MaterialTheme.colorScheme
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                shape = CardShape,
                color = scheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = scheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Nota rápida", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { text = ""; onChange("") }, enabled = text.isNotEmpty()) { Text("Borrar") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onClose) { Text("Listo") }
                    }
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it; onChange(it) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).focusRequester(focus),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp, color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) {
                                    Text(
                                        "Escribe aquí…",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.9f),
        contentColor = scheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = scheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Text("Buscar apps o en la web", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RoundIconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.9f),
        contentColor = scheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = scheme.onSurfaceVariant)
        }
    }
}

val QuickAction.icon: ImageVector
    get() = when (this) {
        QuickAction.WIFI -> Icons.Rounded.Wifi
        QuickAction.BLUETOOTH -> Icons.Rounded.Bluetooth
        QuickAction.VOLUME -> @Suppress("DEPRECATION") Icons.Rounded.VolumeUp
        QuickAction.DISPLAY -> Icons.Rounded.BrightnessMedium
        QuickAction.BATTERY -> Icons.Rounded.BatteryFull
        QuickAction.SETTINGS -> Icons.Rounded.Settings
    }

@Composable
fun QuickSettingsRow(onAction: (QuickAction) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction.entries.forEach { action ->
            Surface(
                onClick = { onAction(action) },
                modifier = Modifier.weight(1f).height(88.dp),
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainer.copy(alpha = 0.84f),
                contentColor = scheme.onSurface,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    IconBadge(action.icon, size = 38)
                    Spacer(Modifier.height(6.dp))
                    Text(action.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
