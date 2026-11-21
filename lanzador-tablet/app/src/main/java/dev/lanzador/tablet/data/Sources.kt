package dev.lanzador.tablet.data

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun Context.granted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/** Eventos de hoy y el primero de mañana, desde el proveedor de calendario de Android. */
object CalendarSource {

    fun hasPermission(context: Context) = context.granted(Manifest.permission.READ_CALENDAR)

    suspend fun load(context: Context): CalendarUi = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext CalendarUi.NoPermission
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, todayStart - 86_400_000L)
            ContentUris.appendId(it, rangeEnd + 86_400_000L)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val events = ArrayList<CalendarEvent>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(4) == 1
                    var begin = c.getLong(2)
                    var end = c.getLong(3)
                    if (allDay) {
                        // Los eventos de día completo vienen en UTC: se pasan al día local.
                        val startDate = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                        val endDate = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()
                        begin = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                        end = endDate.atStartOfDay(zone).toInstant().toEpochMilli()
                    }
                    events += CalendarEvent(
                        id = c.getLong(0),
                        title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(Sin título)",
                        begin = begin,
                        end = end,
                        allDay = allDay,
                        color = c.getInt(5),
                    )
                }
            }
        }
        val todayEvents = events.filter { it.begin < tomorrowStart && it.end > maxOf(now, todayStart) }
            .sortedWith(compareBy<CalendarEvent>({ !it.allDay }, { it.begin }))
        val tomorrow = events.filter { it.begin >= tomorrowStart && it.begin < rangeEnd }
            .sortedWith(compareBy<CalendarEvent>({ it.allDay }, { it.begin }))
            .firstOrNull()
        CalendarUi.Ready(todayEvents, tomorrow)
    }
}

/** «Hace un año»: fotos tomadas hace un año (±3 o ±15 días); si no hay, las más recientes. */
object PhotoSource {

    val permissions: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasAccess(context: Context) = permissions.any { context.granted(it) }

    suspend fun load(context: Context): PhotoUi = withContext(Dispatchers.IO) {
        if (!hasAccess(context)) return@withContext PhotoUi.NoPermission
        val zone = ZoneId.systemDefault()
        val yearAgo = LocalDate.now(zone).minusYears(1)
        val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.getDefault())
        for (days in listOf(3L, 15L)) {
            val from = yearAgo.minusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = yearAgo.plusDays(days + 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val found = query(context, from, to)
            if (found.isNotEmpty()) {
                val cover = found[found.size / 2]
                val bitmap = thumbnail(context, cover) ?: continue
                val n = found.size
                return@withContext PhotoUi.Ready(
                    title = "Hace un año",
                    subtitle = "${yearAgo.format(fmt)} · $n ${if (n == 1) "foto" else "fotos"}",
                    cover = bitmap.asImageBitmap(),
                    uri = cover,
                )
            }
        }
        val recentFrom = System.currentTimeMillis() - 30L * 86_400_000L
        val recent = query(context, recentFrom, Long.MAX_VALUE)
        val cover = recent.firstOrNull() ?: return@withContext PhotoUi.Empty
        val bitmap = thumbnail(context, cover) ?: return@withContext PhotoUi.Empty
        PhotoUi.Ready("Recientes", "${recent.size} fotos en los últimos 30 días", bitmap.asImageBitmap(), cover)
    }

    private fun query(context: Context, from: Long, to: Long): List<android.net.Uri> {
        val result = ArrayList<android.net.Uri>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?",
                arrayOf(from.toString(), to.toString()),
                "${MediaStore.Images.Media.DATE_TAKEN} DESC",
            )?.use { c ->
                while (c.moveToNext() && result.size < 200) result += ContentUris.withAppendedId(collection, c.getLong(0))
            }
        }
        return result
    }

    private fun thumbnail(context: Context, uri: android.net.Uri): Bitmap? =
        runCatching { context.contentResolver.loadThumbnail(uri, Size(720, 480), null) }.getOrNull()
}

/** Estado de Wi-Fi, Bluetooth, No molestar, linterna, giro y almacenamiento. */
object DeviceSource {

    fun read(context: Context, torchOn: Boolean): DeviceToggles {
        val wifi = runCatching { context.getSystemService(WifiManager::class.java)?.isWifiEnabled == true }.getOrDefault(false)
        val bt = runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true }.getOrDefault(false)
        val dnd = runCatching {
            context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter.let {
                it != null && it != NotificationManager.INTERRUPTION_FILTER_ALL && it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            }
        }.getOrDefault(false)
        val rotate = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)
        val storage = runCatching {
            val fs = StatFs(Environment.getDataDirectory().path)
            ((fs.totalBytes - fs.availableBytes) * 100 / fs.totalBytes).toInt()
        }.getOrDefault(0)
        return DeviceToggles(wifi, bt, dnd, torchOn, torchCameraId(context) != null, rotate, storage)
    }

    fun torchCameraId(context: Context): String? = runCatching {
        val cm = context.getSystemService(CameraManager::class.java)
        cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()
}
