package dev.lanzador.tablet.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppListKind { FAVORITES, DOCK }

/**
 * Favoritos, dock, apps ocultas, tareas, temporizador y ajustes, en SharedPreferences.
 * Una sola instancia por proceso para que el receptor del temporizador y la
 * interfaz vean los mismos datos.
 */
class LauncherPrefs private constructor(context: Context) {

    private val sp = context.getSharedPreferences("lanzador", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readList(K_FAVORITES))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _dock = MutableStateFlow(readList(K_DOCK))
    val dock: StateFlow<List<String>> = _dock.asStateFlow()

    private val _hidden = MutableStateFlow(readList(K_HIDDEN).toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _usage = MutableStateFlow(readUsage())
    val usage: StateFlow<Map<String, Int>> = _usage.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _tasks = MutableStateFlow(readTasks())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _timer = MutableStateFlow(readTimer())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    fun list(kind: AppListKind): List<String> = when (kind) {
        AppListKind.FAVORITES -> _favorites.value
        AppListKind.DOCK -> _dock.value
    }

    fun setList(kind: AppListKind, keys: List<String>) {
        val clean = keys.distinct()
        when (kind) {
            AppListKind.FAVORITES -> {
                _favorites.value = clean
                sp.edit { putString(K_FAVORITES, clean.joinToString("\n")) }
            }
            AppListKind.DOCK -> {
                _dock.value = clean
                sp.edit { putString(K_DOCK, clean.joinToString("\n")) }
            }
        }
    }

    fun toggle(kind: AppListKind, key: String) {
        val current = list(kind)
        setList(kind, if (key in current) current - key else current + key)
    }

    /** Mueve [key] una posición entre las apps que se ven (no entre claves de apps desinstaladas). */
    fun move(kind: AppListKind, key: String, delta: Int, visibleKeys: List<String>) {
        val index = visibleKeys.indexOf(key)
        val neighbour = visibleKeys.getOrNull(index + delta) ?: return
        val stored = list(kind).toMutableList()
        val a = stored.indexOf(key)
        val b = stored.indexOf(neighbour)
        if (a < 0 || b < 0) return
        stored[a] = neighbour
        stored[b] = key
        setList(kind, stored)
    }

    fun setHidden(key: String, hidden: Boolean) {
        val next = if (hidden) _hidden.value + key else _hidden.value - key
        _hidden.value = next
        sp.edit { putString(K_HIDDEN, next.joinToString("\n")) }
    }

    fun recordLaunch(key: String) {
        val next = _usage.value.toMutableMap()
        next[key] = (next[key] ?: 0) + 1
        _usage.value = next
        sp.edit { putString(K_USAGE, next.entries.joinToString("\n") { "${it.key}\t${it.value}" }) }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        sp.edit {
            putString("style", next.style.name)
            putBoolean("daily_rotation", next.dailyRotation)
            putString("wallpaper_mode", next.wallpaperMode.name)
            putString("wallpaper_photo", next.wallpaperPhoto)
            putString("weather_place", next.weatherPlace?.encode())
            putInt("timer_minutes", next.timerMinutes)
            putBoolean("first_run_done", next.firstRunDone)
        }
    }

    // --- Tareas ---

    fun addTask(text: String) {
        val clean = text.replace('\n', ' ').replace('\t', ' ').trim()
        if (clean.isEmpty()) return
        saveTasks(_tasks.value + TaskItem(System.currentTimeMillis(), clean, false))
    }

    fun toggleTask(id: Long) = saveTasks(_tasks.value.map { if (it.id == id) it.copy(done = !it.done) else it })

    fun deleteTask(id: Long) = saveTasks(_tasks.value.filterNot { it.id == id })

    fun clearDoneTasks() = saveTasks(_tasks.value.filterNot { it.done })

    private fun saveTasks(list: List<TaskItem>) {
        _tasks.value = list
        sp.edit { putString(K_TASKS, list.joinToString("\n") { "${it.id}\t${if (it.done) 1 else 0}\t${it.text}" }) }
    }

    // --- Temporizador ---

    fun setTimer(state: TimerState) {
        _timer.value = state
        sp.edit {
            putLong("timer_duration", state.durationMs)
            putLong("timer_end", state.endAt)
            putLong("timer_remaining", state.remainingMs)
        }
    }

    /** Primera ejecución: rellena dock y favoritos con apps habituales que estén instaladas. */
    fun applyDefaultsIfNeeded(apps: List<AppEntry>) {
        if (_settings.value.firstRunDone || apps.isEmpty()) return
        val byPackage = apps.groupBy { it.packageName }
        fun pick(candidates: List<String>, max: Int, exclude: Set<String>): List<String> =
            candidates.mapNotNull { byPackage[it]?.firstOrNull()?.key }
                .filter { it !in exclude }
                .distinct()
                .take(max)

        val favorites = pick(FAVORITE_CANDIDATES, 9, emptySet())
        val dock = pick(DOCK_CANDIDATES, 7, emptySet())
        if (_favorites.value.isEmpty()) setList(AppListKind.FAVORITES, favorites)
        if (_dock.value.isEmpty()) setList(AppListKind.DOCK, dock)
        if (_tasks.value.isEmpty()) {
            saveTasks(listOf(TaskItem(1, "Mantén pulsada una tarea para borrarla", false)))
        }
        updateSettings { it.copy(firstRunDone = true) }
    }

    private fun readList(key: String): List<String> =
        sp.getString(key, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    private fun readUsage(): Map<String, Int> =
        readList(K_USAGE).mapNotNull { line ->
            val tab = line.lastIndexOf('\t')
            if (tab <= 0) null else line.substring(0, tab) to (line.substring(tab + 1).toIntOrNull() ?: 0)
        }.toMap()

    private fun readTasks(): List<TaskItem> =
        readList(K_TASKS).mapNotNull { line ->
            val parts = line.split('\t', limit = 3)
            if (parts.size != 3) null else TaskItem(parts[0].toLongOrNull() ?: return@mapNotNull null, parts[2], parts[1] == "1")
        }

    private fun readTimer(): TimerState {
        val minutes = sp.getInt("timer_minutes", 25)
        val duration = sp.getLong("timer_duration", minutes * 60_000L)
        val end = sp.getLong("timer_end", 0L)
        val remaining = sp.getLong("timer_remaining", duration)
        return TimerState(duration, end, remaining)
    }

    private fun readSettings() = LauncherSettings(
        style = LauncherStyle.entries.firstOrNull { it.name == sp.getString("style", null) } ?: LauncherStyle.CRISTAL,
        dailyRotation = sp.getBoolean("daily_rotation", false),
        wallpaperMode = WallpaperMode.entries.firstOrNull { it.name == sp.getString("wallpaper_mode", null) }
            ?: WallpaperMode.STYLE,
        wallpaperPhoto = sp.getString("wallpaper_photo", null),
        weatherPlace = WeatherPlace.decode(sp.getString("weather_place", null)),
        timerMinutes = sp.getInt("timer_minutes", 25),
        firstRunDone = sp.getBoolean("first_run_done", false),
    )

    companion object {
        @Volatile private var instance: LauncherPrefs? = null

        fun get(context: Context): LauncherPrefs =
            instance ?: synchronized(this) {
                instance ?: LauncherPrefs(context.applicationContext).also { instance = it }
            }

        private const val K_FAVORITES = "favorites"
        private const val K_DOCK = "dock"
        private const val K_HIDDEN = "hidden"
        private const val K_USAGE = "usage"
        private const val K_TASKS = "tasks"

        private val FAVORITE_CANDIDATES = listOf(
            "com.google.android.dialer",
            "com.android.chrome",
            "com.anthropic.claude",
            "org.jw.jwlibrary.mobile",
            "com.google.android.youtube",
            "com.google.android.apps.photos",
            "com.google.android.apps.maps",
            "com.android.settings",
            "com.google.android.calendar",
            "com.google.android.gm",
            "com.whatsapp",
            "com.google.android.keep",
            "com.google.android.deskclock",
            "com.android.vending",
        )

        private val DOCK_CANDIDATES = listOf(
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "com.google.android.calendar",
            "com.mediatek.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.google.android.keep",
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.android.vending",
            "com.google.android.apps.nbu.files",
        )
    }
}
