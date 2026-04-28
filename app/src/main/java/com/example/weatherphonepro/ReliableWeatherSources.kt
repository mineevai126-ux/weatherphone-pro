package com.example.weatherphonepro

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

object ReliableWeatherSources {
    private val extraOpenMeteoModels = listOf(
        "gfs_seamless",
        "icon_seamless",
        "ecmwf_ifs025",
        "ukmo_seamless"
    )

    fun collectModelProviders(latitude: Double, longitude: Double): List<ProviderForecast> {
        val result = mutableListOf<ProviderForecast>()
        extraOpenMeteoModels.forEach { model ->
            try {
                val json = JSONObject(download(forecastUrl(latitude, longitude, model)))
                result += providerFromJson(modelTitle(model), json)
            } catch (_: Exception) {
            }
        }
        return result
    }

    fun fetchMetNorwayProvider(latitude: Double, longitude: Double): ProviderForecast? {
        return try {
            val json = JSONObject(downloadMetNorway(latitude, longitude))
            val series = json.getJSONObject("properties").getJSONArray("timeseries")
            val hours = mutableListOf<HourlyWeather>()
            for (i in 0 until minOf(series.length(), 48)) {
                val item = series.getJSONObject(i)
                val instant = item.getJSONObject("data").getJSONObject("instant").getJSONObject("details")
                val next = item.getJSONObject("data").optJSONObject("next_1_hours")
                val nextDetails = next?.optJSONObject("details")
                val symbol = next?.optJSONObject("summary")?.optString("symbol_code", "") ?: ""
                val precipitation = nextDetails?.optDouble("precipitation_amount", 0.0) ?: 0.0
                val temp = instant.optDouble("air_temperature", 0.0).roundToInt()
                val wind = (instant.optDouble("wind_speed", 0.0) * 3.6).roundToInt()
                val gust = (instant.optDouble("wind_speed_of_gust", instant.optDouble("wind_speed", 0.0)) * 3.6).roundToInt()
                val humidity = instant.optDouble("relative_humidity", 0.0).roundToInt().coerceIn(0, 100)
                val clouds = instant.optDouble("cloud_area_fraction", 0.0).roundToInt().coerceIn(0, 100)
                val pressure = instant.optDouble("air_pressure_at_sea_level", 1013.0).roundToInt()
                val time = item.getString("time").substringAfter("T").substring(0, 5)
                hours += HourlyWeather(time, temp, estimateMetRisk(precipitation, clouds, humidity, symbol), precipitation, pressure, humidity, clouds, wind, gust, metIcon(symbol))
            }
            val first = hours.firstOrNull() ?: return null
            ProviderForecast(
                name = "MET Norway",
                currentTemp = first.temp,
                nextRainRisk = hours.take(12).maxOfOrNull { it.rain } ?: first.rain,
                nextWindGust = hours.take(12).maxOfOrNull { it.gusts } ?: first.gusts,
                nextPressure = first.pressure,
                hourly = hours
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun modelTitle(model: String): String = when (model) {
        "gfs_seamless" -> "GFS"
        "icon_seamless" -> "ICON"
        "ecmwf_ifs025" -> "ECMWF IFS"
        "ukmo_seamless" -> "UKMO"
        else -> model.uppercase(Locale.US)
    }

    private fun estimateMetRisk(amount: Double, clouds: Int, humidity: Int, symbol: String): Int {
        val bySymbol = when {
            symbol.contains("heavyrain", true) -> 90
            symbol.contains("thunder", true) -> 85
            symbol.contains("rain", true) -> 75
            symbol.contains("sleet", true) -> 70
            symbol.contains("snow", true) -> 65
            symbol.contains("showers", true) -> 60
            else -> 0
        }
        val byAmount = when {
            amount >= 3.0 -> 90
            amount >= 1.0 -> 75
            amount >= 0.3 -> 55
            amount > 0.0 -> 35
            clouds > 80 && humidity > 80 -> 30
            clouds > 65 && humidity > 70 -> 20
            else -> 0
        }
        return maxOf(bySymbol, byAmount).coerceIn(0, 100)
    }

    private fun metIcon(symbol: String): String = when {
        symbol.contains("snow", true) -> "❄️"
        symbol.contains("rain", true) -> "🌧️"
        symbol.contains("thunder", true) -> "⛈️"
        symbol.contains("fog", true) -> "🌫️"
        symbol.contains("cloud", true) -> "☁️"
        symbol.contains("fair", true) -> "🌤️"
        symbol.contains("clear", true) -> "☀️"
        else -> "🌡️"
    }

    private fun downloadMetNorway(latitude: Double, longitude: Double): String {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$latitude&lon=$longitude"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("User-Agent", "SerzWeatherPhonePro/1.0")
        return try {
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (code !in 200..299) throw Exception("MET Norway error: $code")
            text
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchSerzDashboardByCityReliable(context: Context, city: String): SerzDashboardData = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(city, "UTF-8")
    val geo = JSONObject(download("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json"))
    val results = geo.optJSONArray("results") ?: throw Exception("Город не найден")
    if (results.length() == 0) throw Exception("Город не найден")
    val place = results.getJSONObject(0)
    fetchSerzDashboardByPointReliable(context, place.optString("name", city), place.optString("country", ""), place.getDouble("latitude"), place.getDouble("longitude"), "центр найденного населённого пункта")
}

suspend fun fetchSerzDashboardByPointReliable(context: Context, label: String, country: String, latitude: Double, longitude: Double, source: String): SerzDashboardData = withContext(Dispatchers.IO) {
    val mainJson = JSONObject(download(forecastUrl(latitude, longitude, null)))
    val currentJson = mainJson.getJSONObject("current")
    val hourlyJson = mainJson.getJSONObject("hourly")
    val providers = mutableListOf<ProviderForecast>()
    providers += providerFromJson("Open-Meteo Best Match", mainJson)
    providers += ReliableWeatherSources.collectModelProviders(latitude, longitude)
    ReliableWeatherSources.fetchMetNorwayProvider(latitude, longitude)?.let { providers += it }

    val code = currentJson.getInt("weather_code")
    val info = weatherCode(code)
    val hours = buildHours(hourlyJson)
    val current = CurrentWeather(
        temp = currentJson.getDouble("temperature_2m").roundToInt(),
        feels = currentJson.getDouble("apparent_temperature").roundToInt(),
        humidity = currentJson.getInt("relative_humidity_2m"),
        pressure = currentJson.getDouble("pressure_msl").roundToInt(),
        wind = currentJson.getDouble("wind_speed_10m").roundToInt(),
        gusts = currentJson.getDouble("wind_gusts_10m").roundToInt(),
        direction = currentJson.getInt("wind_direction_10m"),
        clouds = currentJson.getInt("cloud_cover"),
        visibility = hourlyJson.getJSONArray("visibility").optDouble(0, 10000.0).roundToInt(),
        uv = hourlyJson.getJSONArray("uv_index").optDouble(0, 0.0),
        code = code,
        isDay = currentJson.getInt("is_day") == 1,
        description = info.first,
        icon = info.second
    )
    val days = buildDays(mainJson.getJSONObject("daily"))
    val air = fetchAir(latitude, longitude)
    val consensus = buildConsensus(providers, current, hours)
    val weather = WeatherResult(label, country, current, hours, days, air, consensus, makeAdvice(current, hours, air, consensus))
    val maxRain = hours.take(12).maxOfOrNull { it.rain } ?: 0
    val memory = ForecastMemoryStore(context)
    val stats = memory.estimateStats(label, current.temp, maxRain, current.gusts)
    memory.saveForecast(label, current.temp, maxRain, current.gusts)
    SerzDashboardData(weather, ExactForecastPoint(latitude, longitude, label, source), ScenarioAdvisor.buildScenarios(current, hours, consensus), stats, ProviderWeightEngine.comparisonRows(providers))
}
