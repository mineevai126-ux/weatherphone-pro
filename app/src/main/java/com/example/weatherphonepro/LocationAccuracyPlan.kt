package com.example.weatherphonepro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object LocationAccuracyPlan {
    const val REQUEST_CODE = 8301

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_CODE
        )
    }
}

data class ExactForecastPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val source: String
)

data class ProviderWeight(
    val providerName: String,
    val baseWeight: Double,
    val localCorrection: Double,
    val finalWeight: Double
)

data class ProviderComparisonRow(
    val providerName: String,
    val temperature: Int,
    val rainRisk: Int,
    val windGust: Int,
    val confidenceNote: String
)

object ProviderWeightEngine {
    fun buildWeights(providers: List<ProviderForecast>, localStats: ForecastErrorStats?): List<ProviderWeight> {
        val correction = localStats?.trustCorrection ?: 0
        return providers.map { provider ->
            val base = when {
                provider.name.contains("ICON", ignoreCase = true) -> 1.10
                provider.name.contains("GFS", ignoreCase = true) -> 1.00
                provider.name.contains("Open", ignoreCase = true) -> 1.05
                else -> 0.90
            }
            val local = (100 + correction).coerceIn(70, 110) / 100.0
            ProviderWeight(provider.name, base, local, base * local)
        }
    }

    fun comparisonRows(providers: List<ProviderForecast>): List<ProviderComparisonRow> {
        return providers.map { provider ->
            ProviderComparisonRow(
                providerName = provider.name,
                temperature = provider.currentTemp,
                rainRisk = provider.nextRainRisk,
                windGust = provider.nextWindGust,
                confidenceNote = when {
                    provider.nextRainRisk >= 70 -> "показывает высокий риск осадков"
                    provider.nextRainRisk >= 40 -> "показывает средний риск осадков"
                    else -> "осадки маловероятны"
                }
            )
        }
    }
}
