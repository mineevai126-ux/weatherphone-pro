package com.example.weatherphonepro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SerzQuickSummaryStrip(weather: WeatherResult, max: SerzMaxBundle) {
    val rain = weather.hourly.take(12).maxOfOrNull { it.rain } ?: 0
    val wind = weather.hourly.take(12).maxOfOrNull { it.gusts } ?: weather.current.gusts
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617).copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Быстрый вывод", color = Color(0xFFBDE7FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SerzMiniDecision("Зонт", max.verdict.umbrella, Modifier.weight(1f))
                SerzMiniDecision("Осадки", "$rain%", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SerzMiniDecision("Ветер", "$wind км/ч", Modifier.weight(1f))
                SerzMiniDecision("Точность", "${weather.consensus.confidence}%", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SerzMiniDecision(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.13f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.64f), fontSize = 12.sp, maxLines = 1)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SerzPremiumMetricsGrid(weather: WeatherResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SerzMetricTile("Влажность", "${weather.current.humidity}%", MetricKind.HUMIDITY, Modifier.weight(1f))
            SerzMetricTile("Давление", "${weather.current.pressure} гПа", MetricKind.PRESSURE, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SerzMetricTile("UV", one(weather.current.uv), MetricKind.UV, Modifier.weight(1f))
            SerzMetricTile("Облачность", "${weather.current.clouds}%", MetricKind.CLOUD, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SerzMetricTile("Видимость", "${weather.current.visibility / 1000} км", MetricKind.VISIBILITY, Modifier.weight(1f))
            SerzMetricTile("Воздух", "AQI ${weather.air?.aqi ?: 0}", MetricKind.AIR, Modifier.weight(1f))
        }
    }
}

enum class MetricKind { HUMIDITY, PRESSURE, UV, CLOUD, VISIBILITY, AIR }

@Composable
fun SerzMetricTile(title: String, value: String, kind: MetricKind, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            SerzMetricIcon(kind, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp, maxLines = 1)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun SerzMetricIcon(kind: MetricKind, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        when (kind) {
            MetricKind.HUMIDITY -> {
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.22f), w * 0.44f, c)
                drawCircle(Color(0xFF70B7FF), w * 0.23f, Offset(w * 0.5f, h * 0.58f))
                drawLine(Color(0xFF70B7FF), Offset(w * 0.5f, h * 0.16f), Offset(w * 0.34f, h * 0.52f), strokeWidth = 7f, cap = StrokeCap.Round)
                drawLine(Color(0xFF70B7FF), Offset(w * 0.5f, h * 0.16f), Offset(w * 0.66f, h * 0.52f), strokeWidth = 7f, cap = StrokeCap.Round)
            }
            MetricKind.PRESSURE -> {
                drawCircle(Color.White.copy(alpha = 0.22f), w * 0.44f, c)
                drawCircle(Color(0xFFBDE7FF), w * 0.36f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                drawLine(Color(0xFFFFD166), c, Offset(w * 0.70f, h * 0.32f), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            MetricKind.UV -> {
                drawCircle(Color(0xFFFFD166), w * 0.22f, c)
                repeat(8) { i ->
                    val angle = i * 0.785398f
                    drawLine(
                        Color(0xFFFFD166),
                        Offset(c.x + kotlin.math.cos(angle) * w * 0.30f, c.y + kotlin.math.sin(angle) * h * 0.30f),
                        Offset(c.x + kotlin.math.cos(angle) * w * 0.43f, c.y + kotlin.math.sin(angle) * h * 0.43f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
            MetricKind.CLOUD -> {
                drawCircle(Color.White.copy(alpha = 0.88f), w * 0.24f, Offset(w * 0.42f, h * 0.52f))
                drawCircle(Color.White.copy(alpha = 0.92f), w * 0.30f, Offset(w * 0.58f, h * 0.46f))
                drawCircle(Color.White.copy(alpha = 0.86f), w * 0.22f, Offset(w * 0.72f, h * 0.58f))
            }
            MetricKind.VISIBILITY -> {
                drawCircle(Color(0xFFBDE7FF).copy(alpha = 0.28f), w * 0.44f, c)
                drawOval(Color(0xFFBDE7FF), topLeft = Offset(w * 0.12f, h * 0.32f), size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                drawCircle(Color.White, w * 0.12f, c)
            }
            MetricKind.AIR -> {
                drawCircle(Color(0xFF22C55E).copy(alpha = 0.24f), w * 0.44f, c)
                drawLine(Color(0xFF86EFAC), Offset(w * 0.24f, h * 0.68f), Offset(w * 0.72f, h * 0.28f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF86EFAC), w * 0.12f, Offset(w * 0.68f, h * 0.32f))
                drawCircle(Color(0xFF86EFAC), w * 0.10f, Offset(w * 0.42f, h * 0.54f))
            }
        }
    }
}

@Composable
fun SerzPremiumDailyCard(day: DailyWeather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            WeatherIconByDescription(day.description, modifier = Modifier.size(50.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day.date, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(day.description, color = Color.White.copy(alpha = 0.74f), fontSize = 13.sp, maxLines = 1)
                Text("Осадки ${day.rain}% · ветер ${day.wind} · UV ${one(day.uv)}", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 1)
            }
            Text("${day.min}°/${day.max}°", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun WeatherIconByDescription(description: String, modifier: Modifier = Modifier) {
    val code = when {
        description.contains("дожд", ignoreCase = true) || description.contains("лив", ignoreCase = true) -> 61
        description.contains("снег", ignoreCase = true) -> 71
        description.contains("гроз", ignoreCase = true) -> 95
        description.contains("туман", ignoreCase = true) -> 45
        description.contains("пасм", ignoreCase = true) -> 3
        description.contains("облач", ignoreCase = true) -> 2
        else -> 0
    }
    WeatherIconVector(code, modifier)
}

@Composable
fun SerzModelDisagreementCard(consensus: ForecastConsensus) {
    val color = when {
        consensus.confidence >= 80 -> Color(0xFF22C55E)
        consensus.confidence >= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Почему Serz может сомневаться", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(consensus.explanation, color = Color.White.copy(alpha = 0.76f), fontSize = 13.sp, lineHeight = 19.sp)
            Text("Статус: ${consensus.agreement}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SerzReleaseReadyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Подготовка к релизу", color = Color(0xFF312E81), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Следующие технические шаги: release AAB, подпись keystore, privacy policy, финальная иконка, публикация в RuStore/Google Play.", color = Color(0xFF3730A3), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
