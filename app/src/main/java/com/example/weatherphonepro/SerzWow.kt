package com.example.weatherphonepro

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

data class HourlyComfortPoint(
    val time: String,
    val comfort: Int,
    val wind: Int,
    val rain: Int,
    val label: String
)

data class RainMapState(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val subtitle: String,
    val rainViewerUrl: String
)

object SerzWowEngine {
    fun confidenceColorName(confidence: Int): String = when {
        confidence >= 80 -> "зелёный"
        confidence >= 60 -> "жёлтый"
        else -> "красный"
    }

    fun confidenceLabel(confidence: Int): String = when {
        confidence >= 85 -> "очень высокая"
        confidence >= 70 -> "хорошая"
        confidence >= 55 -> "средняя"
        else -> "низкая"
    }

    fun buildHourlyComfort(hours: List<HourlyWeather>): List<HourlyComfortPoint> {
        return hours.take(24).map { hour ->
            val rainPenalty = hour.rain * 0.55
            val windPenalty = hour.gusts * 0.65
            val tempPenalty = when {
                hour.temp < -15 -> 35.0
                hour.temp < -5 -> 22.0
                hour.temp < 5 -> 12.0
                hour.temp > 31 -> 30.0
                hour.temp > 26 -> 14.0
                else -> 0.0
            }
            val comfort = (100 - rainPenalty - windPenalty - tempPenalty).roundToInt().coerceIn(0, 100)
            val label = when {
                comfort >= 80 -> "комфортно"
                comfort >= 60 -> "нормально"
                comfort >= 40 -> "спорно"
                else -> "неуютно"
            }
            HourlyComfortPoint(hour.time, comfort, hour.gusts.coerceIn(0, 100), hour.rain.coerceIn(0, 100), label)
        }
    }

    fun buildRainMapState(point: ExactForecastPoint): RainMapState {
        val url = "https://www.rainviewer.com/map.html?loc=${point.latitude},${point.longitude},7&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=1&layer=radar&sm=1&sn=1"
        return RainMapState(
            latitude = point.latitude,
            longitude = point.longitude,
            title = "Карта осадков Serz",
            subtitle = "Координаты ${point.latitude.formatCoord()}, ${point.longitude.formatCoord()}. Следующий этап — встроить живой радар RainViewer/OpenStreetMap прямо в экран.",
            rainViewerUrl = url
        )
    }
}

object SerzRainNotifier {
    private const val CHANNEL_ID = "serz_weather_alerts"
    private const val NOTIFICATION_ID = 2401

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Serz Weather Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Предупреждения Serz о дожде, ветре и резкой погоде"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun canNotify(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyRainRisk(context: Context, weather: WeatherResult) {
        ensureChannel(context)
        if (!canNotify(context)) return
        val riskyHour = weather.hourly.firstOrNull { it.rain >= 60 || it.precipitation > 0.2 } ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Суперпрогноз от Serz")
            .setContentText("Возможен дождь около ${riskyHour.time}. Риск ${riskyHour.rain}%. Лучше взять зонт.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Возможен дождь около ${riskyHour.time}. Риск ${riskyHour.rain}%. Окно риска: ${weather.consensus.rainWindow}. Надёжность прогноза ${weather.consensus.confidence}%."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

object SerzPaidProviderPlan {
    val providerNames = listOf("OpenWeather", "WeatherAPI", "Tomorrow.io", "Visual Crossing")

    fun description(): String {
        return "Платные источники будут подключаться опционально через ключи. Бесплатная версия продолжит работать на Open-Meteo, GFS и ICON."
    }
}
