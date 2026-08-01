package com.sirc.feature.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.SectionCard
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.model.DayStat
import com.sirc.domain.model.HistoryStats
import java.text.DecimalFormat

/** Dashboard de estadísticas de la actividad de captura (O4). */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                label = "Ofertas analizadas",
                value = "${stats.offersAnalyzed}",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Aceptación",
                value = "${formatPercent(stats.acceptancePercent)}",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                label = "Ganancia total",
                value = formatMoney(stats.totalEstimatedProfit),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Procesamiento",
                value = "${formatNumber(stats.avgProcessingMillis)} ms",
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(title = "Ganancia por hora y por km") {
            MetricRow(label = "Ganancia / hora", value = formatMoney(stats.avgProfitPerHour))
            MetricRow(label = "Ganancia / km", value = formatMoney(stats.avgProfitPerKm))
            MetricRow(label = "Confianza promedio", value = "${formatNumber(stats.avgConfidencePercent)}%")
        }

        SectionCard(title = "Ganancia por día") {
            if (stats.daily.isEmpty()) {
                EmptyChart(message = "Sin datos todavía. El gráfico aparece tras capturar ofertas.")
            } else {
                DailyProfitChart(daily = stats.daily.takeLast(MAX_DAYS))
            }
        }

        SectionCard(title = "Distribución de resultados") {
            if (stats.offersAnalyzed == 0) {
                EmptyChart(message = "Sin datos todavía.")
            } else {
                DecisionDonut(stats = stats)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DailyProfitChart(daily: List<DayStat>) {
    val bars = daily
    val maxProfit = bars.maxOfOrNull { it.profit }?.coerceAtLeast(1.0) ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val barWidth = size.width / bars.size * 0.6f
        val spacing = size.width / bars.size
        val baseline = size.height - 16.dp.toPx()
        bars.forEachIndexed { index, day ->
            val barHeight = (size.height - 24.dp.toPx()) * (day.profit / maxProfit).toFloat().coerceIn(0f, 1f)
            val left = index * spacing + (spacing - barWidth) / 2f
            drawRect(
                color = if (day.profit >= 0) SircColors.Profit else SircColors.NotProfit,
                topLeft = Offset(left, baseline - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
private fun DecisionDonut(stats: HistoryStats) {
    val total = (stats.accepted + stats.rejected + stats.marginal).coerceAtLeast(1)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.size(140.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2f
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)
                val acceptedFraction = stats.accepted.toFloat() / total
                val marginalFraction = stats.marginal.toFloat() / total
                val rejectedFraction = stats.rejected.toFloat() / total

                drawArc(
                    color = SircColors.Profit,
                    startAngle = -90f,
                    sweepAngle = acceptedFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = SircColors.Marginal,
                    startAngle = -90f + acceptedFraction * 360f,
                    sweepAngle = marginalFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = SircColors.NotProfit,
                    startAngle = -90f + (acceptedFraction + marginalFraction) * 360f,
                    sweepAngle = rejectedFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            LegendRow(color = SircColors.Profit, label = "Rentables", value = stats.accepted)
            LegendRow(color = SircColors.Marginal, label = "Al límite", value = stats.marginal)
            LegendRow(color = SircColors.NotProfit, label = "No rentables", value = stats.rejected)
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    value: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyChart(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatMoney(value: Double): String = "${DecimalFormat("#,##0.0#").format(value).replace(',', '.')} $"

private fun formatPercent(value: Double): String = "${formatNumber(value)}%"

private fun formatNumber(value: Double): String = DecimalFormat("#,##0.0#").format(value).replace(',', '.')

private const val MAX_DAYS = 14
