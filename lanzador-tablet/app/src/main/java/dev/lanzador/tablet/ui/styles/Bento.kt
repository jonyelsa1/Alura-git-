package dev.lanzador.tablet.ui.styles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanzador.tablet.data.AppListKind
import dev.lanzador.tablet.data.PhotoUi
import dev.lanzador.tablet.data.TaskItem
import dev.lanzador.tablet.data.WeatherUi
import dev.lanzador.tablet.data.weatherDescription
import dev.lanzador.tablet.ui.HomeNav
import dev.lanzador.tablet.ui.HomeState
import dev.lanzador.tablet.ui.LauncherActions
import dev.lanzador.tablet.ui.Permission
import dev.lanzador.tablet.ui.QuickAction
import dev.lanzador.tablet.ui.common.AppTarget
import dev.lanzador.tablet.ui.common.ColorAppIcon
import dev.lanzador.tablet.ui.common.DesignCanvas
import dev.lanzador.tablet.ui.common.widgetClick
import dev.lanzador.tablet.ui.common.GlassSurface
import dev.lanzador.tablet.ui.common.GlassTint
import dev.lanzador.tablet.ui.common.TileMenuContext
import dev.lanzador.tablet.ui.common.countdown
import dev.lanzador.tablet.ui.common.eventTime
import dev.lanzador.tablet.ui.common.formatAlarm
import dev.lanzador.tablet.ui.common.formatDuration
import dev.lanzador.tablet.ui.common.formatLongDate
import dev.lanzador.tablet.ui.common.greeting
import dev.lanzador.tablet.ui.common.is24Hour
import dev.lanzador.tablet.ui.common.mediaPosition
import dev.lanzador.tablet.ui.common.rememberNow
import dev.lanzador.tablet.ui.common.rememberTicker
import dev.lanzador.tablet.ui.common.weatherIcon
import dev.lanzador.tablet.ui.theme.Inter
import dev.lanzador.tablet.ui.theme.Outfit
import java.time.ZoneId

private val Amber = Color(0xFFFFB547)
private val Lila = Color(0xFFB9A6FF)
private val Txt = Color(0xFFF4F0FF)
private val Mut = Color(0xFFA99FC4)
private val OnAmber = Color(0xFF2B1600)
private val TileShape = RoundedCornerShape(28.dp)
private const val GAP = 14

private fun b(size: TextUnit, weight: Int = 500, color: Color = Txt, spacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = Inter, fontSize = size, fontWeight = FontWeight(weight), color = color, letterSpacing = spacing)

private fun o(size: TextUnit, weight: Int = 300, color: Color = Txt, spacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = Outfit, fontSize = size, fontWeight = FontWeight(weight), color = color, letterSpacing = spacing, lineHeight = size * 1.0f)

@Composable
fun BentoHome(state: HomeState, actions: LauncherActions, nav: HomeNav, landscape: Boolean) {
    CompositionLocalProvider(LocalContentColor provides Txt) {
        if (landscape) DesignCanvas(1280.dp, 744.dp) { BentoLandscape(state, actions, nav) }
        else DesignCanvas(800.dp, 1224.dp) { BentoPortrait(state, actions, nav) }
    }
}

@Composable
private fun BentoLandscape(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 8.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
            Column(Modifier.weight(601f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                Hero(state, actions, Modifier.fillMaxWidth().weight(308f))
                Row(Modifier.fillMaxWidth().weight(308f), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
                    Favorites(state, actions, nav, Modifier.weight(396f).fillMaxHeight())
                    Toggles(state, actions, Modifier.weight(191f).fillMaxHeight())
                }
            }
            Column(Modifier.weight(396f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                Weather(state, actions, nav, Modifier.fillMaxWidth().weight(147f))
                Photo(state, actions, Modifier.fillMaxWidth().weight(147f))
                Tasks(state, actions, nav, Modifier.fillMaxWidth().weight(308f))
            }
            Column(Modifier.weight(191f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                Focus(state, actions, Modifier.fillMaxWidth().weight(308f))
                Battery(state, actions, Modifier.fillMaxWidth().weight(147f))
                Music(state, actions, Modifier.fillMaxWidth().weight(147f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Dock(state, actions, nav, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun BentoPortrait(state: HomeState, actions: LauncherActions, nav: HomeNav) {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
        Hero(state, actions, Modifier.fillMaxWidth().height(300.dp))
        Row(Modifier.fillMaxWidth().height(300.dp), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
            Column(Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                Weather(state, actions, nav, Modifier.fillMaxWidth().weight(1f))
                Photo(state, actions, Modifier.fillMaxWidth().weight(1f))
            }
            Focus(state, actions, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
            Favorites(state, actions, nav, Modifier.weight(2f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                Battery(state, actions, Modifier.fillMaxWidth().weight(1f))
                Music(state, actions, Modifier.fillMaxWidth().weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth().height(230.dp), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
            Tasks(state, actions, nav, Modifier.weight(2f).fillMaxHeight())
            Toggles(state, actions, Modifier.weight(1f).fillMaxHeight())
        }
        Dock(state, actions, nav, Modifier.align(Alignment.CenterHorizontally), max = 4, searchWidth = 240.dp)
    }
}

@Composable
private fun Tile(modifier: Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 20.dp), content: @Composable BoxScope.() -> Unit) {
    GlassSurface(modifier, shape = TileShape, tint = GlassTint.DARK, onClick = onClick, contentPadding = padding, content = content)
}

@Composable
private fun Kicker(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(15.dp), tint = Mut)
            Spacer(Modifier.width(7.dp))
        }
        Text(text.uppercase(), style = b(12.sp, 700, Mut, 1.6.sp))
    }
}

@Composable
private fun Hero(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val now = rememberNow()
    val is24 = is24Hour()
    Tile(modifier, padding = PaddingValues(horizontal = 30.dp, vertical = 24.dp)) {
        Column(Modifier.fillMaxSize()) {
            Kicker(greeting(now.hour))
            val h = now.format(java.time.format.DateTimeFormatter.ofPattern(if (is24) "H" else "h"))
            val m = now.format(java.time.format.DateTimeFormatter.ofPattern("mm"))
            Text(
                buildAnnotatedString {
                    append(h)
                    withStyle(SpanStyle(color = Amber)) { append(":") }
                    append(m)
                },
                style = o(128.sp, 250, spacing = (-6).sp),
                modifier = Modifier.padding(top = 2.dp).widgetClick(onClick = actions::openClock),
            )
            Text(formatLongDate(now), style = b(22.sp, 500, Color(0xFFDDD5F5)))
            Spacer(Modifier.weight(1f))
            val next = state.nextEvent
            val alarm = state.nextAlarmMillis
            val millisNow = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.06f))
                    .widgetClick { if (next != null) actions.openCalendar(next) else if (alarm != null) actions.openClock() else actions.openCalendar(null) }
                    .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Amber.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(if (next == null && alarm != null) Icons.Rounded.Alarm else Icons.Rounded.CalendarToday, null, Modifier.size(22.dp), tint = Amber)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    when {
                        next != null -> {
                            Text(next.title, style = b(16.sp, 650), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(eventTime(next, is24), style = b(13.sp, 500, Mut))
                        }
                        alarm != null -> {
                            Text("Próxima alarma", style = b(16.sp, 650))
                            Text(formatAlarm(alarm, is24), style = b(13.sp, 500, Mut))
                        }
                        else -> {
                            Text("Sin eventos hoy", style = b(16.sp, 650))
                            Text(if (state.calendar is dev.lanzador.tablet.data.CalendarUi.NoPermission) "Toca para ver tu calendario" else "Día libre", style = b(13.sp, 500, Mut))
                        }
                    }
                }
                val target = next?.takeIf { !it.allDay }?.begin ?: alarm
                if (target != null && target > millisNow) {
                    Text(
                        countdown(millisNow, target), style = b(14.sp, 700, OnAmber),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Amber).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Weather(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val w = state.weather
    Tile(modifier, onClick = if (w is WeatherUi.NotSet) nav.openSettings else actions::refreshWeather) {
        when (w) {
            is WeatherUi.Ready -> Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(128.dp)) {
                    Kicker(w.data.place, Icons.Rounded.LocationOn)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Text("${w.data.temperature}°", style = o(52.sp, 300))
                        Spacer(Modifier.width(6.dp))
                        Icon(weatherIcon(w.data.code, w.data.isDay), null, Modifier.size(28.dp), tint = Amber)
                    }
                    Text(weatherDescription(w.data.code), style = b(13.sp, 500, Mut), maxLines = 1)
                    Text("Máx ${w.data.max}° · Mín ${w.data.min}°", style = b(13.sp, 500, Mut), maxLines = 1)
                }
                Spacer(Modifier.width(14.dp))
                TempChart(w.data.hourly.take(9).map { it.temperature }, w.data.hourly.take(9).map { it.hour }, Modifier.weight(1f).fillMaxHeight())
            }
            WeatherUi.NotSet -> Column { Kicker("Tiempo", Icons.Rounded.LocationOn); Text("Toca para elegir tu ciudad", style = b(16.sp, 600), modifier = Modifier.padding(top = 12.dp)) }
            is WeatherUi.Loading -> Column { Kicker(w.place, Icons.Rounded.LocationOn); Text("Cargando…", style = b(16.sp, 600), modifier = Modifier.padding(top = 12.dp)) }
            is WeatherUi.Error -> Column { Kicker(w.place, Icons.Rounded.LocationOn); Text("Sin datos · toca para reintentar", style = b(16.sp, 600), modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}

@Composable
private fun TempChart(temps: List<Int>, hours: List<Int>, modifier: Modifier) {
    if (temps.size < 2) return
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            val min = temps.min() - 1
            val max = temps.max() + 1
            val pts = temps.mapIndexed { i, t ->
                Offset(size.width * i / (temps.size - 1), size.height * (1f - (t - min).toFloat() / (max - min)))
            }
            val line = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val p0 = pts[i - 1]
                    val p1 = pts[i]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(Amber.copy(alpha = 0.45f), Amber.copy(alpha = 0f))))
            drawPath(line, Amber, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Color.White, 4.5.dp.toPx(), pts[0])
            drawCircle(Amber, 4.5.dp.toPx(), pts[0], style = Stroke(2.5.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 2, 4, 6, 8).filter { it < hours.size }.forEach { i ->
                Text(if (i == 0) "Ahora" else "${hours[i]} h", style = b(11.sp, 500, Mut))
            }
        }
    }
}

@Composable
private fun Focus(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val timer = state.timer
    val now = rememberTicker(timer.running)
    val remaining = timer.remainingAt(now)
    val fraction = 1f - remaining.toFloat() / timer.durationMs.coerceAtLeast(1)
    Tile(modifier, padding = PaddingValues(16.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Kicker("Enfoque", Icons.Rounded.HourglassEmpty)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val s = 11.dp.toPx()
                    val tl = Offset(s / 2, s / 2)
                    val sz = Size(size.width - s, size.height - s)
                    drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, tl, sz, style = Stroke(s))
                    val sweep = if (remaining == timer.durationMs && !timer.running) 360f else 360f * (1f - fraction)
                    drawArc(Amber, -90f, sweep, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDuration(remaining), style = o(34.sp, 400))
                    Text("de ${formatDuration(timer.durationMs)}", style = b(12.sp, 500, Mut))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SquareButton(Icons.Rounded.Refresh, "Reiniciar", Color.White.copy(alpha = 0.08f), Txt, actions::timerReset)
                SquareButton(if (timer.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (timer.running) "Pausar" else "Empezar", Amber, OnAmber, if (timer.running) actions::timerPause else actions::timerStart)
            }
        }
    }
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(bg).widgetClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, Modifier.size(22.dp), tint = fg)
    }
}

@Composable
private fun Photo(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val photos = state.photos
    Box(
        modifier.clip(TileShape).widgetClick {
            if (photos is PhotoUi.NoPermission) actions.request(Permission.PHOTOS) else actions.openPhoto(photos)
        },
    ) {
        if (photos is PhotoUi.Ready) {
            Image(photos.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
        } else {
            SunsetIllustration(Modifier.fillMaxSize())
        }
        Text(
            if (photos is PhotoUi.Ready) "Recuerdo" else "Fotos", style = b(12.sp, 600),
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x730C0818)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp)) {
            when (photos) {
                is PhotoUi.Ready -> { Text(photos.title, style = b(17.sp, 650)); Text(photos.subtitle, style = b(13.sp, 500, Color(0xFFF1E7FF))) }
                PhotoUi.NoPermission -> { Text("Recuerdos", style = b(17.sp, 650)); Text("Toca para permitir el acceso a tus fotos", style = b(13.sp, 500, Color(0xFFF1E7FF))) }
                PhotoUi.Empty -> { Text("Sin fotos recientes", style = b(17.sp, 650)); Text("Aquí saldrán las de hace un año", style = b(13.sp, 500, Color(0xFFF1E7FF))) }
            }
        }
    }
}

@Composable
private fun SunsetIllustration(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(0f to Color(0xFF2A1B5E), 0.55f to Color(0xFFC2477A), 1f to Color(0xFFFFB06B)))
        drawCircle(Color(0xFFFFD9A0), h * 0.18f, Offset(w * 0.7f, h * 0.62f))
        drawPath(Path().apply {
            moveTo(0f, h * 0.75f); lineTo(w * 0.15f, h * 0.48f); lineTo(w * 0.27f, h * 0.67f); lineTo(w * 0.43f, h * 0.35f)
            lineTo(w * 0.6f, h * 0.68f); lineTo(w * 0.76f, h * 0.49f); lineTo(w, h * 0.76f); lineTo(w, h); lineTo(0f, h); close()
        }, Color(0xFF4B2A6E))
        drawPath(Path().apply {
            moveTo(0f, h * 0.87f); lineTo(w * 0.2f, h * 0.65f); lineTo(w * 0.38f, h * 0.83f); lineTo(w * 0.56f, h * 0.61f)
            lineTo(w * 0.76f, h * 0.84f); lineTo(w, h * 0.68f); lineTo(w, h); lineTo(0f, h); close()
        }, Color(0xFF2A1840))
    }
}

@Composable
private fun Favorites(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    val apps = state.favorites.take(8)
    Tile(modifier) {
        Column(Modifier.fillMaxSize()) {
            Kicker("Favoritos", Icons.Rounded.Star)
            Spacer(Modifier.height(16.dp))
            apps.chunked(4).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    row.forEachIndexed { c, app ->
                        val i = r * 4 + c
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AppTarget(app, state, actions, shape = RoundedCornerShape(19.dp), menu = TileMenuContext(AppListKind.FAVORITES, i, state.favorites.size)) {
                                ColorAppIcon(app, 60.dp, badge = state.notifications[app.packageName]?.count ?: 0)
                            }
                            Text(app.label, style = b(12.5.sp, 600, Color(0xFFE9E3FB)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).widgetClick { nav.openDrawer(false, null) }.padding(top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Todas las apps · ${state.apps.size}", style = b(14.sp, 600, Lila), modifier = Modifier.weight(1f))
                @Suppress("DEPRECATION")
                Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = Lila)
            }
        }
    }
}

private data class ToggleSpec(val action: QuickAction, val icon: ImageVector, val label: String, val on: Boolean)

@Composable
private fun Toggles(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val d = state.device
    @Suppress("DEPRECATION")
    val specs = listOf(
        ToggleSpec(QuickAction.WIFI, Icons.Rounded.Wifi, "Wi-Fi", d.wifiOn),
        ToggleSpec(QuickAction.BLUETOOTH, Icons.Rounded.Bluetooth, "Bluetooth", d.bluetoothOn),
        ToggleSpec(QuickAction.DND, Icons.Rounded.NotificationsOff, "Silencio", d.dndOn),
        if (d.torchAvailable) ToggleSpec(QuickAction.TORCH, Icons.Rounded.FlashOn, "Linterna", d.torchOn)
        else ToggleSpec(QuickAction.DISPLAY, Icons.Rounded.BrightnessMedium, "Pantalla", false),
        ToggleSpec(QuickAction.ROTATE, Icons.Rounded.ScreenRotation, "Girar", d.autoRotate),
        ToggleSpec(QuickAction.VOLUME, Icons.Rounded.VolumeUp, "Sonido", false),
    )
    Tile(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(start = 8.dp)) { Kicker("Rápidos") }
            Spacer(Modifier.weight(1f))
            specs.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { s ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp).clip(RoundedCornerShape(14.dp)).widgetClick { actions.quick(s.action) }) {
                            Box(Modifier.size(50.dp).clip(CircleShape).background(if (s.on) Amber else Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                                Icon(s.icon, null, Modifier.size(21.dp), tint = if (s.on) OnAmber else Color(0xFFE9E3FB))
                            }
                            Text(s.label, style = b(11.5.sp, 600, if (s.on) Txt else Mut), modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tasks(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier) {
    Tile(modifier) {
        Column(Modifier.fillMaxSize()) {
            Kicker("Para hoy", Icons.Rounded.CheckCircle)
            Spacer(Modifier.height(6.dp))
            state.tasks.sortedBy { it.done }.take(4).forEach { task -> TaskLine(task, actions) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).widgetClick(onClick = nav.addTask).padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = Mut)
                Spacer(Modifier.width(12.dp))
                Text("Añadir tarea", style = b(16.sp, 500, Mut))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskLine(task: TaskItem, actions: LauncherActions) {
    Column {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .combinedClickable(onClick = { actions.toggleTask(task) }, onLongClick = { actions.deleteTask(task) })
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (task.done) Lila else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (task.done) Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = Color(0xFF1A1230))
                else Canvas(Modifier.fillMaxSize()) { drawRoundRect(Color.White.copy(alpha = 0.3f), style = Stroke(2.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())) }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                task.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = b(16.sp, 500, if (task.done) Mut else Txt).copy(textDecoration = if (task.done) TextDecoration.LineThrough else null),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
    }
}

@Composable
private fun Battery(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    Tile(modifier, onClick = { actions.quick(QuickAction.BATTERY) }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Kicker("Batería", Icons.Rounded.BatteryStd)
            BarLine(Icons.Rounded.TabletAndroid, state.battery?.level ?: 0, Color(0xFF7CF0A8))
            BarLine(Icons.Rounded.Storage, state.device.storageUsedPercent, Lila)
        }
    }
}

@Composable
private fun BarLine(icon: ImageVector, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = Mut)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(percent / 100f).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Text("$percent %", style = b(13.sp, 600))
    }
}

@Composable
private fun Music(state: HomeState, actions: LauncherActions, modifier: Modifier) {
    val media = state.media
    Tile(modifier, padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).widgetClick {
                    when {
                        media != null -> actions.openMediaApp(media)
                        !state.mediaSupported -> actions.openMusicApp()
                        !state.notificationAccess -> actions.request(Permission.NOTIFICATIONS_ACCESS)
                    }
                },
            ) {
                AlbumArt(media?.art, 44.dp, 12.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    val ask = state.mediaSupported && !state.notificationAccess
                    Text(media?.title ?: if (state.mediaSupported && !ask) "Nada sonando" else "Música", style = b(14.sp, 650), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media?.artist ?: when { ask -> "Activar acceso"; state.mediaSupported -> "—"; else -> "Abrir música" }, style = b(12.sp, 500, Mut), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (media != null) {
                val tick = rememberTicker(media.playing)
                val frac = remember(tick, media) { if (media.durationMs > 0) mediaPosition(media).toFloat() / media.durationMs else 0f }
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Amber))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SmallIcon(Icons.Rounded.SkipPrevious, "Anterior", actions::mediaPrevious)
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White).widgetClick(onClick = actions::mediaPlayPause), contentAlignment = Alignment.Center) {
                    Icon(if (media?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproducir o pausar", Modifier.size(22.dp), tint = Color(0xFF1A1230))
                }
                SmallIcon(Icons.Rounded.SkipNext, "Siguiente", actions::mediaNext)
            }
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).widgetClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, Modifier.size(22.dp), tint = Color(0xFFE9E3FB))
    }
}

@Composable
private fun Dock(state: HomeState, actions: LauncherActions, nav: HomeNav, modifier: Modifier, max: Int = 5, searchWidth: Dp = 300.dp) {
    val apps = state.dock.take(max)
    GlassSurface(modifier.height(78.dp), shape = RoundedCornerShape(30.dp), tint = GlassTint.DARK, contentPadding = PaddingValues(horizontal = 12.dp)) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.width(searchWidth).height(54.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.07f))
                    .widgetClick { nav.openDrawer(true, null) }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = Color(0xFFE9E3FB))
                Spacer(Modifier.width(10.dp))
                Text("Buscar apps y web", style = b(15.sp, 500, Mut))
            }
            apps.forEachIndexed { i, app ->
                AppTarget(app, state, actions, shape = RoundedCornerShape(17.dp), menu = TileMenuContext(AppListKind.DOCK, i, apps.size)) {
                    ColorAppIcon(app, 54.dp, badge = state.notifications[app.packageName]?.count ?: 0)
                }
            }
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = 0.08f)).widgetClick(onClickLabel = "Cambiar estilo", onClick = nav.openStylePicker),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Palette, "Cambiar estilo", Modifier.size(24.dp), tint = Color(0xFFE9E3FB)) }
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xFFF6F2FF), Lila)))
                    .widgetClick { nav.openDrawer(false, null) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Apps, "Todas las apps", Modifier.size(25.dp), tint = Color(0xFF1A1230)) }
        }
    }
}
