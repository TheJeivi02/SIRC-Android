package com.sirc.feature.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
 * La tarjeta entra/sale con animación suave (escala + opacidad) y el contenido
 * hace un crossfade entre "estado" y "evaluación", evitando parpadeos.
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

    val visibility by animateFloatAsState(
        targetValue = if (state.visible) 1f else 0f,
        animationSpec = tween(durationMillis = OVERLAY_ANIMATION_MS),
        label = "overlayVisibility",
    )

    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    alpha = visibility
                    scaleX = OVERLAY_SCALE_MIN + (OVERLAY_SCALE_MAX - OVERLAY_SCALE_MIN) * visibility
                    scaleY = OVERLAY_SCALE_MIN + (OVERLAY_SCALE_MAX - OVERLAY_SCALE_MIN) * visibility
                }
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
                AnimatedContent(
                    targetState = evaluation != null,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "overlayBody",
                ) { hasEvaluation ->
                    if (hasEvaluation) {
                        val current = evaluation
                        if (current != null) {
                            EvaluationContent(
                                state = state,
                                evaluation = current,
                                engine = engine,
                                compact = config.compactMode,
                            )
                        }
                    } else {
                        StatusLabel(
                            status = state.status,
                            compact = config.compactMode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationContent(
    state: OverlayUiState,
    evaluation: com.sirc.domain.model.ProfitEvaluation,
    engine: ProfitEngine,
    compact: Boolean,
) {
    val config = state.config
    val recommendation = state.recommendation
    if (recommendation != null) {
        RecommendationBadge(
            recommendation = recommendation.recommendation,
            compact = compact,
        )
    } else {
        DecisionBadge(decision = evaluation.decision, compact = compact)
    }

    val metrics = evaluation.metrics
    val color = valueColor(metrics)
    MetricCell(
        label = "PRECIO",
        value = engine.formatCurrency(metrics.estimatedTotal, evaluation.offer.currency),
        valueColor = SircColors.OnDark,
        compact = compact,
    )
    if (config.showProfit) {
        MetricCell(
            label = "GANANCIA",
            value = engine.formatCurrency(metrics.estimatedProfit, evaluation.offer.currency),
            valueColor = color,
            compact = compact,
        )
    }
    if (config.showProfitPerHour) {
        MetricCell(
            label = "POR HORA",
            value = "${engine.formatCurrency(metrics.profitPerHour, evaluation.offer.currency)}/h",
            valueColor = color,
            compact = compact,
        )
    }
    if (config.showProfitPerKm) {
        MetricCell(
            label = "POR KM",
            value = "${engine.formatCurrency(metrics.profitPerKm, evaluation.offer.currency)}/km",
            valueColor = color,
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
        MetricCell(
            label = "COSTO EST.",
            value = engine.formatCurrency(metrics.totalCost, evaluation.offer.currency),
            valueColor = SircColors.OnDarkMuted,
            compact = compact,
        )
    }
    if (recommendation != null) {
        Text(
            text = "${recommendation.mainReason} · ${recommendation.confidencePercent}% confianza",
            color = SircColors.OnDarkMuted,
            fontSize = if (compact) 9.sp else 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
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
            fontSize = if (compact) 9.sp else 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
        )
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
            OverlayState.PROCESSING -> "Analizando oferta…" to SircColors.OnDark
            OverlayState.ERROR -> ERROR_MESSAGE to SircColors.NotProfit
            OverlayState.DISABLED -> return
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = if (compact) 4.dp else 8.dp),
    ) {
        AnimatedVisibility(visible = status != OverlayState.ERROR) {
            Box(
                modifier =
                    Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                        .padding(4.dp),
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = if (compact) 11.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun valueColor(metrics: ProfitMetrics): Color =
    when {
        metrics.estimatedProfit <= 0 -> SircColors.NotProfit
        metrics.estimatedProfit >= metrics.totalCost * 0.5 -> SircColors.Profit
        else -> SircColors.OnDark
    }

private const val OVERLAY_ANIMATION_MS = 220
private const val OVERLAY_SCALE_MIN = 0.94f
private const val OVERLAY_SCALE_MAX = 1f
private const val ERROR_MESSAGE = "No se pudo analizar la pantalla. Revisa el permiso de captura."
