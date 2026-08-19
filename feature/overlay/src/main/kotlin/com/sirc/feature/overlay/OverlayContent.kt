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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.capture.model.OverlayState
import com.sirc.core.ui.components.MetricCell
import com.sirc.core.ui.components.OverlayCard
import com.sirc.core.ui.components.OverlayCardContent
import com.sirc.core.ui.theme.ProfitState
import com.sirc.core.ui.theme.SircColors
import com.sirc.core.ui.theme.SircSpacing
import com.sirc.domain.engine.ProfitEngine

/**
 * Contenido del Overlay. Muestra el estado real del pipeline y, cuando hay
 * resultado, la evaluación organizada en una jerarquía de 3 niveles:
 *
 * 1. **Decisión** (dominante): banner de ancho completo con el color semáforo
 *    y la etiqueta accionable (ACEPTAR / RECHAZAR / REVISAR).
 * 2. **Oferta**: monto grande + resumen derivado (distancia · duración).
 * 3. **Métricas**: filas de dos columnas con el ancho repartido por igual
 *    (GANANCIA | POR HORA / POR KM | COSTO EST.). Cada celda lleva el color de
 *    SU propio semáforo: verde si cumple su objetivo, naranja si está cerca,
 *    rojo si no cumple.
 *
 * Sin texto explicativo: el conductor RECONOCE de un vistazo si cada dato
 * cumple. Colores semáforo (del tema) + números grandes.
 */
@Composable
fun OverlayContent(
    state: OverlayUiState,
    engine: ProfitEngine,
    onDismiss: () -> Unit,
    onDrag: (dx: Int, dy: Int) -> Unit = { _, _ -> },
) {
    if (state.evaluation == null && state.status == OverlayState.DISABLED) return
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
                title = state.evaluation?.offer?.platform?.displayName ?: "SIRC",
                compact = config.compactMode,
                onDismiss = onDismiss,
            ) {
                AnimatedContent(
                    targetState = state.evaluation != null,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "overlayBody",
                ) { hasEvaluation ->
                    if (hasEvaluation) {
                        EvaluationContent(
                            state = state,
                            engine = engine,
                            compact = config.compactMode,
                        )
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
    engine: ProfitEngine,
    compact: Boolean,
) {
    val presentation = remember(state) { mapToOverlayPresentation(state, engine) } ?: return

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) SircSpacing.XS else SircSpacing.SM),
    ) {
        presentation.decision?.let { decision ->
            DecisionBanner(
                state = decision.state,
                label = decision.label,
                compact = compact,
            )
        }
        presentation.offer?.let { offer ->
            OfferBlock(offer = offer, compact = compact)
        }
        presentation.metricRows.forEach { row ->
            MetricRowView(row = row, compact = compact)
        }
    }
}

/** Nivel 1 — decisión dominante: barra de ancho completo con el color semáforo. */
@Composable
private fun DecisionBanner(
    state: ProfitState,
    label: String,
    compact: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(state.color)
                .padding(vertical = if (compact) 4.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Nivel 2 — oferta: monto grande y resumen derivado (distancia · duración). */
@Composable
private fun OfferBlock(
    offer: OfferPresentation,
    compact: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = offer.amount,
            color = SircColors.OnDark,
            fontSize = if (compact) 22.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        offer.summary?.let { summary ->
            Text(
                text = summary,
                color = SircColors.OnDarkMuted,
                fontSize = if (compact) 10.sp else 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Nivel 3 — métricas en filas de dos columnas con ancho repartido. */
@Composable
private fun MetricRowView(
    row: MetricRowPresentation,
    compact: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SircSpacing.SM),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MetricCell(
            label = row.left.label,
            value = row.left.value,
            valueColor = row.left.tone.toColor(),
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        row.right?.let { right ->
            MetricCell(
                label = right.label,
                value = right.value,
                valueColor = right.tone.toColor(),
                compact = compact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Mapea el tono semáforo del modelo de presentación al color del tema. */
private fun MetricTone.toColor(): Color =
    when (this) {
        MetricTone.NEUTRAL -> SircColors.OnDark
        MetricTone.POSITIVE -> SircColors.Profit
        MetricTone.WARNING -> SircColors.Marginal
        MetricTone.NEGATIVE -> SircColors.NotProfit
        MetricTone.MUTED -> SircColors.OnDarkMuted
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

private const val OVERLAY_ANIMATION_MS = 220
private const val OVERLAY_SCALE_MIN = 0.94f
private const val OVERLAY_SCALE_MAX = 1f
private const val ERROR_MESSAGE = "No se pudo analizar la pantalla. Revisa el permiso de captura."
