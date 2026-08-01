package com.sirc.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.components.LabeledValue
import com.sirc.core.ui.components.RecommendationBadge
import com.sirc.core.ui.components.SectionCard
import com.sirc.core.ui.components.StatusDot
import com.sirc.core.ui.theme.recommendationLabel
import com.sirc.domain.model.RuleVerdict
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Panel de depuración: estado de la infraestructura de captura, Feature Flags,
 * último snapshot, tiempos y eventos recientes. Solo para desarrollo.
 */
@Composable
fun DebugPanelScreen(viewModel: DebugPanelViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Feature Flags") {
            state.flags.forEach { status ->
                FlagRow(status = status, onToggle = { viewModel.toggleFlag(status.flag) })
            }
        }

        SectionCard(title = "Estado de infraestructura") {
            StatusRow(label = "Accessibility", active = state.accessibilityEnabled)
            StatusRow(label = "Overlay en ejecución", active = state.overlayRunning)
            StatusRow(label = "Captura activa", active = state.isCapturing)
            StatusRow(label = "Parser", active = state.parserEnabled)
            StatusRow(label = "OCR", active = state.ocrEnabled)
        }

        SectionCard(title = "Captura") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isCapturing) {
                    OutlinedButton(onClick = viewModel::stopCapture, modifier = Modifier.weight(1f)) {
                        Text("Detener")
                    }
                } else {
                    Button(onClick = viewModel::startCapture, modifier = Modifier.weight(1f)) {
                        Text("Iniciar")
                    }
                }
                OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) {
                    Text("Limpiar")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LabeledValue(
                label = "Sesión activa",
                value = state.activeSession?.let { sessionLabel(it) } ?: "—",
            )
            LabeledValue(label = "Eventos procesados", value = "${state.eventsProcessed}")
            LabeledValue(
                label = "Tiempo de procesamiento",
                value = "${formatMillis(state.lastProcessingTimeMillis)} ms",
            )
            LabeledValue(label = "Captura", value = "${formatMillis(state.lastCaptureMillis)} ms")
            LabeledValue(label = "OCR", value = "${formatMillis(state.lastOcrMillis)} ms")
            LabeledValue(label = "Detección", value = "${formatMillis(state.lastDetectionMillis)} ms")
            LabeledValue(label = "Parseo", value = "${formatMillis(state.lastParseMillis)} ms")
            LabeledValue(label = "Total", value = "${formatMillis(state.lastTotalMillis)} ms")
            LabeledValue(
                label = "Estado del pipeline",
                value = state.overlayState.name,
            )
            LabeledValue(
                label = "Memoria aproximada",
                value = "${formatMemory(state.approximateMemoryMb)} MB",
            )
        }

        SectionCard(title = "Última oferta") {
            val record = state.lastOffer
            if (record == null) {
                Text(
                    text =
                        "Sin ofertas evaluadas todavía. Cuando el pipeline detecte y " +
                            "analice una oferta real, aquí aparece su análisis completo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecommendationBadge(recommendation = record.recommendation)
                    DecisionBadge(decision = record.evaluation.decision)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LabeledValue(
                    label = "Recomendación",
                    value =
                        "${recommendationLabel(record.recommendation)} · " +
                            "${record.confidencePercent}% confianza",
                )
                LabeledValue(label = "Plataforma", value = record.platform.displayName)
                LabeledValue(label = "Precio", value = formatNumber(record.price))
                LabeledValue(label = "Distancia", value = "${formatNumber(record.distanceKm)} km")
                LabeledValue(label = "Duración", value = "${formatNumber(record.durationMin)} min")
                LabeledValue(
                    label = "Motivo",
                    value = record.evaluation.reasons.joinToString(", "),
                )
                LabeledValue(
                    label = "Texto OCR",
                    value = formatOcr(record.ocrText),
                )
                LabeledValue(label = "Parser", value = record.parserResult ?: "—")
                LabeledValue(
                    label = "Capturado",
                    value = formatTimestamp(record.timestampMillis),
                )
            }
        }

        SectionCard(title = "Análisis") {
            LabeledValue(label = "Tipo de oferta", value = state.offerType ?: "—")
            LabeledValue(
                label = "Confianza",
                value =
                    if (state.confidencePercent != null) {
                        "${state.confidencePercent}% (${state.confidenceLevel ?: "—"})"
                    } else {
                        "—"
                    },
            )
            if (state.confidenceReasons.isNotEmpty()) {
                LabeledValue(
                    label = "Razones",
                    value = state.confidenceReasons.joinToString(", "),
                )
            }
            if (state.ruleResults.isEmpty()) {
                Text(
                    text =
                        "Sin reglas evaluadas todavía. Cuando el pipeline analice " +
                            "una oferta real, cada regla (ganancia, por km, por hora, " +
                            "distancia, recogida, duración) aparece con su veredicto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.ruleResults.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = row.verdict.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = verdictColor(row.verdict),
                        )
                    }
                }
            }
        }

        SectionCard(title = "Rendimiento (promedio últimas 20 ofertas)") {
            LabeledValue(label = "Captura", value = "${formatMillis(state.avgCaptureMillis)} ms")
            LabeledValue(label = "OCR", value = "${formatMillis(state.avgOcrMillis)} ms")
            LabeledValue(label = "Detección", value = "${formatMillis(state.avgDetectionMillis)} ms")
            LabeledValue(label = "Parseo", value = "${formatMillis(state.avgParseMillis)} ms")
            LabeledValue(label = "Reglas", value = "${formatMillis(state.avgRulesMillis)} ms")
            LabeledValue(label = "Evaluación", value = "${formatMillis(state.avgEvaluationMillis)} ms")
            LabeledValue(label = "Overlay", value = "${formatMillis(state.avgOverlayMillis)} ms")
            LabeledValue(label = "Total", value = "${formatMillis(state.avgTotalMillis)} ms")
            val timing = state.lastTiming
            if (timing != null) {
                LabeledValue(
                    label = "Última oferta (ms)",
                    value =
                        "R ${formatMillis(timing.rulesMillis)} · " +
                            "E ${formatMillis(timing.evaluationMillis)} · " +
                            "O ${formatMillis(timing.overlayMillis)} · " +
                            "T ${formatMillis(timing.totalMillis)}",
                )
            }
        }

        SectionCard(title = "Último snapshot") {
            val snapshot = state.lastSnapshot
            if (snapshot == null) {
                Text(
                    text =
                        "Sin snapshots todavía. Inicia la captura y abre una plataforma " +
                            "soportada con el servicio de accesibilidad activo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LabeledValue(label = "Plataforma", value = snapshot.platform.displayName)
                LabeledValue(label = "Monto", value = formatNumber(snapshot.estimatedTotal))
                LabeledValue(label = "Distancia", value = "${formatNumber(snapshot.distanceKm)} km")
                LabeledValue(label = "Duración", value = "${formatNumber(snapshot.durationMin)} min")
                LabeledValue(label = "Fuente", value = snapshot.source.name)
                LabeledValue(
                    label = "Capturado",
                    value = formatTimestamp(snapshot.capturedAtMillis),
                )
            }
        }

        SectionCard(title = "Eventos recientes") {
            if (state.recentEvents.isEmpty()) {
                Text(
                    text =
                        "Sin eventos. Con la accesibilidad activa, cada cambio " +
                            "de ventana relevante aparece aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.recentEvents.take(MAX_VISIBLE_EVENTS).forEach { event ->
                    EventRow(event = event)
                }
            }
        }
    }
}

@Composable
private fun FlagRow(
    status: DebugPanelViewModel.FlagStatus,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = status.flag.name, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = status.enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun StatusRow(
    label: String,
    active: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        StatusDot(active = active)
    }
}

@Composable
private fun EventRow(event: CaptureWindowEvent) {
    val now = System.currentTimeMillis()
    val secondsAgo = ((now - event.timestampMillis) / 1000L).coerceAtLeast(0L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.packageName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = "${event.eventType.name} · ${event.textCount} textos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "hace $secondsAgo s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun verdictColor(verdict: RuleVerdict): Color =
    when (verdict) {
        RuleVerdict.PASS -> Color(0xFF2E7D32)
        RuleVerdict.WARNING -> Color(0xFFF9A825)
        RuleVerdict.FAIL -> MaterialTheme.colorScheme.error
    }

private fun sessionLabel(session: OfferCaptureSession): String =
    "${session.packageName} · ${session.status.name} · ${session.capturedSnapshotCount} snapshots"

private fun formatMillis(value: Double?): String = value?.let { formatNumber(it) } ?: "—"

private fun formatMemory(value: Double): String = DecimalFormat("#,##0.0").format(value)

private fun formatNumber(value: Double): String = DecimalFormat("#,##0.0#").format(value).replace(',', '.')

private fun formatOcr(texts: List<String>): String =
    if (texts.isEmpty()) {
        "—"
    } else {
        texts.joinToString(" | ").take(MAX_OCR_TEXT_LENGTH)
    }

private fun formatTimestamp(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))

private const val MAX_VISIBLE_EVENTS = 20
private const val MAX_OCR_TEXT_LENGTH = 200
