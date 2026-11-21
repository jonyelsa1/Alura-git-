package dev.lanzador.tablet.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que el servicio de notificaciones publica para la interfaz. */
object NotificationHub {
    internal val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal val _notifications = MutableStateFlow<Map<String, NotifSummary>>(emptyMap())
    val notifications: StateFlow<Map<String, NotifSummary>> = _notifications.asStateFlow()

    internal val _media = MutableStateFlow<MediaInfo?>(null)
    val media: StateFlow<MediaInfo?> = _media.asStateFlow()

    @Volatile internal var controller: MediaController? = null

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }

    fun previous() { controller?.transportControls?.skipToPrevious() }
}

/**
 * Lee las notificaciones (para los contadores y la vista previa de Enfoque) y la
 * música que suena (para el widget de música). El usuario debe activarlo en
 * Ajustes > Notificaciones > Acceso a notificaciones.
 */
class NotificationService : NotificationListenerService() {

    private var sessions: MediaSessionManager? = null
    private var controller: MediaController? = null
    private val main = Handler(Looper.getMainLooper())

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() = pick(activeSessions())
    }

    override fun onListenerConnected() {
        NotificationHub._connected.value = true
        publishNotifications()
        sessions = getSystemService(MediaSessionManager::class.java)
        runCatching {
            sessions?.addOnActiveSessionsChangedListener(sessionsListener, component(), main)
            pick(activeSessions())
        }
    }

    override fun onListenerDisconnected() {
        NotificationHub._connected.value = false
        runCatching { sessions?.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
        NotificationHub.controller = null
        NotificationHub._media.value = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publishNotifications()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publishNotifications()

    private fun component() = ComponentName(this, NotificationService::class.java)

    private fun activeSessions(): List<MediaController> =
        runCatching { sessions?.getActiveSessions(component()).orEmpty() }.getOrDefault(emptyList())

    private fun publishNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationHub._notifications.value = active
            .filter { it.isClearable && !it.isOngoing && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .groupBy { it.packageName }
            .mapValues { (pkg, items) ->
                val last = items.maxBy { it.postTime }
                val extras = last.notification.extras
                NotifSummary(
                    packageName = pkg,
                    count = items.size,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                )
            }
    }

    private fun pick(list: List<MediaController>?) {
        val chosen = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = chosen
            chosen?.registerCallback(callback, main)
        }
        NotificationHub.controller = controller
        publishMedia()
    }

    private fun publishMedia() {
        val c = controller
        val metadata = c?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: metadata?.description?.title?.toString()
        if (c == null || metadata == null || title.isNullOrBlank()) {
            NotificationHub._media.value = null
            return
        }
        val state = c.playbackState
        val art: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.description?.iconBitmap
        val scaled = art?.let { if (it.width > 256) Bitmap.createScaledBitmap(it, 256, 256 * it.height / it.width, true) else it }
        NotificationHub._media.value = MediaInfo(
            packageName = c.packageName,
            title = title,
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata.description?.subtitle?.toString().orEmpty(),
            art = scaled?.asImageBitmap(),
            playing = state?.state == PlaybackState.STATE_PLAYING,
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = state?.position ?: 0L,
            positionAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed ?: 1f,
        )
    }
}
