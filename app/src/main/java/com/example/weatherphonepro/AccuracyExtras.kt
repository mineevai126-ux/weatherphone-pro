package com.example.weatherphonepro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

data class ScenarioAdvice(
    val title: String,
    val verdict: String,
    val details: String
)

data class ForecastMemoryRecord(
    val city: String,
    val timestampMillis: Long,
    val forecastTemp: Int,
    val forecastRainRisk: Int,
    val forecastWind: Int,
    val actualTemp: Int? = null,
    val actualRainRisk: Int? = null,
    val actualWind: Int? = null
)

data class ForecastErrorStats(
    val records: Int,
    val avgTempError: Double,
    val avgWindError: Double,
    val avgRainError: Double,
    val trustCorrection: Int
)

object ScenarioAdvisor {
    fun buildScenarios(current: CurrentWeather, hours: List<HourlyWeather>, consensus: ForecastConsensus): List<ScenarioAdvice> {
        val next = hours.take(12)
        val maxRain = next.maxOfOrNull { it.rain } ?: 0
        val maxGust = next.maxOfOrNull { it.gusts } ?: current.gusts
        val minFeels = minOf(current.feels, next.minOfOrNull { it.temp } ?: current.feels)
        val maxTemp = maxOf(current.temp, next.maxOfOrNull { it.temp } ?: current.temp)

        return listOf(
            ScenarioAdvice(
                title = "Прогулка",
                verdict = when {
                    maxRain >= 70 -> "лучше перенести"
                    maxRain >= 40 || maxGust >= 40 -> "можно, но с осторожностью"
                    else -> "хорошее окно для прогулки"
                },
                details = "Риск осадков до $maxRain%, порывы до $maxGust км/ч, надёжность ${consensus.confidence}%."
            ),
            ScenarioAdvice(
                title = "Дорога",
                verdict = when {
                    maxRain >= 70 || maxGust >= 55 -> "закладывайте запас времени"
                    maxRain >= 40 -> "возможны мокрые участки"
                    else -> "условия спокойные"
                },
                details = "Окно риска: ${consensus.rainWindow}. Ветер и осадки учтены по ближайшим 12 часам."
            ),
            ScenarioAdvice(
                title = "Дача / сад",
                verdict = when {
                    maxRain >= 60 -> "полив, скорее всего, не нужен"
                    maxTemp >= 27 && maxRain < 30 -> "полив лучше вечером"
                    else -> "условия умеренные"
                },
                details = "Температура до ${maxTemp}°, осадки до $maxRain%, влажность ${current.humidity}%."
            ),
            ScenarioAdvice(
                title = "Ребёнок на улице",
                verdict = when {
                    minFeels <= -10 -> "нужно сильно утеплить"
                    maxGust >= 45 -> "лучше короткая прогулка"
                    maxRain >= 50 -> "нужна непромокаемая одежда"
                    else -> "условия нормальные"
                },
                details = "Ощущается минимум ${minFeels}°, порывы до $maxGust км/ч."
            ),
            ScenarioAdvice(
                title = "Одежда",
                verdict = when {
                    current.feels <= -10 -> "зимний комплект"
                    current.feels <= 5 -> "тёплая куртка"
                    current.feels <= 15 -> "лёгкая куртка"
                    maxRain >= 40 -> "добавить зонт или мембрану"
                    else -> "лёгкая одежда по сезону"
                },
                details = "Ощущается как ${current.feels}°, риск осадков ${consensus.rainRisk}."
            )
        )
    }
}

class FavoriteCityStore(context: Context) {
    private val prefs = context.getSharedPreferences("serz_favorite_cities", Context.MODE_PRIVATE)

    fun getCities(): List<String> {
        val raw = prefs.getString("cities", "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun addCity(city: String) {
        val normalized = city.trim()
        if (normalized.isEmpty()) return
        val updated = (listOf(normalized) + getCities()).distinct().take(8)
        prefs.edit().putString("cities", JSONArray(updated).toString()).apply()
    }

    fun removeCity(city: String) {
        val updated = getCities().filterNot { it.equals(city, ignoreCase = true) }
        prefs.edit().putString("cities", JSONArray(updated).toString()).apply()
    }
}

class ForecastMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("serz_forecast_memory", Context.MODE_PRIVATE)

    fun saveForecast(city: String, temp: Int, rainRisk: Int, wind: Int) {
        val records = readRecords().toMutableList()
        records.add(
            ForecastMemoryRecord(
                city = city,
                timestampMillis = System.currentTimeMillis(),
                forecastTemp = temp,
                forecastRainRisk = rainRisk,
                forecastWind = wind
            )
        )
        writeRecords(records.takeLast(80))
    }

    fun estimateStats(city: String, actualTemp: Int, actualRainRisk: Int, actualWind: Int): ForecastErrorStats {
        val comparable = readRecords()
            .filter { it.city.equals(city, ignoreCase = true) }
            .takeLast(20)

        if (comparable.isEmpty()) {
            return ForecastErrorStats(0, 0.0, 0.0, 0.0, 0)
        }

        val tempError = comparable.map { abs(it.forecastTemp - actualTemp).toDouble() }.average()
        val windError = comparable.map { abs(it.forecastWind - actualWind).toDouble() }.average()
        val rainError = comparable.map { abs(it.forecastRainRisk - actualRainRisk).toDouble() }.average()
        val penalty = (tempError * 2 + windError * 0.6 + rainError * 0.25).roundToInt().coerceIn(0, 25)

        return ForecastErrorStats(
            records = comparable.size,
            avgTempError = tempError,
            avgWindError = windError,
            avgRainError = rainError,
            trustCorrection = -penalty
        )
    }

    private fun readRecords(): List<ForecastMemoryRecord> {
        val raw = prefs.getString("records", "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            ForecastMemoryRecord(
                city = item.getString("city"),
                timestampMillis = item.getLong("timestampMillis"),
                forecastTemp = item.getInt("forecastTemp"),
                forecastRainRisk = item.getInt("forecastRainRisk"),
                forecastWind = item.getInt("forecastWind")
            )
        }
    }

    private fun writeRecords(records: List<ForecastMemoryRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("city", record.city)
                    .put("timestampMillis", record.timestampMillis)
                    .put("forecastTemp", record.forecastTemp)
                    .put("forecastRainRisk", record.forecastRainRisk)
                    .put("forecastWind", record.forecastWind)
            )
        }
        prefs.edit().putString("records", array.toString()).apply()
    }
}
