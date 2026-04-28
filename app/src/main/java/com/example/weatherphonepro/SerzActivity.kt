package com.example.weatherphonepro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

class SerzActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SerzConnectedApp() }
    }
}

data class SerzDashboardData(
    val weather: WeatherResult,
    val point: ExactForecastPoint,
    val scenarios: List<ScenarioAdvice>,
    val stats: ForecastErrorStats,
    val providerRows: List<ProviderComparisonRow>
)

@Composable
fun SerzConnectedApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF07111F)) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val favoriteStore = remember { FavoriteCityStore(context) }

            var city by remember { mutableStateOf("Омск") }
            var selectedTab by remember { mutableStateOf("Сейчас") }
            var favorites by remember { mutableStateOf(favoriteStore.getCities()) }
            var data by remember { mutableStateOf<SerzDashboardData?>(null) }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            fun loadCity(targetCity: String = city) {
                if (targetCity.trim().isEmpty()) {
                    error = "Введите город"
                    return
                }
                city = targetCity.trim()
                scope.launch {
                    loading = true
                    error = null
                    try {
                        data = fetchSerzDashboardByCity(context, city.trim())
                    } catch (e: Exception) {
                        data = null
                        error = e.message ?: "Не удалось загрузить прогноз"
                    } finally {
                        loading = false
                    }
                }
            }

            fun loadMyWeather() {
                val activity = context as? Activity
                if (!LocationAccuracyPlan.hasLocationPermission(context)) {
                    if (activity != null) {
                        LocationAccuracyPlan.requestLocationPermission(activity)
                        error = "Разрешите геолокацию и нажмите “Моя погода” ещё раз"
                    } else {
                        error = "Не удалось запросить разрешение геолокации"
                    }
                    return
                }

                val location = getLastKnownLocation(context)
                if (location == null) {
                    error = "Телефон пока не отдал координаты. Включите геолокацию и попробуйте ещё раз."
                    return
                }

                scope.launch {
                    loading = true
                    error = null
                    try {
                        data = fetchSerzDashboardByPoint(
                            context = context,
                            label = "Моя точка",
                            country = "GPS",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            source = "геолокация телефона"
                        )
                        city = "Моя точка"
                    } catch (e: Exception) {
                        data = null
                        error = e.message ?: "Не удалось загрузить прогноз по координатам"
                    } finally {
                        loading = false
                    }
                }
            }

            LaunchedEffect(Unit) { loadCity() }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedWeatherBackground(data?.weather?.current?.code ?: 1, data?.weather?.current?.isDay ?: true)
                Text(
                    text = "Serz",
                    color = Color.White.copy(alpha = 0.10f),
                    fontSize = 92.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { SerzHeader() }
                    item {
                        SerzSearchPanel(
                            city = city,
                            loading = loading,
                            onCityChange = { city = it },
                            onSearch = { loadCity() },
                            onMyWeather = { loadMyWeather() },
                            onSaveFavorite = {
                                favoriteStore.addCity(city)
                                favorites = favoriteStore.getCities()
                            }
                        )
                    }
                    if (favorites.isNotEmpty()) {
                        item { FavoriteCitiesRow(favorites, onSelect = { loadCity(it) }) }
                    }
                    item { SerzTabs(selectedTab, onSelect = { selectedTab = it }) }
                    if (loading) item { LoadingCard() }
                    error?.let { item { ErrorCard(it) } }

                    data?.let { dashboard ->
                        val weather = dashboard.weather
                        item { HeroCard(weather) }
                        item { ForecastPointCard(dashboard.point) }

                        when (selectedTab) {
                            "Сейчас" -> {
                                item { AdviceCard(weather.advice) }
                                item { MetricsGrid(weather) }
                                item { SectionTitle("Ближайшие 24 часа") }
                                item { HourlyRow(weather.hourly.take(24)) }
                            }
                            "Точность" -> {
                                item { AccuracyCard(weather.consensus) }
                                item { ProviderComparisonCard(dashboard.providerRows) }
                                item { LocalAccuracyMemoryCard(dashboard.stats) }
                            }
                            "Сценарии" -> {
                                item { ScenarioCards(dashboard.scenarios) }
                            }
                            "7 дней" -> {
                                item { SectionTitle("Прогноз на 7 дней") }
                                items(weather.daily) { DailyCard(it) }
                            }
                        }
                        item { FooterWatermark() }
                    }
                }
            }
        }
    }
}

@Composable
fun SerzHeader() {
    Column {
        Text("Суперпрогноз от Serz", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text("точная точка · несколько моделей · сценарии · память ошибок", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
    }
}

@Composable
fun SerzSearchPanel(
    city: String,
    loading: Boolean,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    onMyWeather: () -> Unit,
    onSaveFavorite: () -> Unit
) {
    SerzGlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Город или точка") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onSearch,
                    enabled = !loading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF70B7FF), contentColor = Color(0xFF06111F))
                ) { Text("Найти", fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMyWeather, enabled = !loading, shape = RoundedCornerShape(18.dp)) { Text("📍 Моя погода") }
                Button(onClick = onSaveFavorite, enabled = city.isNotBlank(), shape = RoundedCornerShape(18.dp)) { Text("★ В избранное") }
            }
        }
    }
}

@Composable
fun FavoriteCitiesRow(cities: List<String>, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.forEach { city ->
            Button(onClick = { onSelect(city) }, shape = RoundedCornerShape(18.dp)) {
                Text(city, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SerzTabs(selected: String, onSelect: (String) -> Unit) {
    val tabs = listOf("Сейчас", "Точность", "Сценарии", "7 дней")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val active = tab == selected
            Button(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Color(0xFFEFF6FF) else Color.White.copy(alpha = 0.18f),
                    contentColor = if (active) Color(0xFF0F3B66) else Color.White
                )
            ) { Text(tab, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ForecastPointCard(point: ExactForecastPoint) {
    SerzGlassCard {
        Column {
            Text("Точка прогноза", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(point.label, color = Color.White.copy(alpha = 0.82f), fontSize = 15.sp)
            Text("${point.latitude.formatCoord()}, ${point.longitude.formatCoord()} · ${point.source}", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        }
    }
}

@Composable
fun ProviderComparisonCard(rows: List<ProviderComparisonRow>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Сравнение источников", color = Color(0xFF7C2D12), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            rows.forEach { row ->
                Column {
                    Text(row.providerName, color = Color(0xFF7C2D12), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${row.temperature}° · осадки ${row.rainRisk}% · порывы ${row.windGust} км/ч", color = Color(0xFF9A3412), fontSize = 14.sp)
                    Text(row.confidenceNote, color = Color(0xFF9A3412).copy(alpha = 0.82f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LocalAccuracyMemoryCard(stats: ForecastErrorStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Локальная память точности", color = Color(0xFF14532D), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            if (stats.records == 0) {
                Text("Память только начала собираться. После нескольких запусков Serz будет видеть среднюю ошибку прогноза для этой точки.", color = Color(0xFF166534), fontSize = 14.sp, lineHeight = 20.sp)
            } else {
                Text("Сравнений: ${stats.records}", color = Color(0xFF166534), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Средняя ошибка температуры: ${stats.avgTempError.oneDigit()}°", color = Color(0xFF166534), fontSize = 14.sp)
                Text("Средняя ошибка ветра: ${stats.avgWindError.oneDigit()} км/ч", color = Color(0xFF166534), fontSize = 14.sp)
                Text("Средняя ошибка осадков: ${stats.avgRainError.oneDigit()}%", color = Color(0xFF166534), fontSize = 14.sp)
                Text("Поправка доверия: ${stats.trustCorrection}%", color = Color(0xFF166534), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ScenarioCards(scenarios: List<ScenarioAdvice>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Сценарии Serz")
        scenarios.forEach { scenario ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(scenario.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(scenario.verdict, color = Color(0xFFBDE7FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(scenario.details, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
fun SerzGlassCard(content: @Composable () -> Unit) = Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
) {
    Box(modifier = Modifier.fillMaxWidth().padding(17.dp)) { content() }
}

suspend fun fetchSerzDashboardByCity(context: Context, city: String): SerzDashboardData = withContext(Dispatchers.IO) {
    val encodedCity = URLEncoder.encode(city, "UTF-8")
    val geo = JSONObject(download("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=ru&format=json"))
    val results = geo.optJSONArray("results") ?: throw Exception("Город не найден")
    if (results.length() == 0) throw Exception("Город не найден")
    val place = results.getJSONObject(0)
    fetchSerzDashboardByPoint(
        context = context,
        label = place.optString("name", city),
        country = place.optString("country", ""),
        latitude = place.getDouble("latitude"),
        longitude = place.getDouble("longitude"),
        source = "центр найденного населённого пункта"
    )
}

suspend fun fetchSerzDashboardByPoint(
    context: Context,
    label: String,
    country: String,
    latitude: Double,
    longitude: Double,
    source: String
): SerzDashboardData = withContext(Dispatchers.IO) {
    val mainJson = JSONObject(download(forecastUrl(latitude, longitude, null)))
    val currentJson = mainJson.getJSONObject("current")
    val hourlyJson = mainJson.getJSONObject("hourly")

    val providerForecasts = mutableListOf<ProviderForecast>()
    providerForecasts += providerFromJson("Open-Meteo", mainJson)
    listOf("gfs_seamless", "icon_seamless").forEach { model ->
        try {
            providerForecasts += providerFromJson(model.uppercase(Locale.US), JSONObject(download(forecastUrl(latitude, longitude, model))))
        } catch (_: Exception) {
        }
    }

    val code = currentJson.getInt("weather_code")
    val info = weatherCode(code)
    val hours = buildHours(hourlyJson)
    val firstUv = hourlyJson.getJSONArray("uv_index").optDouble(0, 0.0)
    val firstVisibility = hourlyJson.getJSONArray("visibility").optDouble(0, 10000.0).roundToInt()
    val current = CurrentWeather(
        temp = currentJson.getDouble("temperature_2m").roundToInt(),
        feels = currentJson.getDouble("apparent_temperature").roundToInt(),
        humidity = currentJson.getInt("relative_humidity_2m"),
        pressure = currentJson.getDouble("pressure_msl").roundToInt(),
        wind = currentJson.getDouble("wind_speed_10m").roundToInt(),
        gusts = currentJson.getDouble("wind_gusts_10m").roundToInt(),
        direction = currentJson.getInt("wind_direction_10m"),
        clouds = currentJson.getInt("cloud_cover"),
        visibility = firstVisibility,
        uv = firstUv,
        code = code,
        isDay = currentJson.getInt("is_day") == 1,
        description = info.first,
        icon = info.second
    )
    val days = buildDays(mainJson.getJSONObject("daily"))
    val air = fetchAir(latitude, longitude)
    val consensus = buildConsensus(providerForecasts, current, hours)
    val weather = WeatherResult(label, country, current, hours, days, air, consensus, makeAdvice(current, hours, air, consensus))
    val maxRain = hours.take(12).maxOfOrNull { it.rain } ?: 0
    val memory = ForecastMemoryStore(context)
    val stats = memory.estimateStats(label, current.temp, maxRain, current.gusts)
    memory.saveForecast(label, current.temp, maxRain, current.gusts)

    SerzDashboardData(
        weather = weather,
        point = ExactForecastPoint(latitude, longitude, label, source),
        scenarios = ScenarioAdvisor.buildScenarios(current, hours, consensus),
        stats = stats,
        providerRows = ProviderWeightEngine.comparisonRows(providerForecasts)
    )
}

fun getLastKnownLocation(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null

    return try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        providers.mapNotNull { provider ->
            try { manager.getLastKnownLocation(provider) } catch (_: Exception) { null }
        }.maxByOrNull { it.time }
    } catch (_: Exception) {
        null
    }
}

fun Double.formatCoord(): String = String.format(Locale.US, "%.4f", this)
fun Double.oneDigit(): String = String.format(Locale.US, "%.1f", this)
