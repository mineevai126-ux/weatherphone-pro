package com.example.weatherphonepro

import android.content.Context
import android.widget.RemoteViews
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import kotlin.math.abs
import kotlin.math.roundToInt

data class SerzMainVerdict(
    val title: String,
    val subtitle: String,
    val umbrella: String,
    val clothes: String,
    val road: String,
    val confidence: Int
)

data class SeparateAccuracy(
    val temperature: Int,
    val precipitation: Int,
    val wind: Int,
    val pressure: Int,
    val summary: String
)

data class HazardWarning(
    val title: String,
    val level: String,
    val details: String
)

data class SerzActionCard(
    val title: String,
    val decision: String,
    val reason: String
)

data class SerzMaxBundle(
    val verdict: SerzMainVerdict,
    val separateAccuracy: SeparateAccuracy,
    val hazards: List<HazardWarning>,
    val actions: List<SerzActionCard>,
    val comfort: List<HourlyComfortPoint>,
    val rainMap: RainMapState
)

object SerzMaxEngine {
    fun build(weather: WeatherResult, point: ExactForecastPoint, providers: List<ProviderComparisonRow>): SerzMaxBundle {
        val hours = weather.hourly.take(24)
        val next12 = weather.hourly.take(12)
        val maxRain = next12.maxOfOrNull { it.rain } ?: 0
        val maxGust = next12.maxOfOrNull { it.gusts } ?: weather.current.gusts
        val minTemp = next12.minOfOrNull { it.temp } ?: weather.current.temp
        val maxTemp = next12.maxOfOrNull { it.temp } ?: weather.current.temp
        val avgProviderRain = if (providers.isEmpty()) maxRain else providers.map { it.rainRisk }.average().roundToInt()
        val providerRainSpread = if (providers.isEmpty()) 0 else providers.maxOf { it.rainRisk } - providers.minOf { it.rainRisk }
        val providerTempSpread = if (providers.isEmpty()) 0 else providers.maxOf { it.temperature } - providers.minOf { it.temperature }
        val confidence = weather.consensus.confidence

        val verdictTitle = when {
            maxRain >= 70 -> "День с высоким риском осадков"
            maxGust >= 55 -> "Главный риск дня — сильный ветер"
            weather.current.uv >= 6 -> "Хороший день, но высокий UV"
            confidence < 60 -> "Прогноз спорный, лучше перестраховаться"
            else -> "Погода в целом спокойная"
        }

        val verdictSubtitle = when {
            maxRain >= 70 -> "Serz видит окно осадков: ${weather.consensus.rainWindow}. Лучше планировать дела с запасом."
            confidence < 60 -> "Модели расходятся, поэтому Serz снижает доверие и показывает осторожный вывод."
            else -> "Serz сравнил источники, осадки и ветер. Критичных сигналов на ближайшие часы мало."
        }

        val separate = SeparateAccuracy(
            temperature = (95 - providerTempSpread * 8).coerceIn(45, 98),
            precipitation = (92 - providerRainSpread).coerceIn(35, 96),
            wind = (90 - abs(maxGust - weather.current.gusts)).coerceIn(40, 95),
            pressure = if (weather.current.pressure in 995..1035) 88 else 72,
            summary = "Температура зависит от разброса моделей, осадки — от согласия источников, ветер — от порывов в ближайшие часы."
        )

        val hazards = buildList {
            if (maxRain >= 60) add(HazardWarning("Осадки", if (maxRain >= 80) "высокий" else "средний", "Риск до $maxRain%. Окно: ${weather.consensus.rainWindow}."))
            if (maxGust >= 45) add(HazardWarning("Ветер", if (maxGust >= 60) "высокий" else "средний", "Порывы до $maxGust км/ч. На улице может быть некомфортно."))
            if (minTemp in -3..2 && maxRain >= 35) add(HazardWarning("Гололёд", "возможен", "Температура около нуля и есть риск осадков."))
            if (weather.current.uv >= 6) add(HazardWarning("UV", "высокий", "UV ${one(weather.current.uv)}. Нужны очки и SPF."))
            if ((weather.air?.aqi ?: 0) >= 80) add(HazardWarning("Воздух", "снижен", "AQI ${weather.air?.aqi}. Долгие интенсивные прогулки лучше ограничить."))
            if (weather.current.pressure < 995 || weather.current.pressure > 1035) add(HazardWarning("Давление", "нестандартное", "Давление ${weather.current.pressure} гПа. Возможна метеочувствительность."))
        }

        val actions = listOf(
            SerzActionCard("Зонт", when { maxRain >= 70 -> "обязательно"; maxRain >= 40 -> "лучше взять"; else -> "не обязателен" }, "Риск осадков до $maxRain%, средний по моделям около $avgProviderRain%."),
            SerzActionCard("Одежда", when { weather.current.feels <= -10 -> "зимний комплект"; weather.current.feels <= 5 -> "тёплая куртка"; maxRain >= 45 -> "мембрана или капюшон"; else -> "по сезону" }, "Ощущается как ${weather.current.feels}°, ветер до $maxGust км/ч."),
            SerzActionCard("Дорога", when { maxRain >= 70 || maxGust >= 55 -> "заложить запас"; maxRain >= 40 -> "быть внимательнее"; else -> "спокойно" }, "Serz учитывает осадки, порывы и видимость."),
            SerzActionCard("Проветривание", when { (weather.air?.aqi ?: 0) >= 80 -> "лучше коротко"; maxRain >= 70 -> "после дождя"; else -> "можно" }, "Воздух: AQI ${weather.air?.aqi ?: 0}, влажность ${weather.current.humidity}%.")
        )

        return SerzMaxBundle(
            verdict = SerzMainVerdict(verdictTitle, verdictSubtitle, actions[0].decision, actions[1].decision, actions[2].decision, confidence),
            separateAccuracy = separate,
            hazards = hazards,
            actions = actions,
            comfort = SerzWowEngine.buildHourlyComfort(hours),
            rainMap = SerzWowEngine.buildRainMapState(point)
        )
    }
}

object SerzWidgetUpdater {
    fun saveAndUpdate(context: Context, weather: WeatherResult) {
        val prefs = context.getSharedPreferences("serz_widget_state", Context.MODE_PRIVATE)
        val rain = weather.hourly.take(12).maxOfOrNull { it.rain } ?: 0
        prefs.edit()
            .putString("city", weather.city)
            .putString("temp", "${weather.current.temp}°")
            .putString("note", "осадки $rain% · точность ${weather.consensus.confidence}%")
            .apply()

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SerzWeatherWidgetProvider::class.java))
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.serz_weather_widget)
            views.setTextViewText(R.id.serz_widget_title, weather.city)
            views.setTextViewText(R.id.serz_widget_temp, "${weather.current.temp}° · ${weather.current.description}")
            views.setTextViewText(R.id.serz_widget_note, "осадки $rain% · точность ${weather.consensus.confidence}%")
            manager.updateAppWidget(id, views)
        }
    }
}

object SerzSettingsStore {
    fun saveDefaultCity(context: Context, city: String) {
        context.getSharedPreferences("serz_settings", Context.MODE_PRIVATE).edit().putString("default_city", city).apply()
    }

    fun defaultCity(context: Context): String {
        return context.getSharedPreferences("serz_settings", Context.MODE_PRIVATE).getString("default_city", "Омск") ?: "Омск"
    }

    fun setQuietNight(context: Context, enabled: Boolean) {
        context.getSharedPreferences("serz_settings", Context.MODE_PRIVATE).edit().putBoolean("quiet_night", enabled).apply()
    }

    fun quietNight(context: Context): Boolean {
        return context.getSharedPreferences("serz_settings", Context.MODE_PRIVATE).getBoolean("quiet_night", true)
    }
}
