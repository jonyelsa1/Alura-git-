@file:OptIn(ExperimentalTextApi::class)

package dev.lanzador.tablet.ui.styles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.data.weatherDescription
import dev.lanzador.tablet.ui.HomeNav
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.common.AppTarget
import dev.lanzador.tablet.ui.common.DesignCanvas
import dev.lanzador.tablet.ui.common.GlassAppIcon
import dev.lanzador.tablet.ui.common.GlassSurface
import dev.lanzador.tablet.ui.common.TileMenuContext
import dev.lanzador.tablet.ui.common.eventTime
import dev.lanzador.tablet.ui.common.formatAlarm
import dev.lanzador.tablet.ui.common.formatClock
import dev.lanzador.tablet.ui.common.formatDuration
import dev.lanzador.tablet.ui.common.formatLongDate
import dev.lanzador.tablet.ui.common.formatShortDay
import dev.lanzador.tablet.ui.common.is24Hour
import dev.lanzador.tablet.ui.common.mediaPosition
import dev.lanzador.tablet.ui.common.rememberNow
import dev.lanzador.tablet.ui.common.rememberTicker
import dev.lanzador.tablet.ui.common.weatherIcon
import dev.lanzador.tablet.ui.theme.Inter

private val Soft = Shadow(Color.Black.copy(alpha = 0.25f), Offset(0f, 2f), 6f)

private fun t(size: TextUnit, weight: Int = 500, alpha: Float = 1f, spacing: TextUnit = TextUnit.Unspecified) = TextStyle(
    fontFamily = Inter, fontSize = size, fontWeight = FontWeight(weight), color = Color.White.copy(alpha = alpha),
    letterSpacing = spacing, shadow = Soft,
)

@Composable
fun CristalHome(state: HomeState, actions: LauncherActions, nav: HomeNav, landscape: Boolean) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        if (landscape) {
            DesignCanvas(1280.dp, 744.dp) { CristalLandscape(state, actions, nav) }
        } else {
            DesignCanvas(800.dp, 1224.dp) { CristalPortrait(state, actions, nav) }
        }
    }
}

@Composable
private fun CristalLandscape(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(start = 54.dp, end = 52.dp, top = 26.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            CristalClock(state, actions, nav, Modifier.weight(1f), clockSize = 176.sp)
            Column(Modifier.width(466.dp)) {
                SearchGlass(actions, nav)
                Spacer(Modifier.height(16.dp))
                WeatherGlass(state, actions, nav, Modifier.fillMaxWidth().height(200.dp))
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        Row(Modifier.fillMaxWidth().height(252.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AgendaGlass(state, actions, Modifier.weight(340f).fillMaxHeight())
            MusicGlass(state, actions, Modifier.weight(342f).fillMaxHeight())
            Column(Modifier.weight(222f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BatteryGlass(state, actions, Modifier.fillMaxWidth().weight(1f))
                TimerGlass(state, actions, Modifier.fillMaxWidth().weight(1f))
            }
            FavoriteCluster(state, actions, nav, columns = 2, Modifier.weight(222f).fillMaxHeight())
        }
        Spacer(Modifier.weight(1.3f).heightIn(min = 18.dp))
        Dock(state, actions, nav, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun CristalPortrait(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CristalClock(state, actions, nav, Modifier.fillMaxWidth(), clockSize = 150.sp)
        SearchGlass(actions, nav)
        WeatherGlass(state, actions, nav, Modifier.fillMaxWidth().height(200.dp))
        Row(Modifier.fillMaxWidth().height(252.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AgendaGlass(state, actions, Modifier.weight(1f).fillMaxHeight())
            MusicGlass(state, actions, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BatteryGlass(state, actions, Modifier.weight(1f).fillMaxHeight())
            TimerGlass(state, actions, Modifier.weight(1f).fillMaxHeight())
        }
        FavoriteCluster(state, actions, nav, columns = 4, Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.weight(1f))
        Dock(state, actions, nav, Modifier.align(Alignment.CenterHorizontally), max = 6)
    }
}

@Composable
private fun CristalClock(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier, clockSize: TextUnit) {
    val now = rememberNow()
    val is24 = is24Hour()
    Column(modifier) {
        Text(
            formatClock(now, is24),
            modifier = Modifier.clickable(remember0(), indication = null, onClick = actions::openClock),
            style = TextStyle(
                fontFamily = Inter, fontWeight = FontWeight(200), fontSize = clockSize, lineHeight = clockSize * 0.98f,
                letterSpacing = (-8).sp,
                brush = Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.62f))),
                shadow = Shadow(Color(0x592A0A5C), Offset(0f, 6f), 22f),
            ),
        )
        Text(formatLongDate(now), style = t(25.sp, 500, 0.95f))
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val weather = state.weather
            when (weather) {
                is WeatherUi.Ready -> Chip(weatherIcon(weather.data.code, weather.data.isDay), "${weather.data.temperature}° ${weather.data.place}", actions::refreshWeather)
                WeatherUi.NotSet -> Chip(Icons.Rounded.LocationOn, "Elegir ciudad", nav.openSettings)
                else -> Unit
            }
            val next = state.nextEvent
            val alarm = state.nextAlarmMillis
            when {
                next != null -> Chip(Icons.Rounded.CalendarToday, "${next.title} · ${eventTime(next, is24)}") { actions.openCalendar(next) }
                alarm != null -> Chip(Icons.Rounded.Alarm, formatAlarm(alarm, is24), actions::openClock)
            }
        }
    }
}

@Composable
private fun remember0() = androidx.compose.runtime.remember { MutableInteractionSource() }

@Composable
private fun Chip(icon: ImageVector, text: String, onClick: () -> Unit) {
    GlassSurface(Modifier.height(40.dp), shape = RoundedCornerShape(20.dp), onClick = onClick, contentPadding = PaddingValues(start = 12.dp, end = 16.dp)) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = t(15.sp, 600), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchGlass(actions: LauncherActions, nav: HomeNav) {
    GlassSurface(
        Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(29.dp),
        onClick = { nav.openDrawer(true, null) }, onClickLabel = "Buscar",
        contentPadding = PaddingValues(start = 20.dp, end = 8.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, Modifier.size(23.dp))
            Spacer(Modifier.width(12.dp))
            Text("Buscar apps, ajustes o en la web", style = t(16.sp, 500, 0.85f), modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = actions::voiceSearch), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Mic, "Buscar por voz", Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun WeatherGlass(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val weather = state.weather
    GlassSurface(
        modifier,
        onClick = if (weather is WeatherUi.NotSet) nav.openSettings else actions::refreshWeather,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        when (weather) {
            is WeatherUi.Ready -> {
                val w = weather.data
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(w.place, style = t(17.sp, 600), maxLines = 1)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp))
                            }
                            Text(weatherDescription(w.code), style = t(14.sp, 500, 0.85f), modifier = Modifier.padding(top = 4.dp))
                            Text("Máx ${w.max}° · Mín ${w.min}°", style = t(14.sp, 500, 0.85f), modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(weatherIcon(w.code, w.isDay), null, Modifier.size(46.dp).padding(top = 6.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("${w.temperature}°", style = t(64.sp, 300, spacing = (-2).sp).copy(lineHeight = 64.sp))
                    }
                    val hours = w.hourly.drop(1).take(7)
                    if (hours.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.18f)))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            hours.forEach { h ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                                    Text("${h.hour} h", style = t(13.sp, 500))
                                    Icon(weatherIcon(h.code, h.isDay), null, Modifier.padding(vertical = 5.dp).size(22.dp))
                                    Text("${h.temperature}°", style = t(15.sp, 600))
                                }
                            }
                        }
                    }
                }
            }
            is WeatherUi.Loading -> CenterNote("Cargando el tiempo de ${weather.place}…")
            is WeatherUi.Error -> CenterNote("Sin datos del tiempo de ${weather.place}\nToca para reintentar")
            WeatherUi.NotSet -> CenterNote("El tiempo\nToca para elegir tu ciudad")
        }
    }
}

@Composable
private fun CenterNote(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = t(16.sp, 500, 0.9f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun WidgetHead(left: String, right: String = "") {
    Row(Modifier.fillMaxWidth()) {
        Text(left.uppercase(), style = t(12.sp, 700, 0.8f, 1.6.sp), modifier = Modifier.weight(1f))
        Text(right.uppercase(), style = t(12.sp, 700, 0.8f, 1.6.sp))
    }
}

@Composable
private fun AgendaGlass(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val now = rememberNow()
    val is24 = is24Hour()
    GlassSurface(modifier, onClick = { actions.openCalendar(null) }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp)) {
        Column(Modifier.fillMaxSize()) {
            WidgetHead("Hoy", formatShortDay(now))
            when (state.calendar) {
                CalendarUi.NoPermission -> {
                    Spacer(Modifier.weight(1f))
                    Text("Tus eventos del día aparecerán aquí", style = t(16.sp, 600))
                    Spacer(Modifier.height(12.dp))
                    PillButton("Permitir calendario") { actions.request(Permission.CALENDAR) }
                    Spacer(Modifier.weight(1f))
                }
                is CalendarUi.Ready -> {
                    val events = state.todayEvents.take(3)
                    if (events.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Text("Nada más por hoy", style = t(18.sp, 600))
                        Spacer(Modifier.weight(1f))
                    } else {
                        events.forEach { e ->
                            Row(Modifier.padding(top = 14.dp).clickable { actions.openCalendar(e) }) {
                                Box(Modifier.width(4.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(Color(e.color or 0xFF000000.toInt())))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(eventTime(e, is24), style = t(13.sp, 500, 0.8f))
                                    Text(e.title, style = t(17.sp, 600), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    state.tomorrowEvent?.let { e ->
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                        Text(
                            "Mañana · ${eventTime(e, is24)} ${e.title}", style = t(13.sp, 500, 0.75f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(text, style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A1460))) }
}

@Composable
private fun MusicGlass(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val media = state.media
    GlassSurface(modifier, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp)) {
        when {
            media != null -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { actions.openMediaApp(media) }) {
                    AlbumArt(media.art, 86.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(media.title, style = t(20.sp, 650), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(media.artist, style = t(14.sp, 500, 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                val tick = rememberTicker(media.playing)
                val pos = androidx.compose.runtime.remember(tick, media) { mediaPosition(media) }
                val frac = if (media.durationMs > 0) pos.toFloat() / media.durationMs else 0f
                ProgressLine(frac)
                Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                    Text(formatDuration(pos), style = t(12.sp, 500, 0.75f), modifier = Modifier.weight(1f))
                    if (media.durationMs > 0) Text(formatDuration(media.durationMs), style = t(12.sp, 500, 0.75f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.Rounded.SkipPrevious, "Anterior", 26.dp, actions::mediaPrevious)
                    Spacer(Modifier.width(26.dp))
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White).clickable(onClick = actions::mediaPlayPause), contentAlignment = Alignment.Center) {
                        Icon(if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (media.playing) "Pausa" else "Reproducir", tint = Color(0xFF2A1460), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(26.dp))
                    RoundIcon(Icons.Rounded.SkipNext, "Siguiente", 26.dp, actions::mediaNext)
                }
            }
            !state.notificationAccess -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) { AlbumArt(null, 64.dp); Spacer(Modifier.width(14.dp)); Text("Música", style = t(20.sp, 650)) }
                Spacer(Modifier.weight(1f))
                Text("Para ver y controlar lo que suena, activa el acceso a notificaciones.", style = t(14.sp, 500, 0.85f))
                Spacer(Modifier.height(12.dp))
                PillButton("Activar acceso") { actions.request(Permission.NOTIFICATIONS_ACCESS) }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) { AlbumArt(null, 86.dp); Spacer(Modifier.width(16.dp)); Column { Text("Nada sonando", style = t(20.sp, 650)); Text("Pon música en cualquier app", style = t(14.sp, 500, 0.8f)) } }
                Spacer(Modifier.weight(1f))
                ProgressLine(0f)
            }
        }
    }
}

@Composable
fun AlbumArt(art: androidx.compose.ui.graphics.ImageBitmap?, size: Dp, corner: Dp = size * 0.23f) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(
            Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF7C3AED), Color(0xFFEC4899))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, Modifier.size(size * 0.4f), tint = Color.White)
    }
}

@Composable
private fun ProgressLine(fraction: Float) {
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height / 2
        val stroke = 5.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        val x = size.width * fraction.coerceIn(0f, 1f)
        drawLine(Color.White, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
        drawCircle(Color.White, 6.5.dp.toPx(), Offset(x, y))
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size + 18.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, Modifier.size(size))
    }
}

@Composable
private fun Ring(fraction: Float, icon: ImageVector, label: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val s = 4.5.dp.toPx()
                drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(s), topLeft = Offset(s / 2, s / 2), size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s))
                drawArc(Color(0xFF7CFFB2), -90f, 360f * fraction.coerceIn(0f, 1f), false, style = Stroke(s, cap = StrokeCap.Round), topLeft = Offset(s / 2, s / 2), size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s))
            }
            Icon(icon, null, Modifier.size(22.dp))
        }
        Text(label, style = t(13.sp, 650), modifier = Modifier.padding(top = 5.dp))
        Text(caption, style = t(11.sp, 500, 0.7f))
    }
}

@Composable
private fun BatteryGlass(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val battery = state.battery
    GlassSurface(modifier, onClick = { actions.quick(QuickAction.BATTERY) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Ring((battery?.level ?: 0) / 100f, Icons.Rounded.TabletAndroid, "${battery?.level ?: "–"} %", if (battery?.charging == true) "cargando" else "batería")
            Ring(state.device.storageUsedPercent / 100f, Icons.Rounded.Storage, "${state.device.storageUsedPercent} %", "espacio")
        }
    }
}

@Composable
private fun TimerGlass(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val timer = state.timer
    val now = rememberTicker(timer.running)
    val remaining = timer.remainingAt(now)
    GlassSurface(modifier, contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HourglassEmpty, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enfoque", style = t(13.sp, 600, 0.85f))
                }
                Text(formatDuration(remaining), style = t(38.sp, 300, spacing = (-1).sp))
            }
            if (!timer.running && remaining < timer.durationMs) {
                Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = actions::timerReset), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Refresh, "Reiniciar", Modifier.size(20.dp))
                }
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))
                    .clickable(onClick = if (timer.running) actions::timerPause else actions::timerStart),
                contentAlignment = Alignment.Center,
            ) { Icon(if (timer.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (timer.running) "Pausar" else "Empezar", Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun FavoriteCluster(state: HomeState, actions: LauncherActions, nav: HomeNav, columns: Int, modifier: Modifier) {
    val rows = if (columns >= 4) 1 else 2
    val apps = state.favorites.take(rows * columns)
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (c in 0 until columns) {
                    val i = r * columns + c
                    val app = apps.getOrNull(i)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                        if (app != null) {
                            AppTarget(app, state, actions, shape = RoundedCornerShape(21.dp), menu = TileMenuContext(AppListKind.FAVORITES, i, state.favorites.size)) {
                                GlassAppIcon(app, 66.dp, badge = state.notifications[app.packageName]?.count ?: 0)
                            }
                            Text(app.label, style = t(14.sp, 600), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            GlassSurface(Modifier.size(66.dp), shape = RoundedCornerShape(21.dp), onClick = { nav.openDrawer(false, null) }) {
                                Icon(Icons.Rounded.Add, "Añadir favorito", Modifier.align(Alignment.Center).size(28.dp))
                            }
                            Text("Añadir", style = t(14.sp, 600, 0.7f), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dock(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier, max: Int = 8) {
    val apps = state.dock.take(max)
    GlassSurface(modifier.height(94.dp), shape = RoundedCornerShape(36.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            apps.forEachIndexed { i, app ->
                AppTarget(app, state, actions, shape = RoundedCornerShape(21.dp), menu = TileMenuContext(AppListKind.DOCK, i, apps.size)) {
                    GlassAppIcon(app, 66.dp, badge = state.notifications[app.packageName]?.count ?: 0)
                }
            }
            if (apps.isNotEmpty()) Box(Modifier.width(1.5.dp).height(46.dp).background(Color.White.copy(alpha = 0.3f)))
            GlassSurface(Modifier.size(66.dp), shape = RoundedCornerShape(21.dp), onClick = { nav.openDrawer(false, null) }, onClickLabel = "Todas las apps") {
                Icon(Icons.Rounded.Apps, "Todas las apps", Modifier.align(Alignment.Center).size(29.dp))
            }
        }
    }
}
