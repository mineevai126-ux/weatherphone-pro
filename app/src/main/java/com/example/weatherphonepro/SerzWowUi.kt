package com.example.weatherphonepro

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RichAccuracyCard(consensus: ForecastConsensus) {
    val accent = when {
        consensus.confidence >= 80 -> Color(0xFF22C55E)
        consensus.confidence >= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC).copy(alpha = 0.97f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Точность Serz", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(SerzWowEngine.confidenceLabel(consensus.confidence), color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text("${consensus.confidence}%", color = accent, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            }
            ConfidenceBar(consensus.confidence, accent)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccuracyChip("Осадки: ${consensus.rainRisk}", accent)
                AccuracyChip(consensus.agreement, accent)
            }
            Text("Окно риска: ${consensus.rainWindow}", color = Color(0xFF334155), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(consensus.explanation, color = Color(0xFF475569), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ConfidenceBar(value: Int, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
        val y = size.height / 2
        drawLine(Color(0xFFE2E8F0), Offset(0f, y), Offset(size.width, y), strokeWidth = 18f, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, y), Offset(size.width * value.coerceIn(0, 100) / 100f, y), strokeWidth = 18f, cap = StrokeCap.Round)
    }
}

@Composable
fun AccuracyChip(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f))
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HourlyComfortScaleCard(points: List<HourlyComfortPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Почасовая шкала комфорта", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Serz оценивает температуру, ветер и осадки как общий индекс выхода на улицу.", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(points) { point -> ComfortPointCard(point) }
            }
        }
    }
}

@Composable
fun ComfortPointCard(point: HourlyComfortPoint) {
    val color = when {
        point.comfort >= 80 -> Color(0xFF22C55E)
        point.comfort >= 60 -> Color(0xFF84CC16)
        point.comfort >= 40 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Card(
        modifier = Modifier.width(118.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(point.time, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Text("${point.comfort}%", color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(point.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MiniBar("ветер", point.wind, Color(0xFF70B7FF))
            MiniBar("осадки", point.rain, Color(0xFF38BDF8))
        }
    }
}

@Composable
fun MiniBar(label: String, value: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label $value", color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp)
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val y = size.height / 2
            drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, y), Offset(size.width * value.coerceIn(0, 100) / 100f, y), strokeWidth = 8f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun RainMapPreviewCard(map: RainMapState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220).copy(alpha = 0.82f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(map.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val w = size.width
                    val h = size.height
                    repeat(6) { i ->
                        drawLine(Color(0xFF70B7FF).copy(alpha = 0.18f), Offset(0f, h * i / 5f), Offset(w, h * i / 5f), strokeWidth = 2f)
                        drawLine(Color(0xFF70B7FF).copy(alpha = 0.18f), Offset(w * i / 5f, 0f), Offset(w * i / 5f, h), strokeWidth = 2f)
                    }
                    drawCircle(Color(0xFF38BDF8).copy(alpha = 0.24f), 78f, Offset(w * 0.58f, h * 0.42f))
                    drawCircle(Color(0xFF0EA5E9).copy(alpha = 0.34f), 42f, Offset(w * 0.58f, h * 0.42f))
                    drawCircle(Color(0xFFFACC15), 9f, Offset(w * 0.50f, h * 0.50f))
                    drawCircle(Color.White, 4f, Offset(w * 0.50f, h * 0.50f))
                }
                Text("RADAR\n${map.latitude.formatCoord()}, ${map.longitude.formatCoord()}", color = Color.White.copy(alpha = 0.90f), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(map.subtitle, color = Color.White.copy(alpha = 0.74f), fontSize = 13.sp, lineHeight = 19.sp)
            Text("RainViewer: ${map.rainViewerUrl}", color = Color(0xFFBDE7FF), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun RainAlertButton(context: Context, weather: WeatherResult) {
    Button(onClick = { SerzRainNotifier.notifyRainRisk(context, weather) }, shape = RoundedCornerShape(18.dp)) {
        Text("🔔 Проверить уведомление о дожде")
    }
}

@Composable
fun PaidProvidersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Следующий уровень источников", color = Color(0xFF312E81), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(SerzPaidProviderPlan.providerNames.joinToString(" · "), color = Color(0xFF4338CA), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(SerzPaidProviderPlan.description(), color = Color(0xFF3730A3), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
