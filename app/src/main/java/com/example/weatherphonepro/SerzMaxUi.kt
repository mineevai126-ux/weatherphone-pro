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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SerzVerdictCard(verdict: SerzMainVerdict) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617).copy(alpha = 0.86f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Вывод Serz", color = Color(0xFFBDE7FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(verdict.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
            Text(verdict.subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, lineHeight = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecisionPill("Зонт", verdict.umbrella)
                DecisionPill("Одежда", verdict.clothes)
            }
            DecisionPill("Дорога", verdict.road)
        }
    }
}

@Composable
fun DecisionPill(title: String, value: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF70B7FF).copy(alpha = 0.18f))) {
        Text("$title: $value", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFFBDE7FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SeparateAccuracyCard(accuracy: SeparateAccuracy) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Раздельная точность", color = Color(0xFF0F172A), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            AccuracyLine("Температура", accuracy.temperature, Color(0xFF22C55E))
            AccuracyLine("Осадки", accuracy.precipitation, Color(0xFF38BDF8))
            AccuracyLine("Ветер", accuracy.wind, Color(0xFFF59E0B))
            AccuracyLine("Давление", accuracy.pressure, Color(0xFF8B5CF6))
            Text(accuracy.summary, color = Color(0xFF475569), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
fun AccuracyLine(label: String, value: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF334155), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$value%", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(11.dp)) {
            val y = size.height / 2
            drawLine(Color(0xFFE2E8F0), Offset(0f, y), Offset(size.width, y), strokeWidth = 11f, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, y), Offset(size.width * value.coerceIn(0, 100) / 100f, y), strokeWidth = 11f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HazardWarningsCard(hazards: List<HazardWarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Предупреждения", color = Color(0xFF78350F), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            if (hazards.isEmpty()) {
                Text("Критичных погодных сигналов Serz не видит.", color = Color(0xFF92400E), fontSize = 14.sp)
            } else {
                hazards.forEach { hazard ->
                    Column {
                        Text("${hazard.title}: ${hazard.level}", color = Color(0xFF78350F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(hazard.details, color = Color(0xFF92400E), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SerzActionsCard(actions: List<SerzActionCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Что делать")
        actions.forEach { action ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(action.reason, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(action.decision, color = Color(0xFFBDE7FF), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SettingsPreviewCard(defaultCity: String, quietNight: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC).copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Настройки Serz", color = Color(0xFF0F172A), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Город по умолчанию: $defaultCity", color = Color(0xFF334155), fontSize = 14.sp)
            Text("Тихий ночной режим: ${if (quietNight) "включён" else "выключен"}", color = Color(0xFF334155), fontSize = 14.sp)
            Text("Следующий этап: полноценный экран настроек, единицы измерения, расписание уведомлений и ключи платных источников.", color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
fun WeatherIconVector(code: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)
        val rain = isRain(code)
        val snow = isSnow(code)
        if (code == 0 || code == 1) {
            drawCircle(Color(0xFFFFD166), radius = w * 0.24f, center = center)
            repeat(8) { i ->
                val angle = i * 0.785398f
                val start = Offset(center.x + kotlin.math.cos(angle) * w * 0.33f, center.y + kotlin.math.sin(angle) * h * 0.33f)
                val end = Offset(center.x + kotlin.math.cos(angle) * w * 0.44f, center.y + kotlin.math.sin(angle) * h * 0.44f)
                drawLine(Color(0xFFFFD166), start, end, strokeWidth = 5f, cap = StrokeCap.Round)
            }
        } else {
            drawCircle(Color.White.copy(alpha = 0.92f), radius = w * 0.20f, center = Offset(w * 0.42f, h * 0.45f))
            drawCircle(Color.White.copy(alpha = 0.88f), radius = w * 0.24f, center = Offset(w * 0.58f, h * 0.43f))
            drawCircle(Color.White.copy(alpha = 0.90f), radius = w * 0.18f, center = Offset(w * 0.70f, h * 0.52f))
            drawCircle(Color.White.copy(alpha = 0.86f), radius = w * 0.22f, center = Offset(w * 0.42f, h * 0.56f))
            if (rain) {
                drawLine(Color(0xFF70B7FF), Offset(w * 0.38f, h * 0.76f), Offset(w * 0.32f, h * 0.94f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(Color(0xFF70B7FF), Offset(w * 0.55f, h * 0.76f), Offset(w * 0.49f, h * 0.94f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(Color(0xFF70B7FF), Offset(w * 0.72f, h * 0.76f), Offset(w * 0.66f, h * 0.94f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            if (snow) {
                drawCircle(Color.White, radius = 5f, center = Offset(w * 0.38f, h * 0.83f))
                drawCircle(Color.White, radius = 5f, center = Offset(w * 0.55f, h * 0.91f))
                drawCircle(Color.White, radius = 5f, center = Offset(w * 0.72f, h * 0.83f))
            }
        }
    }
}
