package dev.lanzador.tablet.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppListKind { FAVORITES, DOCK }

/** Favoritos, dock, apps ocultas, nota y ajustes. Todo en SharedPreferences. */
class LauncherPrefs(context: Context) {

    private val sp = context.getSharedPreferences("lanzador", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readList(K_FAVORITES))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _dock = MutableStateFlow(readList(K_DOCK))
    val dock: StateFlow<List<String>> = _dock.asStateFlow()

    private val _hidden = MutableStateFlow(readList(K_HIDDEN).toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _usage = MutableStateFlow(readUsage())
    val usage: StateFlow<Map<String, Int>> = _usage.asStateFlow()

    private val _note = MutableStateFlow(sp.getString(K_NOTE, "").orEmpty())
    val note: StateFlow<String> = _note.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

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

    /**
     * Mueve [key] una posición entre las apps que se ven. Se trabaja sobre las
     * claves visibles para que una app desinstalada guardada en la lista no
     * convierta el movimiento en un intercambio invisible.
     */
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

    fun setNote(text: String) {
        _note.value = text
        sp.edit { putString(K_NOTE, text) }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        sp.edit {
            putString("icon_size", next.iconSize.name)
            putString("theme", next.theme.name)
            putBoolean("show_weather", next.showWeather)
            putBoolean("show_note", next.showNote)
            putBoolean("show_quick", next.showQuickSettings)
            putString("weather_place", next.weatherPlace?.encode())
            putBoolean("first_run_done", next.firstRunDone)
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

        val dock = pick(DOCK_CANDIDATES, 6, emptySet())
        val favorites = pick(FAVORITE_CANDIDATES, 10, dock.toSet())
        if (_dock.value.isEmpty()) setList(AppListKind.DOCK, dock)
        if (_favorites.value.isEmpty()) setList(AppListKind.FAVORITES, favorites)
        updateSettings { it.copy(firstRunDone = true) }
    }

    private fun readList(key: String): List<String> =
        sp.getString(key, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    private fun readUsage(): Map<String, Int> =
        readList(K_USAGE).mapNotNull { line ->
            val tab = line.lastIndexOf('\t')
            if (tab <= 0) null else line.substring(0, tab) to (line.substring(tab + 1).toIntOrNull() ?: 0)
        }.toMap()

    private fun readSettings() = LauncherSettings(
        iconSize = IconSize.entries.firstOrNull { it.name == sp.getString("icon_size", null) } ?: IconSize.MEDIUM,
        theme = ThemeMode.entries.firstOrNull { it.name == sp.getString("theme", null) } ?: ThemeMode.SYSTEM,
        showWeather = sp.getBoolean("show_weather", true),
        showNote = sp.getBoolean("show_note", true),
        showQuickSettings = sp.getBoolean("show_quick", true),
        weatherPlace = WeatherPlace.decode(sp.getString("weather_place", null)),
        firstRunDone = sp.getBoolean("first_run_done", false),
    )

    private companion object {
        const val K_FAVORITES = "favorites"
        const val K_DOCK = "dock"
        const val K_HIDDEN = "hidden"
        const val K_USAGE = "usage"
        const val K_NOTE = "note"

        val DOCK_CANDIDATES = listOf(
            "com.android.chrome",
            "com.anthropic.claude",
            "org.jw.jwlibrary.mobile",
            "com.google.android.dialer",
            "com.android.vending",
            "com.android.settings",
        )

        val FAVORITE_CANDIDATES = listOf(
            "com.google.android.youtube",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.calendar",
            "com.google.android.apps.maps",
            "com.google.android.keep",
            "com.google.android.apps.docs",
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "com.google.android.contacts",
            "com.mediatek.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.google.android.apps.nbu.files",
            "com.google.android.documentsui",
        )
    }
}
