package dev.lanzador.tablet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Cliente mínimo de Open-Meteo (https://open-meteo.com). No necesita clave;
 * el uso no comercial es gratuito y los datos son CC BY 4.0.
 */
object WeatherClient {

    suspend fun searchPlaces(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=es&format=json"))
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            val name = o.optString("name")
            val detail = listOf(o.optString("admin1"), o.optString("country"))
                .filter { it.isNotBlank() && it != name }
                .joinToString(", ")
            WeatherPlace(name, detail, o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    suspend fun current(place: WeatherPlace): WeatherNow = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,weather_code,is_day" +
            "&hourly=temperature_2m,weather_code,is_day&forecast_hours=9" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=1"
        val json = JSONObject(get(url))
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val hourly = json.optJSONObject("hourly")
        val forecast = if (hourly == null) emptyList() else {
            val times = hourly.getJSONArray("time")
            val temps = hourly.getJSONArray("temperature_2m")
            val codes = hourly.getJSONArray("weather_code")
            val days = hourly.optJSONArray("is_day")
            (0 until times.length()).mapNotNull { i ->
                val hour = times.getString(i).substringAfter('T').take(2).toIntOrNull() ?: return@mapNotNull null
                HourForecast(hour, temps.getDouble(i).roundToInt(), codes.getInt(i), (days?.optInt(i, 1) ?: 1) == 1)
            }
        }
        WeatherNow(
            place = place.name,
            temperature = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            max = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            min = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            hourly = forecast,
        )
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Códigos WMO que devuelve Open-Meteo. */
fun weatherDescription(code: Int): String = when (code) {
    0 -> "Despejado"
    1 -> "Casi despejado"
    2 -> "Nubes y claros"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"
    in 61..67 -> "Lluvia"
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    85, 86 -> "Chubascos de nieve"
    in 95..99 -> "Tormenta"
    else -> "Sin datos"
}
