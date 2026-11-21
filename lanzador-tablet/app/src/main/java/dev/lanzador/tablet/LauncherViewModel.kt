package dev.lanzador.tablet

import android.app.AlarmManager
import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lanzador.tablet.data.AppRepository
import dev.lanzador.tablet.data.BatteryState
import dev.lanzador.tablet.data.LauncherPrefs
import dev.lanzador.tablet.data.WeatherClient
import dev.lanzador.tablet.data.WeatherUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)
    val prefs = LauncherPrefs(application)

    private val _weather = MutableStateFlow<WeatherUi>(WeatherUi.NotSet)
    val weather: StateFlow<WeatherUi> = _weather.asStateFlow()

    private val _battery = MutableStateFlow<BatteryState?>(null)
    val battery: StateFlow<BatteryState?> = _battery.asStateFlow()

    private val _nextAlarm = MutableStateFlow<Long?>(null)
    val nextAlarm: StateFlow<Long?> = _nextAlarm.asStateFlow()

    private val _darkText = MutableStateFlow(false)
    val darkTextOnWallpaper: StateFlow<Boolean> = _darkText.asStateFlow()

    private val _homePresses = MutableStateFlow(0)
    val homePresses: StateFlow<Int> = _homePresses.asStateFlow()

    private var weatherJob: Job? = null
    private var lastWeatherAt = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> _battery.value = BatteryState.from(intent)
                AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> refreshAlarm()
            }
        }
    }

    private val wallpaperListener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which and WallpaperManager.FLAG_SYSTEM != 0) updateWallpaperHint(colors)
    }

    init {
        repository.start()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        ContextCompat.registerReceiver(application, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.let { _battery.value = BatteryState.from(it) }
        refreshAlarm()

        runCatching {
            val wm = WallpaperManager.getInstance(application)
            wm.addOnColorsChangedListener(wallpaperListener, Handler(Looper.getMainLooper()))
            updateWallpaperHint(wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM))
        }

        viewModelScope.launch {
            repository.apps.collect { prefs.applyDefaultsIfNeeded(it) }
        }
        refreshWeather(force = true)
        viewModelScope.launch {
            prefs.settings.map { it.weatherPlace }.distinctUntilChanged().drop(1).collect {
                refreshWeather(force = true)
            }
        }
    }

    fun onResume() {
        refreshAlarm()
        refreshWeather(force = false)
    }

    fun onHomePressed() {
        _homePresses.value += 1
    }

    fun refreshWeather(force: Boolean) {
        val place = prefs.settings.value.weatherPlace
        if (place == null) {
            weatherJob?.cancel()
            _weather.value = WeatherUi.NotSet
            return
        }
        val fresh = SystemClock.elapsedRealtime() - lastWeatherAt < WEATHER_TTL_MS
        if (!force && (fresh || weatherJob?.isActive == true) && _weather.value !is WeatherUi.Error) return
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            val previous = (_weather.value as? WeatherUi.Ready)?.takeIf { it.data.place == place.name }
            if (previous == null) _weather.value = WeatherUi.Loading(place.name)
            _weather.value = try {
                WeatherUi.Ready(WeatherClient.current(place)).also { lastWeatherAt = SystemClock.elapsedRealtime() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                previous ?: WeatherUi.Error(place.name)
            }
        }
    }

    private fun refreshAlarm() {
        val am = getApplication<Application>().getSystemService(AlarmManager::class.java)
        _nextAlarm.value = am?.nextAlarmClock?.triggerTime
    }

    private fun updateWallpaperHint(colors: WallpaperColors?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || colors == null) return
        _darkText.value = colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    }

    override fun onCleared() {
        repository.stop()
        runCatching { getApplication<Application>().unregisterReceiver(receiver) }
        runCatching { WallpaperManager.getInstance(getApplication()).removeOnColorsChangedListener(wallpaperListener) }
    }

    private companion object {
        const val WEATHER_TTL_MS = 20 * 60 * 1000L
    }
}
