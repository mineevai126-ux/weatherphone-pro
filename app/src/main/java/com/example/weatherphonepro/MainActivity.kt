package com.example.weatherphonepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WeatherPhoneProApp() }
    }
}

data class WeatherResult(
    val city: String,
    val country: String,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val air: AirQuality?,
    val advice: String
)

data class CurrentWeather(
    val temp: Int,
    val feels: Int,
    val humidity: Int,
    val pressure: Int,
    val wind: Int,
    val gusts: Int,
    val direction: Int,
    val clouds: Int,
    val visibility: Int,
    val uv: Double,
    val code: Int,
    val isDay: Boolean,
    val description: String,
    val icon: String
)

data class HourlyWeather(
    val time: String,
    val temp: Int,
    val rain: Int,
    val wind: Int,
    val icon: String
)

data class DailyWeather(
    val date: String,
    val min: Int,
    val max: Int,
    val rain: Int,
    val wind: Int,
    val gusts: Int,
    val uv: Double,
    val sunrise: String,
    val sunset: String,
    val description: String,
    val icon: String
)

data class AirQuality(val aqi: Int, val pm10: Double, val pm25: Double)

@Composable
fun WeatherPhoneProApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF07111F)) {
            var city by remember { mutableStateOf("Омск") }
            var data by remember { mutableStateOf<WeatherResult?>(null) }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            fun load() {
                if (city.trim().isEmpty()) {
                    error = "Введите город"
                    return
                }
                scope.launch {
                    loading = true
                    error = null
                    try {
                        data = fetchWeather(city.trim())
                    } catch (e: Exception) {
                        data = null
                        error = e.message ?: "Не удалось загрузить прогноз"
                    } finally {
                        loading = false
                    }
                }
            }

            LaunchedEffect(Unit) { load() }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedWeatherBackground(data?.current?.code ?: 1, data?.current?.isDay ?: true)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Header() }
                    item { SearchCard(city, loading, { city = it }, { load() }) }
                    if (loading) item { LoadingCard() }
                    error?.let { item { ErrorCard(it) } }
                    data?.let { weather ->
                        item { HeroCard(weather) }
                        item { AdviceCard(weather.advice) }
                        item { MetricsGrid(weather) }
                        item { SectionTitle("Почасовой прогноз") }
                        item { HourlyRow(weather.hourly.take(24)) }
                        item { SectionTitle("Прогноз на 7 дней") }
                        items(weather.daily) { DailyCard(it) }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column {
        Text("WeatherPhone Pro", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("точный прогноз, параметры, предупреждения", color = Color.White.copy(alpha = 0.72f), fontSize = 15.sp)
    }
}

@Composable
fun SearchCard(city: String, loading: Boolean, onChange: (String) -> Unit, onSearch: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = city, onValueChange = onChange, label = { Text("Город") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Button(onClick = onSearch, enabled = !loading, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF70B7FF), contentColor = Color(0xFF06111F))) {
                Text("Найти", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoadingCard() = GlassCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White)
        Spacer(Modifier.width(12.dp))
        Text("Загружаю расширенный прогноз...", color = Color.White)
    }
}

@Composable
fun ErrorCard(message: String) = Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD7D7))) {
    Text(message, modifier = Modifier.padding(18.dp), color = Color(0xFF601010))
}

@Composable
fun HeroCard(data: WeatherResult) = GlassCard {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${data.city}, ${data.country}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(data.current.description, color = Color.White.copy(alpha = 0.78f), fontSize = 18.sp)
            }
            Text(data.current.icon, fontSize = 54.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${data.current.temp}°", color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text("ощущается как ${data.current.feels}°", color = Color.White.copy(alpha = 0.80f), fontSize = 17.sp)
                Text("ветер ${data.current.wind} км/ч, порывы ${data.current.gusts} км/ч", color = Color.White.copy(alpha = 0.72f), fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AdviceCard(text: String) = Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5).copy(alpha = 0.95f))) {
    Column(modifier = Modifier.padding(18.dp)) {
        Text("Умный совет", color = Color(0xFF065F46), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color(0xFF064E3B), fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
fun MetricsGrid(data: WeatherResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Влажность", "${data.current.humidity}%", "💧", Modifier.weight(1f)); MetricCard("Давление", "${data.current.pressure} гПа", "🧭", Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("UV-индекс", one(data.current.uv), "🔆", Modifier.weight(1f)); MetricCard("Облачность", "${data.current.clouds}%", "☁️", Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Видимость", "${data.current.visibility / 1000} км", "👁️", Modifier.weight(1f)); MetricCard("Ветер", windName(data.current.direction), "🧭", Modifier.weight(1f)) }
        data.air?.let { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Качество воздуха", "AQI ${it.aqi}", "🌿", Modifier.weight(1f)); MetricCard("PM2.5", "${one(it.pm25)} мкг/м³", "🫁", Modifier.weight(1f)) } }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: String, modifier: Modifier = Modifier) = GlassCard(modifier) {
    Column {
        Text(icon, fontSize = 26.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White.copy(alpha = 0.70f), fontSize = 13.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

@Composable
fun HourlyRow(hours: List<HourlyWeather>) = LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(hours) { HourlyCard(it) } }

@Composable
fun HourlyCard(hour: HourlyWeather) = Card(modifier = Modifier.width(105.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))) {
    Column(modifier = Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(hour.time, color = Color.White.copy(alpha = 0.80f), fontSize = 13.sp)
        Text(hour.icon, fontSize = 29.sp)
        Text("${hour.temp}°", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("${hour.rain}%", color = Color(0xFFBDE7FF), fontSize = 13.sp)
        Text("${hour.wind} км/ч", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
    }
}

@Composable
fun DailyCard(day: DailyWeather) = Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.icon, fontSize = 35.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day.date, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(day.description, color = Color.White.copy(alpha = 0.74f), fontSize = 14.sp)
            }
            Text("${day.min}° / ${day.max}°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Осадки ${day.rain}% · ветер ${day.wind} км/ч · порывы ${day.gusts} км/ч · UV ${one(day.uv)} · 🌅 ${day.sunrise} · 🌇 ${day.sunset}", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))) {
    Box(modifier = Modifier.fillMaxWidth().padding(17.dp)) { content() }
}

@Composable
fun AnimatedWeatherBackground(code: Int, isDay: Boolean) {
    val transition = rememberInfiniteTransition(label = "bg")
    val progress by transition.animateFloat(0f, 1f, animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val top = if (!isDay) Color(0xFF06111F) else if (isRain(code)) Color(0xFF1E3A5F) else if (isSnow(code)) Color(0xFFB7D8F2) else Color(0xFF2F80ED)
    val bottom = if (!isDay) Color(0xFF111827) else if (isRain(code)) Color(0xFF0F172A) else if (isSnow(code)) Color(0xFFEAF7FF) else Color(0xFF56CCF2)
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (isDay && !isRain(code) && !isSnow(code)) {
                val pulse = 1f + 0.08f * sin(progress * 6.28f)
                drawCircle(Color(0xFFFFF2A8).copy(alpha = 0.36f), 170f * pulse, Offset(w * 0.82f, h * 0.13f))
                drawCircle(Color(0xFFFFF6C7).copy(alpha = 0.52f), 88f * pulse, Offset(w * 0.82f, h * 0.13f))
            }
            repeat(5) { i ->
                val x = ((progress * w * 0.22f) + i * w * 0.30f) % (w + 260f) - 130f
                val y = h * (0.16f + i * 0.08f)
                drawCircle(Color.White.copy(alpha = 0.12f), 86f, Offset(x, y))
                drawCircle(Color.White.copy(alpha = 0.10f), 68f, Offset(x + 78f, y + 14f))
            }
            if (isRain(code)) repeat(70) { i ->
                val x = ((i * 47) % w.toInt()).toFloat()
                val y = (((i * 71) % h.toInt()).toFloat() + progress * h) % h
                drawLine(Color(0xFFBDE7FF).copy(alpha = 0.38f), Offset(x, y), Offset(x - 12f, y + 38f), strokeWidth = 3f)
            }
            if (isSnow(code)) repeat(55) { i ->
                val x = ((i * 61) % w.toInt()).toFloat()
                val y = (((i * 83) % h.toInt()).toFloat() + progress * h * 0.55f) % h
                drawCircle(Color.White.copy(alpha = 0.64f), 4f + (i % 4), Offset(x, y))
            }
        }
    }
}

suspend fun fetchWeather(city: String): WeatherResult = withContext(Dispatchers.IO) {
    val encodedCity = URLEncoder.encode(city, "UTF-8")
    val geo = JSONObject(download("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=ru&format=json"))
    val results = geo.optJSONArray("results") ?: throw Exception("Город не найден")
    if (results.length() == 0) throw Exception("Город не найден")
    val place = results.getJSONObject(0)
    val lat = place.getDouble("latitude")
    val lon = place.getDouble("longitude")
    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m,uv_index,visibility&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,uv_index_max,sunrise,sunset&timezone=auto&forecast_days=7"
    val json = JSONObject(download(url))
    val currentJson = json.getJSONObject("current")
    val hourlyJson = json.getJSONObject("hourly")
    val firstUv = hourlyJson.getJSONArray("uv_index").optDouble(0, 0.0)
    val firstVisibility = hourlyJson.getJSONArray("visibility").optDouble(0, 10000.0).roundToInt()
    val code = currentJson.getInt("weather_code")
    val info = weatherCode(code)
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
    val hours = buildHours(hourlyJson)
    val days = buildDays(json.getJSONObject("daily"))
    val air = fetchAir(lat, lon)
    WeatherResult(place.optString("name", city), place.optString("country", ""), current, hours, days, air, makeAdvice(current, hours, air))
}

fun buildHours(h: JSONObject): List<HourlyWeather> {
    val times = h.getJSONArray("time")
    val temps = h.getJSONArray("temperature_2m")
    val rains = h.getJSONArray("precipitation_probability")
    val codes = h.getJSONArray("weather_code")
    val winds = h.getJSONArray("wind_speed_10m")
    val input = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val output = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    return (0 until minOf(times.length(), 48)).map { i ->
        val info = weatherCode(codes.getInt(i))
        HourlyWeather(LocalDateTime.parse(times.getString(i), input).format(output), temps.getDouble(i).roundToInt(), rains.optInt(i, 0), winds.getDouble(i).roundToInt(), info.second)
    }
}

fun buildDays(d: JSONObject): List<DailyWeather> {
    val dates = d.getJSONArray("time")
    val codes = d.getJSONArray("weather_code")
    val max = d.getJSONArray("temperature_2m_max")
    val min = d.getJSONArray("temperature_2m_min")
    val rain = d.getJSONArray("precipitation_probability_max")
    val wind = d.getJSONArray("wind_speed_10m_max")
    val gusts = d.getJSONArray("wind_gusts_10m_max")
    val uv = d.getJSONArray("uv_index_max")
    val sunrise = d.getJSONArray("sunrise")
    val sunset = d.getJSONArray("sunset")
    val inputDate = DateTimeFormatter.ISO_LOCAL_DATE
    val outDate = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))
    val inputTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val outTime = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    return (0 until dates.length()).map { i ->
        val info = weatherCode(codes.getInt(i))
        DailyWeather(
            date = LocalDate.parse(dates.getString(i), inputDate).format(outDate),
            min = min.getDouble(i).roundToInt(),
            max = max.getDouble(i).roundToInt(),
            rain = rain.optInt(i, 0),
            wind = wind.getDouble(i).roundToInt(),
            gusts = gusts.getDouble(i).roundToInt(),
            uv = uv.optDouble(i, 0.0),
            sunrise = LocalDateTime.parse(sunrise.getString(i), inputTime).format(outTime),
            sunset = LocalDateTime.parse(sunset.getString(i), inputTime).format(outTime),
            description = info.first,
            icon = info.second
        )
    }
}

fun fetchAir(lat: Double, lon: Double): AirQuality? = try {
    val json = JSONObject(download("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=european_aqi,pm10,pm2_5"))
    val c = json.getJSONObject("current")
    AirQuality(c.optDouble("european_aqi", 0.0).roundToInt(), c.optDouble("pm10", 0.0), c.optDouble("pm2_5", 0.0))
} catch (_: Exception) { null }

fun download(urlString: String): String {
    val c = URL(urlString).openConnection() as HttpURLConnection
    c.requestMethod = "GET"
    c.connectTimeout = 12000
    c.readTimeout = 12000
    return try {
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) throw Exception("Ошибка сервера погоды: $code")
        text
    } finally { c.disconnect() }
}

fun makeAdvice(current: CurrentWeather, hours: List<HourlyWeather>, air: AirQuality?): String {
    val rain = hours.take(12).maxOfOrNull { it.rain } ?: 0
    val wind = hours.take(12).maxOfOrNull { it.wind } ?: current.wind
    val parts = mutableListOf<String>()
    parts += when { rain >= 70 -> "Зонт лучше взять: в ближайшие часы высокая вероятность осадков до $rain%."; rain >= 40 -> "Осадки возможны, зонт не помешает."; else -> "Осадки маловероятны, день подходит для прогулок и дел на улице." }
    if (wind >= 35) parts += "Ветер заметный, лучше выбрать более закрытую одежду."
    if (current.uv >= 6.0) parts += "UV-индекс высокий: пригодятся очки и SPF."
    if ((air?.aqi ?: 0) >= 80) parts += "Качество воздуха снижено, долгие интенсивные прогулки лучше ограничить."
    return parts.joinToString(" ")
}

fun weatherCode(code: Int): Pair<String, String> = when (code) {
    0 -> "Ясно" to "☀️"; 1 -> "Преимущественно ясно" to "🌤️"; 2 -> "Переменная облачность" to "⛅"; 3 -> "Пасмурно" to "☁️"
    45, 48 -> "Туман" to "🌫️"; 51, 53, 55 -> "Морось" to "🌦️"; 61, 63, 65 -> "Дождь" to "🌧️"; 71, 73, 75 -> "Снег" to "❄️"
    80, 81, 82 -> "Ливень" to "🌧️"; 85, 86 -> "Снегопад" to "🌨️"; 95, 96, 99 -> "Гроза" to "⛈️"; else -> "Неизвестная погода" to "🌡️"
}
fun isRain(code: Int) = code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
fun isSnow(code: Int) = code in listOf(71, 73, 75, 77, 85, 86)
fun windName(deg: Int): String = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")[((deg + 22.5) / 45.0).toInt() % 8]
fun one(value: Double): String = String.format(Locale.US, "%.1f", value)
