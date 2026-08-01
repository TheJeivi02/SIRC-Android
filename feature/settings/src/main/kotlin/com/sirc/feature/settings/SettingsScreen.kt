package com.sirc.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.SectionCard

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Costos del conductor") {
            NumericField(
                label = "Costo por km",
                value = state.config.costs.costPerKm,
                onValue = { viewModel.updateCosts(state.config.costs.copy(costPerKm = it)) },
            )
            NumericField(
                label = "Costo por minuto",
                value = state.config.costs.costPerMinute,
                onValue = { viewModel.updateCosts(state.config.costs.copy(costPerMinute = it)) },
            )
            NumericField(
                label = "Costo fijo por viaje",
                value = state.config.costs.costPerTrip,
                onValue = { viewModel.updateCosts(state.config.costs.copy(costPerTrip = it)) },
            )
            TextField(
                label = "Moneda (ISO 4217)",
                value = state.config.profile.currency,
                onValueChange = {
                    viewModel.updateCurrency(it.uppercase().take(3))
                },
            )
        }

        SectionCard(title = "Umbrales de decisión") {
            NumericField(
                label = "Ganancia mínima por km",
                value = state.config.thresholds.minProfitPerKm,
                onValue = { viewModel.updateThresholds(state.config.thresholds.copy(minProfitPerKm = it)) },
            )
            NumericField(
                label = "Ganancia mínima por hora",
                value = state.config.thresholds.minProfitPerHour,
                onValue = { viewModel.updateThresholds(state.config.thresholds.copy(minProfitPerHour = it)) },
            )
        }

        SectionCard(title = "Overlay") {
            val config = state.overlayConfig
            ToggleRow(
                label = "Mostrar decisión (insignia)",
                checked = config.showDecision,
                onCheckedChange = { viewModel.updateOverlay(config.copy(showDecision = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia",
                checked = config.showProfit,
                onCheckedChange = { viewModel.updateOverlay(config.copy(showProfit = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia por hora",
                checked = config.showProfitPerHour,
                onCheckedChange = { viewModel.updateOverlay(config.copy(showProfitPerHour = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia por km",
                checked = config.showProfitPerKm,
                onCheckedChange = { viewModel.updateOverlay(config.copy(showProfitPerKm = it)) },
            )
            ToggleRow(
                label = "Mostrar distancia y duración",
                checked = config.showTripSummary,
                onCheckedChange = { viewModel.updateOverlay(config.copy(showTripSummary = it)) },
            )
            ToggleRow(
                label = "Modo compacto",
                checked = config.compactMode,
                onCheckedChange = { viewModel.updateOverlay(config.copy(compactMode = it)) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Opacidad ${config.opacityPercent}%",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Slider(
                    value = config.opacityPercent.toFloat(),
                    onValueChange = {
                        viewModel.updateOverlay(config.copy(opacityPercent = it.toInt()))
                    },
                    valueRange = 20f..100f,
                    modifier = Modifier.weight(2f),
                )
            }
            Text(
                text = "Se muestran máx. 4 indicadores. Se prioriza la velocidad de lectura.",
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                label = "Límite de registros del historial",
                value = config.historyLimit.toString(),
                onValueChange = { new ->
                    new.toIntOrNull()?.takeIf { it > 0 }?.let {
                        viewModel.updateOverlay(config.copy(historyLimit = it))
                    }
                },
            )
            Text(
                text = "Los registros más antiguos se eliminan automáticamente al superar este límite.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.saved) "Guardado ✓" else "Guardar configuración")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NumericField(
    label: String,
    value: Double,
    onValue: (Double) -> Unit,
) {
    var text by rememberSaveable(label) { mutableStateOf(formatNumber(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            new.toDoubleOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
