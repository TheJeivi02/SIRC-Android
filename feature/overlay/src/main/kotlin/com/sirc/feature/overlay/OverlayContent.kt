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
import com.sirc.capture.model.OverlayState
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.components.MetricCell
import com.sirc.core.ui.components.OverlayCard
import com.sirc.core.ui.components.OverlayCardContent
import com.sirc.core.ui.components.RecommendationBadge
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.ProfitMetrics

/**
 * Contenido del Overlay. Muestra el estado real del pipeline y, cuando hay
 * resultado, la recomendación (ACEPTAR/RECHAZAR/REVISAR), el precio, el costo
 * estimado y las métricas de rentabilidad (ganancia, por hora, por km).
 *
 * Filosofía: el conductor NO debe leer; debe RECONOCER de un vistazo si el
 * viaje le conviene. Colores semáforo (del tema) + números grandes.
 */
@Composable
fun OverlayContent(
    state: OverlayUiState,
    engine: ProfitEngine,
    onDismiss: () -> Unit,
    onDrag: (dx: Int, dy: Int) -> Unit = { _, _ -> },
) {
    val evaluation = state.evaluation
    if (evaluation == null && state.status == OverlayState.DISABLED) return
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
                title = evaluation?.offer?.platform?.displayName ?: "SIRC",
                compact = config.compactMode,
                onDismiss = onDismiss,
            ) {
                if (evaluation != null) {
                    val recommendation = state.recommendation
                    if (recommendation != null) {
                        RecommendationBadge(
                            recommendation = recommendation.recommendation,
                            compact = config.compactMode,
                        )
                    } else {
                        DecisionBadge(decision = evaluation.decision, compact = config.compactMode)
                    }

                    val metrics = evaluation.metrics
                    val color = valueColor(metrics)
                    MetricCell(
                        label = "PRECIO",
                        value = engine.formatCurrency(metrics.estimatedTotal, evaluation.offer.currency),
                        valueColor = SircColors.OnDark,
                        compact = config.compactMode,
                    )
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
                        MetricCell(
                            label = "COSTO EST.",
                            value = engine.formatCurrency(metrics.totalCost, evaluation.offer.currency),
                            valueColor = SircColors.OnDarkMuted,
                            compact = config.compactMode,
                        )
                    }
                    if (recommendation != null) {
                        Text(
                            text = "${recommendation.mainReason} · ${recommendation.confidencePercent}% confianza",
                            color = SircColors.OnDarkMuted,
                            fontSize = if (config.compactMode) 9.sp else 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = if (config.compactMode) 4.dp else 6.dp),
                        )
                    }
                    val confidence = state.confidence
                    if (confidence != null) {
                        val confidenceText =
                            if (confidence.isActionable) {
                                val type = state.offerType ?: "Oferta"
                                "$type · Confianza ${confidence.percent}% (${confidence.level.name})"
                            } else {
                                "Información insuficiente · ${confidence.percent}% confianza"
                            }
                        Text(
                            text = confidenceText,
                            color = if (confidence.isActionable) SircColors.OnDarkMuted else SircColors.NotProfit,
                            fontSize = if (config.compactMode) 9.sp else 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = if (config.compactMode) 2.dp else 4.dp),
                        )
                    }
                } else {
                    StatusLabel(status = state.status, compact = config.compactMode)
                }
            }
        }
    }
}

/** Etiqueta ligera del estado del pipeline cuando aún no hay evaluación. */
@Composable
private fun StatusLabel(
    status: OverlayState,
    compact: Boolean,
) {
    val (text, color) =
        when (status) {
            OverlayState.WAITING -> "Esperando oferta…" to SircColors.OnDarkMuted
            OverlayState.CAPTURING -> "Capturando pantalla…" to SircColors.OnDarkMuted
            OverlayState.PROCESSING -> "Analizando oferta…" to SircColors.OnDark
            OverlayState.ERROR -> "Error al analizar" to SircColors.NotProfit
            OverlayState.DISABLED -> return
        }
    Text(
        text = text,
        color = color,
        fontSize = if (compact) 11.sp else 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = if (compact) 4.dp else 8.dp),
    )
}

private fun valueColor(metrics: ProfitMetrics): androidx.compose.ui.graphics.Color =
    when {
        metrics.estimatedProfit <= 0 -> SircColors.NotProfit
        metrics.estimatedProfit >= metrics.totalCost * 0.5 -> SircColors.Profit
        else -> SircColors.OnDark
    }
