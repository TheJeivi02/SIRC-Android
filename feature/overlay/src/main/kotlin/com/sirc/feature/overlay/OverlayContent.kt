package com.sirc.feature.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.components.MetricCell
import com.sirc.core.ui.components.OverlayCard
import com.sirc.core.ui.components.OverlayCardContent
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.engine.ProfitEngine
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
            opacityPercent = config.opacityPercent,
            compact = config.compactMode,
        ) {
            OverlayCardContent(
                title = evaluation.offer.platform.displayName,
                compact = config.compactMode,
                onDismiss = onDismiss,
            ) {
                DecisionBadge(decision = evaluation.decision, compact = config.compactMode)

                val metrics = evaluation.metrics
                val color = valueColor(metrics)
                if (config.showProfit) {
                    MetricCell(
                        label = "GANANCIA",
                        value = engine.formatCurrency(metrics.estimatedProfit, evaluation.offer.currency),
                        valueColor = color,
                        compact = config.compactMode,
                    )
                }
                if (config.showProfitPerHour) {
                    MetricCell(
                        label = "POR HORA",
                        value = "${engine.formatCurrency(metrics.profitPerHour, evaluation.offer.currency)}/h",
                        valueColor = color,
                        compact = config.compactMode,
                    )
                }
                if (config.showProfitPerKm) {
                    MetricCell(
                        label = "POR KM",
                        value = "${engine.formatCurrency(metrics.profitPerKm, evaluation.offer.currency)}/km",
                        valueColor = color,
                        compact = config.compactMode,
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
                            fontSize = if (config.compactMode) 10.sp else 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = if (config.compactMode) 4.dp else 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun valueColor(metrics: ProfitMetrics): androidx.compose.ui.graphics.Color =
    when {
        metrics.estimatedProfit <= 0 -> SircColors.NotProfit
        metrics.estimatedProfit >= metrics.totalCost * 0.5 -> SircColors.Profit
        else -> SircColors.OnDark
    }
