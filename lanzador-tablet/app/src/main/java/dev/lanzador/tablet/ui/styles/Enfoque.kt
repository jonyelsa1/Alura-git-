package dev.lanzador.tablet.ui.styles

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.data.AppEntry
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.CalendarUi
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.data.weatherDescription
import dev.lanzador.tablet.ui.HomeNav
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.common.AppGlyph
import dev.lanzador.tablet.ui.common.AppTarget
import dev.lanzador.tablet.ui.common.DesignCanvas
import dev.lanzador.tablet.ui.common.widgetClick
import dev.lanzador.tablet.ui.common.LetterRail
import dev.lanzador.tablet.ui.common.TileMenuContext
import dev.lanzador.tablet.ui.common.clockHours
import dev.lanzador.tablet.ui.common.clockMinutes
import dev.lanzador.tablet.ui.common.countdown
import dev.lanzador.tablet.ui.common.eventTime
import dev.lanzador.tablet.ui.common.formatAlarm
import dev.lanzador.tablet.ui.common.formatMonthShort
import dev.lanzador.tablet.ui.common.formatShortDay
import dev.lanzador.tablet.ui.common.is24Hour
import dev.lanzador.tablet.ui.common.rememberNow
import dev.lanzador.tablet.ui.common.weatherIcon
import dev.lanzador.tablet.ui.theme.DotoRound
import dev.lanzador.tablet.ui.theme.SpaceGrotesk
import java.time.ZoneId
import kotlin.math.abs

private val Red = Color(0xFFE5302A)
private val G1 = Color(0xFF8B8B8B)
private val G2 = Color(0xFF4A4A4A)
private val Line = Color(0xFF1F1F1F)

private fun s(size: TextUnit, weight: Int = 500, color: Color = Color(0xFFF2F2F2), spacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = SpaceGrotesk, fontSize = size, fontWeight = FontWeight(weight), color = color, letterSpacing = spacing)

val AlphabetLetters: List<String> = ('A'..'Z').map { it.toString() }

@Composable
fun EnfoqueHome(state: HomeState, actions: LauncherActions, nav: HomeNav, landscape: Boolean) {
    CompositionLocalProvider(LocalContentColor provides Color(0xFFF2F2F2)) {
        if (landscape) DesignCanvas(1280.dp, 744.dp) { EnfoqueLandscape(state, actions, nav) }
        else DesignCanvas(800.dp, 1224.dp) { EnfoquePortrait(state, actions, nav) }
    }
}

@Composable
private fun EnfoqueLandscape(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Row(Modifier.fillMaxSize().padding(start = 60.dp, end = 22.dp, top = 36.dp, bottom = 20.dp)) {
        Column(Modifier.width(590.dp).fillMaxHeight()) {
            DayLine()
            DotClock(actions)
            Spacer(Modifier.height(30.dp))
            InfoRows(state, actions, nav)
            Spacer(Modifier.weight(1f))
            SearchField(nav)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.widthIn(max = 470.dp).fillMaxHeight()) {
            AppList(state, actions, nav, Modifier.fillMaxWidth())
            Hints(nav, Modifier.align(Alignment.BottomStart))
        }
        Spacer(Modifier.width(24.dp))
        Rail(state, nav, Modifier.width(34.dp).fillMaxHeight().padding(vertical = 14.dp))
    }
}

@Composable
private fun EnfoquePortrait(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Row(Modifier.fillMaxSize().padding(start = 44.dp, end = 18.dp, top = 30.dp, bottom = 20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            DayLine()
            DotClock(actions)
            Spacer(Modifier.height(20.dp))
            InfoRows(state, actions, nav)
            Spacer(Modifier.height(28.dp))
            AppList(state, actions, nav, Modifier.fillMaxWidth().weight(1f))
            SearchField(nav)
            Hints(nav, Modifier.padding(top = 14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Rail(state, nav, Modifier.width(34.dp).fillMaxHeight().padding(vertical = 14.dp))
    }
}

@Composable
private fun DayLine() {
    val now = rememberNow()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Red))
        Spacer(Modifier.width(12.dp))
        Text("${formatShortDay(now)} ${formatMonthShort(now)}".uppercase(), style = s(16.sp, 500, G1, 4.sp))
    }
}

@Composable
private fun DotClock(actions: LauncherActions) {
    val now = rememberNow()
    val is24 = is24Hour()
    val clock = TextStyle(fontFamily = DotoRound, fontWeight = FontWeight.Black, fontSize = 206.sp, lineHeight = 190.sp, letterSpacing = (-4).sp, color = Color.White)
    Row(
        Modifier.padding(top = 10.dp).offset(x = (-10).dp).clip(RoundedCornerShape(16.dp)).widgetClick(onClick = actions::openClock),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(clockHours(now, is24), style = clock)
        Column(Modifier.padding(start = 14.dp, end = 22.dp), verticalArrangement = Arrangement.spacedBy(46.dp)) {
            Box(Modifier.size(21.dp).clip(CircleShape).background(Red))
            Box(Modifier.size(21.dp).clip(CircleShape).background(Red))
        }
        Text(clockMinutes(now), style = clock)
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    end: String = "",
    // El contenido extra va antes que onClick a propósito: así una lambda final
    // siempre es la acción al tocar y nunca se ejecuta al dibujar la fila.
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(58.dp).then(if (onClick != null) Modifier.widgetClick(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = G1)
            Spacer(Modifier.width(16.dp))
            Text(text.uppercase(), style = s(17.sp, 500, spacing = 1.2.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (end.isNotEmpty()) Text(end.uppercase(), style = s(17.sp, 500, G1, 1.2.sp), maxLines = 1)
            trailing?.invoke()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

@Composable
private fun InfoRows(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    val now = rememberNow()
    val is24 = is24Hour()
    val millisNow = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        when (val w = state.weather) {
            is WeatherUi.Ready -> InfoRow(weatherIcon(w.data.code, w.data.isDay), "${w.data.temperature}° · ${weatherDescription(w.data.code)}", w.data.place, onClick = actions::refreshWeather)
            WeatherUi.NotSet -> InfoRow(Icons.Rounded.WbCloudy, "Tiempo", "Elegir ciudad", onClick = nav.openSettings)
            is WeatherUi.Loading -> InfoRow(Icons.Rounded.WbCloudy, "Cargando tiempo", w.place)
            is WeatherUi.Error -> InfoRow(Icons.Rounded.WbCloudy, "Sin datos del tiempo", w.place, onClick = actions::refreshWeather)
        }
        val next = state.nextEvent
        val alarm = state.nextAlarmMillis
        when {
            next != null -> InfoRow(Icons.Rounded.CalendarToday, "${eventTime(next, is24)} · ${next.title}", if (!next.allDay && next.begin > millisNow) countdown(millisNow, next.begin) else "", onClick = { actions.openCalendar(next) })
            state.calendar is CalendarUi.NoPermission -> InfoRow(Icons.Rounded.CalendarToday, "Calendario", "Permitir", onClick = { actions.request(Permission.CALENDAR) })
            alarm != null -> InfoRow(Icons.Rounded.Alarm, "Alarma · ${formatAlarm(alarm, is24)}", countdown(millisNow, alarm), onClick = actions::openClock)
            else -> InfoRow(Icons.Rounded.CalendarToday, "Sin eventos hoy", "", onClick = { actions.openCalendar(null) })
        }
        val level = state.battery?.level ?: 0
        InfoRow(Icons.Rounded.BatteryStd, "$level %" + if (state.battery?.charging == true) " · cargando" else "", onClick = { actions.quick(QuickAction.BATTERY) }, trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(10) { i -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (i < (level + 5) / 10) Color.White else G2)) }
            }
        })
        val media = state.media
        when {
            media != null -> InfoRow(Icons.Rounded.MusicNote, media.title, media.artist, onClick = actions::mediaPlayPause, trailing = {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (media.playing) Red else G2))
            })
            !state.mediaSupported -> InfoRow(Icons.Rounded.MusicNote, "Música", "Abrir", onClick = actions::openMusicApp)
            !state.notificationAccess -> InfoRow(Icons.Rounded.MusicNote, "Música y avisos", "Activar", onClick = { actions.request(Permission.NOTIFICATIONS_ACCESS) })
            else -> InfoRow(Icons.Rounded.MusicNote, "Nada sonando")
        }
    }
}

@Composable
private fun SearchField(nav: HomeNav) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF111111))
            .widgetClick { nav.openDrawer(true, null) }.padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, Modifier.size(22.dp), tint = Color.White)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(11.dp).height(24.dp).background(Red))
        Spacer(Modifier.width(8.dp))
        Text("Escribe: apps, contactos, web…", style = s(19.sp, 400, G1), modifier = Modifier.weight(1f), maxLines = 1)
        Text("ABC", style = s(13.sp, 500, G1, 1.sp))
    }
}

@Composable
private fun AppList(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val apps = state.favorites.take(9)
    var pressed by remember { mutableIntStateOf(-1) }
    var previews = 0
    Column(modifier) {
        if (apps.isEmpty()) {
            Text("Sin favoritos.\nDesliza hacia arriba, mantén pulsada una app y elige «Añadir a favoritos».", style = s(18.sp, 400, G1))
        }
        apps.forEachIndexed { i, app ->
            val note = state.notifications[app.packageName]
            val showPreview = note != null && !note.text.isNullOrBlank() && previews < 2
            if (showPreview) previews++
            ListItem(app, i, pressed, state, actions, onPress = { down -> pressed = if (down) i else if (pressed == i) -1 else pressed })
            if (showPreview && note != null) {
                Text(
                    listOfNotNull(note.title, note.text).joinToString(": "),
                    style = s(15.sp, 400, G1), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 42.dp, bottom = 8.dp).offset(y = (-6).dp),
                )
            }
        }
    }
}

@Composable
private fun ListItem(app: AppEntry, index: Int, pressed: Int, state: HomeState, actions: LauncherActions, onPress: (Boolean) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    LaunchedEffect(isPressed) { onPress(isPressed) }
    val d = if (pressed < 0) 99 else abs(index - pressed)
    val shift by animateDpAsState(when (d) { 0 -> (-34).dp; 1 -> (-14).dp; 2 -> (-6).dp; else -> 0.dp }, label = "shift")
    val size by animateFloatAsState(if (d == 0) 50f else 36f, label = "size")
    AppTarget(
        app, state, actions, shape = RoundedCornerShape(14.dp), interactionSource = source,
        menu = TileMenuContext(AppListKind.FAVORITES, index, state.favorites.size),
        modifier = Modifier.offset(x = shift),
    ) {
        Row(Modifier.height((size + 28).dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                AppGlyph(app, 44.dp, if (d == 0) Red else G1, monochromeFallback = true)
            }
            Spacer(Modifier.width(16.dp))
            Text(app.label, style = s(size.sp, 500, if (d == 0) Color.White else Color(0xFFE8E8E8), (-0.5).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val count = state.notifications[app.packageName]?.count ?: 0
            if (count > 0) {
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(26.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) {
                    Text(if (count > 9) "9+" else "$count", style = s(14.sp, 700, Color.White))
                }
            }
        }
    }
}

@Composable
private fun Hints(nav: HomeNav, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowUpward, null, Modifier.size(15.dp), tint = G2)
            Spacer(Modifier.width(8.dp))
            Text("TODAS LAS APPS", style = s(13.sp, 500, G2, 1.5.sp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowDownward, null, Modifier.size(15.dp), tint = G2)
            Spacer(Modifier.width(8.dp))
            Text("AVISOS", style = s(13.sp, 500, G2, 1.5.sp))
        }
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).widgetClick(onClickLabel = "Cambiar estilo", onClick = nav.openStylePicker).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Palette, "Cambiar estilo", Modifier.size(16.dp), tint = Red)
            Spacer(Modifier.width(8.dp))
            Text("ESTILO", style = s(13.sp, 600, Color(0xFFBDBDBD), 1.5.sp))
        }
    }
}

@Composable
private fun Rail(state: HomeState, nav: HomeNav, modifier: Modifier) {
    val letters = remember(state.apps) {
        val present = state.apps.map { dev.lanzador.tablet.ui.normalizeForSearch(it.label).firstOrNull()?.uppercaseChar() }.toSet()
        AlphabetLetters.filter { it[0] in present }.ifEmpty { AlphabetLetters }
    }
    LetterRail(
        letters, modifier, idle = G2, active = Red,
        style = s(13.sp, 500), waveWidth = 30f,
        onRelease = { letter -> nav.openDrawer(false, letter) },
    )
}
