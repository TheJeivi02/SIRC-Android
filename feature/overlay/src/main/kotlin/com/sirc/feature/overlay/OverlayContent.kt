package com.sirc.feature.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics

/**
 * Contenido del Overlay. Máximo cuatro indicadores, extremadamente ligero.
 *
 * Filosofía: el conductor NO debe leer; debe RECONOCER de un vistazo si el
 * viaje le conviene. Colores semáforo + números grandes + datos derivados.
 */
@Composable
fun OverlayContent(
    state: OverlayUiState,
    engine: ProfitEngine,
    onDismiss: () -> Unit,
    onDrag: (dx: Int, dy: Int) -> Unit = { _, _ -> },
) {
    val evaluation = state.evaluation ?: return
    val config = state.config

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        },
                    )
                },
    ) {
        OverlayCard(
            evaluation = evaluation,
            config = config,
            engine = engine,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun OverlayCard(
    evaluation: ProfitEvaluation,
    config: OverlayConfig,
    engine: ProfitEngine,
    onDismiss: () -> Unit,
) {
    val metrics = evaluation.metrics
    val shape = RoundedCornerShape(14.dp)
    val alpha = (config.opacityPercent / 100f).coerceIn(0.15f, 1f)
    val compact = config.compactMode

    Column(
        modifier =
            Modifier
                .clip(shape)
                .background(SircColors.OverlayBackground.copy(alpha = alpha))
                .border(1.dp, SircColors.OverlayBorder, shape)
                .padding(if (compact) 8.dp else 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text = evaluation.offer.platform.displayName,
                color = SircColors.OnDarkMuted,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Ocultar overlay",
                tint = SircColors.OnDarkMuted,
                modifier =
                    Modifier
                        .clickable(onClick = onDismiss)
                        .padding(2.dp),
            )
        }

        DecisionBadge(decision = evaluation.decision, compact = compact)

        if (config.showProfit) {
            MetricCell(
                label = "GANANCIA",
                value = engine.formatCurrency(metrics.estimatedProfit, evaluation.offer.currency),
                valueColor = valueColor(metrics),
                compact = compact,
            )
        }
        if (config.showProfitPerHour) {
            MetricCell(
                label = "POR HORA",
                value = "${engine.formatCurrency(metrics.profitPerHour, evaluation.offer.currency)}/h",
                valueColor = valueColor(metrics),
                compact = compact,
            )
        }
        if (config.showProfitPerKm) {
            MetricCell(
                label = "POR KM",
                value = "${engine.formatCurrency(metrics.profitPerKm, evaluation.offer.currency)}/km",
                valueColor = valueColor(metrics),
                compact = compact,
            )
        }
        if (config.showTripSummary) {
            val distance = if (metrics.distanceKm > 0) "${metrics.distanceKm} km" else null
            val duration = if (metrics.durationMin > 0) engine.formatHours(metrics.durationMin) else null
            val summary = listOfNotNull(distance, duration).joinToString(" · ")
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = SircColors.OnDarkMuted,
                    fontSize = if (compact) 10.sp else 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    compact: Boolean,
) {
    Column(modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp)) {
        Text(
            text = label,
            color = SircColors.OnDarkMuted,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (compact) 16.sp else 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun valueColor(metrics: ProfitMetrics): Color =
    when {
        metrics.estimatedProfit <= 0 -> SircColors.NotProfit
        metrics.estimatedProfit >= metrics.totalCost * 0.5 -> SircColors.Profit
        else -> SircColors.OnDark
    }
