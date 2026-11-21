package dev.lanzador.tablet.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.lanzador.tablet.MainActivity
import dev.lanzador.tablet.R

/** Temporizador de enfoque: guarda la hora de fin y programa un aviso. */
object FocusTimer {

    private const val CHANNEL = "enfoque"
    private const val NOTIFICATION_ID = 7

    fun start(context: Context) {
        val prefs = LauncherPrefs.get(context)
        val t = prefs.timer.value
        if (t.running) return
        val remaining = if (t.remainingMs <= 0) t.durationMs else t.remainingMs
        val end = System.currentTimeMillis() + remaining
        prefs.setTimer(t.copy(endAt = end, remainingMs = remaining))
        schedule(context, end)
    }

    fun pause(context: Context) {
        val prefs = LauncherPrefs.get(context)
        val t = prefs.timer.value
        if (!t.running) return
        cancelAlarm(context)
        prefs.setTimer(t.copy(endAt = 0, remainingMs = t.remainingAt(System.currentTimeMillis())))
    }

    fun reset(context: Context) {
        val prefs = LauncherPrefs.get(context)
        cancelAlarm(context)
        val duration = prefs.settings.value.timerMinutes * 60_000L
        prefs.setTimer(TimerState(duration, 0, duration))
    }

    fun setMinutes(context: Context, minutes: Int) {
        val prefs = LauncherPrefs.get(context)
        prefs.updateSettings { it.copy(timerMinutes = minutes) }
        if (!prefs.timer.value.running) reset(context)
    }

    internal fun finished(context: Context) {
        val prefs = LauncherPrefs.get(context)
        val duration = prefs.settings.value.timerMinutes * 60_000L
        prefs.setTimer(TimerState(duration, 0, duration))
        notify(context)
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, TimerReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun schedule(context: Context, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun notify(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Temporizador de enfoque", NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_temporizador)
            .setContentTitle("Tiempo de enfoque terminado")
            .setContentText("Buen trabajo. Toca para volver al inicio.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = FocusTimer.finished(context)
}
