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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.components.LabeledValue
import com.sirc.core.ui.components.SectionCard
import com.sirc.core.ui.components.StatusDot
import com.sirc.feature.overlay.OverlayViewModel
import kotlin.math.roundToInt

/**
 * Diagnóstico del overlay: estado de los 5 requisitos (overlay, accesibilidad,
 * servicio en ejecución, notificaciones, batería) + última oferta evaluada por
 * el overlay en tiempo real.
 */
@Composable
fun DiagnosisScreen(
    viewModel: DiagnosisViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overlayState by overlayViewModel.uiState.collectAsStateWithLifecycle()
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
        SectionCard(title = "Requisitos del overlay") {
            StatusRow(
                label = "Permiso de overlay",
                active = state.overlayPermissionGranted,
                actionLabel = "Permitir",
                onAction = viewModel::openOverlaySettings,
            )
            StatusRow(
                label = "Servicio de accesibilidad",
                active = state.accessibilityEnabled,
                actionLabel = "Activar",
                onAction = viewModel::openAccessibilitySettings,
            )
            StatusRow(
                label = "Notificaciones (FGS)",
                active = state.notificationsGranted,
                actionLabel = "Permitir",
                onAction = viewModel::openNotificationSettings,
            )
            StatusRow(
                label = "Optimización de batería",
                active = state.batteryOptimizationIgnored,
                actionLabel = "Eximir",
                onAction = viewModel::openBatterySettings,
            )
        }

        SectionCard(title = "Overlay") {
            StatusRow(label = "En ejecución", active = state.overlayRunning)
            Spacer(modifier = Modifier.height(12.dp))
            if (state.overlayRunning) {
                OutlinedButton(
                    onClick = viewModel::stopOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Detener overlay")
                }
            } else {
                Button(
                    onClick = viewModel::startOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Iniciar overlay")
                }
            }
        }

        SectionCard(title = "Última oferta evaluada") {
            val evaluation = overlayState.evaluation
            if (evaluation == null) {
                Text(
                    text =
                        "El overlay aún no muestra datos. Inicia el overlay para ver " +
                            "aquí la última oferta procesada en tiempo real.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                DecisionBadge(decision = evaluation.decision)
                Spacer(modifier = Modifier.height(12.dp))
                LabeledValue(
                    label = "Plataforma",
                    value = evaluation.offer.platform.displayName,
                )
                LabeledValue(
                    label = "Ganancia",
                    value = formatMoney(evaluation.metrics.estimatedProfit),
                )
                LabeledValue(
                    label = "Por hora",
                    value = evaluation.metrics.profitPerHour?.let { "${formatMoney(it)}/h" } ?: "—",
                )
                LabeledValue(
                    label = "Por km",
                    value = evaluation.metrics.profitPerKm?.let { "${formatMoney(it)}/km" } ?: "—",
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    active: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!active && actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
            StatusDot(active = active)
        }
    }
}

private fun formatMoney(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        "${rounded.toLong()}"
    } else {
        rounded.toString()
    }
}
