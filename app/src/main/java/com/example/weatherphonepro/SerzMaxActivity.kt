package com.example.weatherphonepro

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.size
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
import kotlinx.coroutines.launch

data class SerzMaxDashboardData(
    val dashboard: SerzDashboardData,
    val max: SerzMaxBundle
)

class SerzMaxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 8302)
        }
        setContent { SerzMaxConnectedApp() }
    }
}

@Composable
fun SerzMaxConnectedApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF07111F)) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val favoriteStore = remember { FavoriteCityStore(context) }
            var city by remember { mutableStateOf(SerzSettingsStore.defaultCity(context)) }
            var selectedTab by remember { mutableStateOf("Главная") }
            var favorites by remember { mutableStateOf(favoriteStore.getCities()) }
            var data by remember { mutableStateOf<SerzMaxDashboardData?>(null) }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            fun setDashboard(base: SerzDashboardData) {
                SerzWidgetUpdater.saveAndUpdate(context, base.weather)
                data = SerzMaxDashboardData(base, SerzMaxEngine.build(base.weather, base.point, base.providerRows))
            }

            fun loadCity(targetCity: String = city) {
                if (targetCity.trim().isEmpty()) {
                    error = "Введите город"
                    return
                }
                city = targetCity.trim()
                scope.launch {
                    loading = true
                    error = null
                    try { setDashboard(fetchSerzDashboardByCity(context, city.trim())) }
                    catch (e: Exception) { data = null; error = e.message ?: "Не удалось загрузить прогноз" }
                    finally { loading = false }
                }
            }

            fun loadMyWeather() {
                val activity = context as? Activity
                if (!LocationAccuracyPlan.hasLocationPermission(context)) {
                    if (activity != null) {
                        LocationAccuracyPlan.requestLocationPermission(activity)
                        error = "Разрешите геолокацию и нажмите “Моя погода” ещё раз"
                    } else error = "Не удалось запросить разрешение геолокации"
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
                        setDashboard(fetchSerzDashboardByPoint(context, "Моя точка", "GPS", location.latitude, location.longitude, "геолокация телефона"))
                        city = "Моя точка"
                    } catch (e: Exception) {
                        data = null
                        error = e.message ?: "Не удалось загрузить прогноз по координатам"
                    } finally { loading = false }
                }
            }

            LaunchedEffect(Unit) { loadCity() }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedWeatherBackground(data?.dashboard?.weather?.current?.code ?: 1, data?.dashboard?.weather?.current?.isDay ?: true)
                Text("Serz", color = Color.White.copy(alpha = 0.08f), fontSize = 86.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Center))

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { SerzMaxHeader() }
                    item {
                        SerzCompactSearchPanel(
                            city = city,
                            loading = loading,
                            onCityChange = { city = it },
                            onSearch = { loadCity() },
                            onMyWeather = { loadMyWeather() },
                            onSaveFavorite = {
                                favoriteStore.addCity(city)
                                SerzSettingsStore.saveDefaultCity(context, city)
                                favorites = favoriteStore.getCities()
                            }
                        )
                    }
                    if (favorites.isNotEmpty()) item { FavoriteCitiesRow(favorites, onSelect = { loadCity(it) }) }
                    item { SerzMaxTabs(selectedTab, onSelect = { selectedTab = it }) }
                    if (loading) item { LoadingCard() }
                    error?.let { item { ErrorCard(it) } }

                    data?.let { pack ->
                        val dashboard = pack.dashboard
                        val weather = dashboard.weather
                        val max = pack.max
                        item { SerzCompactHeroCard(weather) }
                        item { ForecastPointCard(dashboard.point) }
                        when (selectedTab) {
                            "Главная" -> {
                                item { SerzVerdictCard(max.verdict) }
                                item { SerzActionsCard(max.actions) }
                                item { HazardWarningsCard(max.hazards) }
                                item { HourlyComfortScaleCard(max.comfort) }
                            }
                            "Точность" -> {
                                item { RichAccuracyCard(weather.consensus) }
                                item { SeparateAccuracyCard(max.separateAccuracy) }
                                item { ProviderComparisonCard(dashboard.providerRows) }
                                item { LocalAccuracyMemoryCard(dashboard.stats) }
                                item { PaidProvidersCard() }
                            }
                            "Сценарии" -> item { ScenarioCards(dashboard.scenarios) }
                            "Карта" -> {
                                item { RainMapPreviewCard(max.rainMap) }
                                item { RainAlertButton(context, weather) }
                            }
                            "7 дней" -> {
                                item { SectionTitle("Прогноз на 7 дней") }
                                items(weather.daily) { DailyCard(it) }
                            }
                            "Настройки" -> item { SettingsPreviewCard(SerzSettingsStore.defaultCity(context), SerzSettingsStore.quietNight(context)) }
                        }
                        item { FooterWatermark() }
                    }
                }
            }
        }
    }
}

@Composable
fun SerzMaxHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeatherIconVector(1, modifier = Modifier.size(38.dp))
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text("Суперпрогноз от Serz", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 28.sp)
            Text("MAX · риски · точность · радар · виджет", color = Color.White.copy(alpha = 0.74f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SerzCompactSearchPanel(
    city: String,
    loading: Boolean,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    onMyWeather: () -> Unit,
    onSaveFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Город") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSearch,
                    enabled = !loading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF70B7FF), contentColor = Color(0xFF06111F))
                ) { Text("Найти", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onMyWeather, enabled = !loading, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Text("📍 Моя", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
                Button(onClick = onSaveFavorite, enabled = city.isNotBlank(), shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Text("★ Сохранить", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SerzCompactHeroCard(data: WeatherResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${data.city}, ${data.country}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(data.current.description, color = Color.White.copy(alpha = 0.80f), fontSize = 17.sp, maxLines = 1)
                    Text("${data.consensus.providerCount} источника · точность ${data.consensus.confidence}%", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, maxLines = 1)
                }
                WeatherIconVector(data.current.code, modifier = Modifier.size(72.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${data.current.temp}°", color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text("ощущается ${data.current.feels}°", color = Color.White.copy(alpha = 0.84f), fontSize = 16.sp)
                    Text("ветер ${data.current.wind}, порывы ${data.current.gusts} км/ч", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun SerzMaxTabs(selected: String, onSelect: (String) -> Unit) {
    val tabs = listOf("Главная", "Точность", "Сценарии", "Карта", "7 дней", "Настройки")
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
            val active = tab == selected
            Button(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Color(0xFFEFF6FF) else Color.White.copy(alpha = 0.18f),
                    contentColor = if (active) Color(0xFF0F3B66) else Color.White
                )
            ) { Text(tab, fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}
