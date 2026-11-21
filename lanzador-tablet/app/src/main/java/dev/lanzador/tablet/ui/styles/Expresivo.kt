package dev.lanzador.tablet.ui.styles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.TaskItem
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.data.weatherDescription
import dev.lanzador.tablet.ui.HomeNav
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.common.AppTarget
import dev.lanzador.tablet.ui.common.ArchShape
import dev.lanzador.tablet.ui.common.DesignCanvas
import dev.lanzador.tablet.ui.common.PolarShape
import dev.lanzador.tablet.ui.common.ShapedAppIcon
import dev.lanzador.tablet.ui.common.TileMenuContext
import dev.lanzador.tablet.ui.common.clockHours
import dev.lanzador.tablet.ui.common.clockMinutes
import dev.lanzador.tablet.ui.common.eventTime
import dev.lanzador.tablet.ui.common.formatAlarm
import dev.lanzador.tablet.ui.common.formatDayMonth
import dev.lanzador.tablet.ui.common.formatMonthShort
import dev.lanzador.tablet.ui.common.formatWeekday
import dev.lanzador.tablet.ui.common.is24Hour
import dev.lanzador.tablet.ui.common.mediaPosition
import dev.lanzador.tablet.ui.common.rememberNow
import dev.lanzador.tablet.ui.common.rememberTicker
import dev.lanzador.tablet.ui.common.weatherIcon
import dev.lanzador.tablet.ui.theme.RobotoFlex
import dev.lanzador.tablet.ui.theme.RobotoFlexWide
import kotlin.math.sin

private val Ink = Color(0xFF1D1B20)
private val Lila = Color(0xFFD6C3FF)
private val LilaDeep = Color(0xFF2A0E6B)
private val Violet = Color(0xFF6F43E0)
private val Cream = Color(0xFFFFF8F1)

private fun e(size: TextUnit, weight: Int = 500, color: Color = Ink, spacing: TextUnit = TextUnit.Unspecified, wide: Boolean = false) = TextStyle(
    fontFamily = if (wide) RobotoFlexWide else RobotoFlex, fontSize = size, fontWeight = FontWeight(weight), color = color, letterSpacing = spacing,
)

/** Formas y colores de los iconos, en el orden de la cuadrícula. */
private val ShapeCycle: List<Shape> = listOf(
    CircleShape, PolarShape(4, 0.16f), PolarShape(9, 0.09f), RoundedCornerShape(34), ArchShape,
    PolarShape(8, 0.08f), PolarShape(5, 0.11f), PolarShape(6, 0.09f), RoundedCornerShape(34),
)
private val Pastels = listOf(
    Color(0xFFC4EED0) to Color(0xFF0B3D1D), Color(0xFFD3E3FD) to Color(0xFF0B2B5E), Color(0xFFFFB899) to Color(0xFF5C1A00),
    Color(0xFFE9DDFF) to Color(0xFF2A0E6B), Color(0xFFFFE08A) to Color(0xFF3D2E00), Color(0xFFFFC2D6) to Color(0xFF5B1131),
    Color(0xFFFFDAD6) to Color(0xFF690005), Color(0xFFC2F0E6) to Color(0xFF003731), Color(0xFFE3E1EC) to Color(0xFF1B1B21),
)

@Composable
fun ExpresivoHome(state: HomeState, actions: LauncherActions, nav: HomeNav, landscape: Boolean) {
    CompositionLocalProvider(LocalContentColor provides Ink) {
        if (landscape) DesignCanvas(1280.dp, 744.dp) { ExpresivoLandscape(state, actions, nav) }
        else DesignCanvas(800.dp, 1224.dp) { ExpresivoPortrait(state, actions, nav) }
    }
}

@Composable
private fun ExpresivoLandscape(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 44.dp, top = 20.dp, bottom = 14.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.width(352.dp).fillMaxHeight()) {
                CookieClock(actions, Modifier.size(352.dp))
                Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                BatteryPill(state, actions, Modifier.padding(start = 8.dp).width(336.dp).height(74.dp))
                Spacer(Modifier.height(16.dp))
                MediaPill(state, actions, Modifier.padding(start = 8.dp).width(336.dp).height(102.dp))
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.width(404.dp).fillMaxHeight()) {
                DateBlock(state, actions, nav)
                Spacer(Modifier.weight(1f).heightIn(min = 14.dp))
                Row {
                    SunnyWeather(state, actions, nav, Modifier.size(236.dp))
                    Spacer(Modifier.width(16.dp))
                    CalendarArch(state, actions, Modifier.width(152.dp).height(236.dp))
                }
                Spacer(Modifier.height(18.dp))
                TasksCard(state, actions, nav, Modifier.fillMaxWidth().heightIn(min = 132.dp))
            }
            Spacer(Modifier.weight(1f))
            AppGrid(state, actions, nav, columns = 3, Modifier.width(380.dp))
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SearchBar(actions, nav, Modifier.width(590.dp))
            Spacer(Modifier.width(22.dp))
            DockPill(state, actions, nav, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExpresivoPortrait(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CookieClock(actions, Modifier.size(300.dp))
            Spacer(Modifier.width(28.dp))
            DateBlock(state, actions, nav)
        }
        Spacer(Modifier.height(24.dp))
        Row {
            SunnyWeather(state, actions, nav, Modifier.size(220.dp))
            Spacer(Modifier.width(16.dp))
            CalendarArch(state, actions, Modifier.width(150.dp).height(220.dp))
            Spacer(Modifier.width(16.dp))
            TasksCard(state, actions, nav, Modifier.weight(1f).height(220.dp))
        }
        Spacer(Modifier.height(24.dp))
        AppGrid(state, actions, nav, columns = 4, Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BatteryPill(state, actions, Modifier.weight(1f).height(74.dp))
            MediaPill(state, actions, Modifier.weight(1.3f).height(102.dp))
        }
        Spacer(Modifier.height(20.dp))
        SearchBar(actions, nav, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        DockPill(state, actions, nav, Modifier.fillMaxWidth(), max = 5)
    }
}

@Composable
private fun CookieClock(actions: LauncherActions, modifier: Modifier) {
    val now = rememberNow()
    val is24 = is24Hour()
    Box(
        modifier.clip(PolarShape(12, 0.055f)).background(Lila).clickable(onClick = actions::openClock),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clockHours(now, is24), style = e(148.sp, 820, LilaDeep, (-2).sp, wide = true).copy(lineHeight = 120.sp))
            Text(clockMinutes(now), style = e(148.sp, 820, Violet, (-2).sp, wide = true).copy(lineHeight = 120.sp))
        }
    }
}

@Composable
private fun DateBlock(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    val now = rememberNow()
    val is24 = is24Hour()
    Column {
        Text(formatWeekday(now).uppercase(), style = e(18.sp, 600, Violet, 3.sp))
        Text(formatDayMonth(now), style = e(50.sp, 760, Ink, (-1).sp).copy(lineHeight = 54.sp), modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val w = state.weather
            if (w is WeatherUi.Ready) {
                Pill(weatherIcon(w.data.code, w.data.isDay), "${w.data.temperature}° · ${weatherDescription(w.data.code).lowercase()}", Color(0xFFFFE9C2), Color(0xFF4A3300), actions::refreshWeather)
            }
            val next = state.nextEvent
            val alarm = state.nextAlarmMillis
            when {
                next != null -> Pill(Icons.Rounded.CalendarToday, "${next.title} ${eventTime(next, is24)}", Color(0xFFE9DDFF), LilaDeep) { actions.openCalendar(next) }
                alarm != null -> Pill(Icons.Rounded.Alarm, formatAlarm(alarm, is24), Color(0xFFE9DDFF), LilaDeep, actions::openClock)
            }
        }
    }
}

@Composable
private fun Pill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(bg).clickable(onClick = onClick).padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = fg)
        Spacer(Modifier.width(8.dp))
        Text(text, style = e(16.sp, 600, fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SunnyWeather(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val w = state.weather
    val fg = Color(0xFF3D2A00)
    Box(
        modifier.clip(PolarShape(9, 0.085f)).background(Color(0xFFFFD98A))
            .clickable(onClick = if (w is WeatherUi.NotSet) nav.openSettings else actions::refreshWeather),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (w) {
                is WeatherUi.Ready -> {
                    Icon(weatherIcon(w.data.code, w.data.isDay), null, Modifier.size(40.dp), tint = fg)
                    Text("${w.data.temperature}°", style = e(64.sp, 800, fg, (-2).sp, wide = true).copy(lineHeight = 66.sp))
                    Text("${w.data.place} · ${w.data.max}° / ${w.data.min}°", style = e(14.sp, 600, fg), maxLines = 1)
                }
                else -> {
                    Text("–°", style = e(64.sp, 800, fg, wide = true))
                    Text(if (w is WeatherUi.NotSet) "Elige tu ciudad" else "Cargando…", style = e(14.sp, 600, fg))
                }
            }
        }
    }
}

@Composable
private fun CalendarArch(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val now = rememberNow()
    val fg = Color(0xFF5C1300)
    val count = state.todayEvents.size
    Column(
        modifier.clip(ArchShape).background(Color(0xFFFFDAD3)).clickable { actions.openCalendar(null) },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(formatMonthShort(now).uppercase(), style = e(15.sp, 700, fg, 2.sp))
        Text(now.dayOfMonth.toString(), style = e(82.sp, 850, fg, (-3).sp, wide = true).copy(lineHeight = 84.sp))
        val label = when {
            state.calendar is dev.lanzador.tablet.data.CalendarUi.NoPermission -> "Ver eventos"
            count == 0 -> "Sin eventos"
            count == 1 -> "1 evento"
            else -> "$count eventos"
        }
        Text(
            label, style = e(13.sp, 600, fg),
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(fg.copy(alpha = 0.1f))
                .clickable { if (state.calendar is dev.lanzador.tablet.data.CalendarUi.NoPermission) actions.request(Permission.CALENDAR) else actions.openCalendar(null) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TasksCard(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val brown = Color(0xFF8A5A00)
    Column(
        modifier.shadow(12.dp, RoundedCornerShape(48.dp, 18.dp, 48.dp, 48.dp), ambientColor = Color(0x33502800), spotColor = Color(0x33502800))
            .clip(RoundedCornerShape(48.dp, 18.dp, 48.dp, 48.dp)).background(Cream)
            .padding(start = 24.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = brown)
            Spacer(Modifier.width(8.dp))
            Text("PARA HOY", style = e(13.sp, 700, brown, 2.sp), modifier = Modifier.weight(1f))
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFFFE08A)).clickable(onClick = nav.addTask), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "Añadir tarea", Modifier.size(20.dp), tint = Color(0xFF3D2E00))
            }
        }
        val tasks = state.tasks.sortedBy { it.done }.take(3)
        if (tasks.isEmpty()) {
            Text("Nada pendiente. Pulsa + para añadir.", style = e(16.sp, 500, Ink.copy(alpha = 0.6f)), modifier = Modifier.padding(top = 10.dp))
        }
        tasks.forEach { task -> TaskRow(task, brown, actions) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(task: TaskItem, accent: Color, actions: LauncherActions) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = { actions.toggleTask(task) }, onLongClick = { actions.deleteTask(task) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                .background(if (task.done) accent else Color.Transparent)
                .then(if (task.done) Modifier else Modifier.background(Color.Transparent)),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Color.White)
            else androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(accent, style = Stroke(2.5.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            task.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = e(18.sp, 550, if (task.done) Ink.copy(alpha = 0.5f) else Ink).copy(textDecoration = if (task.done) TextDecoration.LineThrough else null),
        )
    }
}

@Composable
private fun BatteryPill(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val level = state.battery?.level ?: 0
    val green = Color(0xFF2E6B3C)
    Row(
        modifier.shadow(10.dp, RoundedCornerShape(37.dp), ambientColor = Color(0x33502800), spotColor = Color(0x33502800))
            .clip(RoundedCornerShape(37.dp)).background(Cream).clickable { actions.quick(QuickAction.BATTERY) }
            .padding(start = 22.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (state.battery?.charging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd, null, Modifier.size(24.dp), tint = green)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFD9EEDD))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(level / 100f).clip(RoundedCornerShape(14.dp)).background(green))
        }
        Spacer(Modifier.width(14.dp))
        Text("$level %", style = e(22.sp, 800, Color(0xFF1D4A28), wide = true))
    }
}

@Composable
private fun MediaPill(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val media = state.media
    val text = Color(0xFFF1E9FF)
    Row(
        modifier.clip(RoundedCornerShape(51.dp)).background(LilaDeep)
            .clickable {
                when {
                    media != null -> actions.openMediaApp(media)
                    !state.notificationAccess -> actions.request(Permission.NOTIFICATIONS_ACCESS)
                }
            }
            .padding(start = 26.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(media?.title ?: if (state.notificationAccess) "Nada sonando" else "Música", style = e(18.sp, 700, text), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                media?.artist ?: if (state.notificationAccess) "Pon música en cualquier app" else "Toca para activar el acceso",
                style = e(13.sp, 500, text.copy(alpha = 0.75f)), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val tick = rememberTicker(media?.playing == true)
            val frac = remember(tick, media) { if (media != null && media.durationMs > 0) mediaPosition(media).toFloat() / media.durationMs else 0f }
            WavyProgress(frac, playing = media?.playing == true, Modifier.padding(top = 8.dp).width(190.dp).height(14.dp))
        }
        Box(
            Modifier.size(72.dp).clip(PolarShape(4, 0.12f)).background(Lila).clickable(enabled = media != null, onClick = actions::mediaPlayPause),
            contentAlignment = Alignment.Center,
        ) { Icon(if (media?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproducir o pausar", Modifier.size(30.dp), tint = LilaDeep) }
    }
}

/** Barra de progreso ondulada de Material 3 Expressive. */
@Composable
private fun WavyProgress(fraction: Float, playing: Boolean, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val y = size.height / 2
        val stroke = 4.dp.toPx()
        val end = size.width * fraction.coerceIn(0f, 1f)
        drawLine(Lila.copy(alpha = 0.35f), Offset(end + stroke, y), Offset(size.width, y), stroke, StrokeCap.Round)
        if (end > 0f) {
            val path = Path()
            val amp = if (playing) 3.6.dp.toPx() else 0f
            val period = 5.dp.toPx()
            var x = 0f
            path.moveTo(0f, y)
            while (x <= end) {
                path.lineTo(x, y + amp * sin(x / period))
                x += 1f
            }
            drawPath(path, Lila, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun AppGrid(state: HomeState, actions: LauncherActions, nav: HomeNav, columns: Int, modifier: Modifier) {
    val apps = state.favorites.take(9)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        apps.chunked(columns).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEachIndexed { c, app ->
                    val i = r * columns + c
                    val (bg, fg) = Pastels[i % Pastels.size]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
                        AppTarget(app, state, actions, shape = ShapeCycle[i % ShapeCycle.size], menu = TileMenuContext(AppListKind.FAVORITES, i, state.favorites.size)) {
                            ShapedAppIcon(app, 104.dp, ShapeCycle[i % ShapeCycle.size], bg, fg, badge = state.notifications[app.packageName]?.count ?: 0)
                        }
                        Text(app.label, style = e(15.sp, 600), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.width(120.dp)) }
            }
        }
        if (apps.size < 9) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Pill(Icons.Rounded.Add, "Añadir favoritos", Color(0xFFEDE2D6), Ink) { nav.openDrawer(false, null) }
            }
        }
    }
}

@Composable
private fun SearchBar(actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    Row(
        modifier.height(76.dp).shadow(14.dp, RoundedCornerShape(38.dp), ambientColor = Color(0x44502800), spotColor = Color(0x44502800))
            .clip(RoundedCornerShape(38.dp)).background(Color(0xFFFFFAF5)).clickable { nav.openDrawer(true, null) }
            .padding(start = 26.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, Modifier.size(26.dp), tint = Violet)
        Spacer(Modifier.width(14.dp))
        Text("Buscar apps, contactos y web", style = e(19.sp, 500, Color(0xFF4A4458)), modifier = Modifier.weight(1f))
        SmallRound(Icons.Rounded.Mic, "Buscar por voz", actions::voiceSearch)
        Spacer(Modifier.width(10.dp))
        SmallRound(Icons.Rounded.Apps, "Todas las apps") { nav.openDrawer(false, null) }
    }
}

@Composable
private fun SmallRound(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFEFE6FF)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, Modifier.size(22.dp), tint = LilaDeep)
    }
}

@Composable
private fun DockPill(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier, max: Int = 6) {
    val apps = state.dock.take(max)
    Row(
        modifier.height(76.dp).clip(RoundedCornerShape(38.dp)).background(Color(0xFFEDE2D6)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(LilaDeep).clickable { nav.openDrawer(false, null) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Apps, "Todas las apps", Modifier.size(26.dp), tint = Color(0xFFF1E9FF))
        }
        apps.forEachIndexed { i, app ->
            val (bg, fg) = Pastels[(i + 1) % Pastels.size]
            val shape: Shape = if (i % 2 == 0) CircleShape else RoundedCornerShape(20.dp)
            AppTarget(app, state, actions, shape = shape, menu = TileMenuContext(AppListKind.DOCK, i, apps.size)) {
                ShapedAppIcon(app, 58.dp, shape, bg, fg, badge = state.notifications[app.packageName]?.count ?: 0)
            }
        }
    }
}
