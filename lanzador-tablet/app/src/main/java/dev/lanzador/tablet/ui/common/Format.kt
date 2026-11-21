package dev.lanzador.tablet.ui.common

import android.content.Context
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.lanzador.tablet.data.CalendarEvent
import dev.lanzador.tablet.data.MediaInfo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Hora fija para pruebas y capturas; null = hora real. */
val LocalFixedTime = staticCompositionLocalOf<LocalDateTime?> { null }

/** La hora actual, actualizada en cada cambio de minuto mientras la pantalla está visible. */
@Composable
fun rememberNow(): LocalDateTime {
    val fixed = LocalFixedTime.current
    if (fixed != null) return fixed
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            }
        }
    }
    return now
}

/** Milisegundos actuales, refrescados cada segundo solo mientras [active]. */
@Composable
fun rememberTicker(active: Boolean): Long {
    val fixed = LocalFixedTime.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (fixed != null) return fixed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(active, owner) {
        now = System.currentTimeMillis()
        if (!active) return@LaunchedEffect
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000L - System.currentTimeMillis() % 1000L + 10L)
            }
        }
    }
    return now
}

@Composable
fun is24Hour(): Boolean {
    val context = LocalContext.current
    return remember(context) { DateFormat.is24HourFormat(context) }
}

private val locale: Locale get() = Locale.getDefault()

private fun pattern(skeleton: String, fallback: String): DateTimeFormatter = runCatching {
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}.getOrElse { DateTimeFormatter.ofPattern(fallback, locale) }

private fun String.capitalized() = replaceFirstChar { it.titlecase(locale) }

fun formatClock(now: LocalDateTime, is24: Boolean): String =
    now.format(DateTimeFormatter.ofPattern(if (is24) "H:mm" else "h:mm", locale))

fun clockHours(now: LocalDateTime, is24: Boolean): String =
    now.format(DateTimeFormatter.ofPattern(if (is24) "HH" else "hh", locale))

fun clockMinutes(now: LocalDateTime): String = now.format(DateTimeFormatter.ofPattern("mm", locale))

/** «Sábado, 21 de noviembre» */
fun formatLongDate(now: LocalDateTime): String = now.format(pattern("EEEEdMMMM", "EEEE d MMMM")).capitalized()

/** «Sábado» */
fun formatWeekday(now: LocalDateTime): String = now.format(DateTimeFormatter.ofPattern("EEEE", locale)).capitalized()

/** «21 de noviembre» */
fun formatDayMonth(now: LocalDateTime): String = now.format(pattern("dMMMM", "d MMMM"))

/** «sáb 21» */
fun formatShortDay(now: LocalDateTime): String = now.format(pattern("EEEd", "EEE d")).replace(".", "")

/** «nov» */
fun formatMonthShort(now: LocalDateTime): String = now.format(DateTimeFormatter.ofPattern("MMM", locale)).replace(".", "")

fun greeting(hour: Int) = when (hour) {
    in 6..13 -> "Buenos días"
    in 14..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

fun formatTimeOf(millis: Long, is24: Boolean): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(if (is24) "H:mm" else "h:mm a", locale))

fun eventTime(event: CalendarEvent, is24: Boolean): String =
    if (event.allDay) "Todo el día" else formatTimeOf(event.begin, is24)

/** «en 9 h 25 min», «en 5 min», «ahora» */
fun countdown(from: Long, to: Long): String {
    val minutes = ((to - from) / 60_000L).toInt()
    return when {
        minutes <= 0 -> "ahora"
        minutes < 60 -> "en $minutes min"
        minutes % 60 == 0 -> "en ${minutes / 60} h"
        else -> "en ${minutes / 60} h ${minutes % 60} min"
    }
}

/** «día 22 · 7:00» o «lun 7:00» para la alarma. */
fun formatAlarm(millis: Long, is24: Boolean): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    return dt.format(pattern(if (is24) "EEEHmm" else "EEEhmma", "EEE H:mm")).replace(".", "")
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun mediaPosition(media: MediaInfo, nowElapsed: Long = SystemClock.elapsedRealtime()): Long {
    val pos = if (media.playing) media.positionMs + ((nowElapsed - media.positionAt) * media.speed).toLong() else media.positionMs
    return if (media.durationMs > 0) pos.coerceIn(0, media.durationMs) else pos.coerceAtLeast(0)
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

fun Context.isLandscape(): Boolean = resources.configuration.screenWidthDp > resources.configuration.screenHeightDp
