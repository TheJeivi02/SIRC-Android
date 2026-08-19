package com.sirc.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.LabeledValue
import com.sirc.core.ui.components.SectionCard
import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.FuelType
import com.sirc.domain.model.RidePlatform

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.config

    var costDrafts by
        remember(state.reloadTick) {
            mutableStateOf(config.additionalCosts.map { CostDraft(it.label, formatNumber(it.costPerKm)) })
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Perfil") {
            TextField(
                label = "Nombre (opcional)",
                value = config.profile.name.orEmpty(),
                onValueChange = { viewModel.updateProfile(config.profile.copy(name = it.ifBlank { null })) },
            )
            TextField(
                label = "País",
                value = config.profile.country,
                onValueChange = { viewModel.updateProfile(config.profile.copy(country = it)) },
            )
            TextField(
                label = "Ciudad",
                value = config.profile.city,
                onValueChange = { viewModel.updateProfile(config.profile.copy(city = it)) },
            )
            TextField(
                label = "Moneda (ISO 4217)",
                value = config.profile.currency,
                onValueChange = { viewModel.updateCurrency(it.uppercase().take(3)) },
            )
        }

        SectionCard(title = "Vehículo") {
            TextField(
                label = "Nombre del vehículo",
                value = config.vehicle.name,
                onValueChange = { viewModel.updateVehicle(config.vehicle.copy(name = it)) },
            )
            TextField(
                label = "Marca",
                value = config.vehicle.brand,
                onValueChange = { viewModel.updateVehicle(config.vehicle.copy(brand = it)) },
            )
            TextField(
                label = "Modelo",
                value = config.vehicle.model,
                onValueChange = { viewModel.updateVehicle(config.vehicle.copy(model = it)) },
            )
            IntField(
                label = "Año",
                value = config.vehicle.year,
                resetKey = state.reloadTick,
                onValue = { viewModel.updateVehicle(config.vehicle.copy(year = it)) },
            )
            Text(text = "Tipo de combustible", style = MaterialTheme.typography.titleMedium)
            FuelTypeSelector(
                selected = config.vehicle.fuelType,
                onSelect = { viewModel.updateVehicle(config.vehicle.copy(fuelType = it)) },
            )
            NumericField(
                label = "Consumo (km/L o km/kWh)",
                value = config.vehicle.consumptionKmPerUnit,
                resetKey = state.reloadTick,
                onValue = { viewModel.updateVehicle(config.vehicle.copy(consumptionKmPerUnit = it)) },
            )
        }

        SectionCard(title = "Costos") {
            LabeledValue(label = "Costo por km (calculado)", value = formatNumber(state.derivedCostPerKm))
            Text(
                text = "Se calcula desde combustible/consumo + mantenimiento + otros costos. No se edita.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumericField(
                label = "Precio del combustible por litro",
                value = config.fuelPrice,
                resetKey = state.reloadTick,
                onValue = viewModel::updateFuelPrice,
            )
            NumericField(
                label = "Mantenimiento estimado por km",
                value = config.maintenanceCostPerKm,
                resetKey = state.reloadTick,
                onValue = viewModel::updateMaintenanceCost,
            )
            NumericField(
                label = "Costo fijo por viaje",
                value = config.costs.costPerTrip,
                resetKey = state.reloadTick,
                onValue = { viewModel.updateCosts(config.costs.copy(costPerTrip = it)) },
            )
            Text(
                text =
                    "Cargo fijo opcional por cada viaje (p. ej. estacionamiento, zona). " +
                        "Se resta de la ganancia, no del objetivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "Otros costos configurables (opcional)", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "P. ej. peajes, estacionamiento. Cada costo se suma por kilómetro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            costDrafts.forEachIndexed { index, draft ->
                CostRow(
                    draft = draft,
                    onLabel = { new ->
                        costDrafts = costDrafts.replaceAt(index, draft.copy(label = new))
                        viewModel.updateAdditionalCosts(parseCostDrafts(costDrafts))
                    },
                    onAmount = { new ->
                        costDrafts = costDrafts.replaceAt(index, draft.copy(amount = new))
                        viewModel.updateAdditionalCosts(parseCostDrafts(costDrafts))
                    },
                    onRemove = {
                        costDrafts = costDrafts.filterIndexed { i, _ -> i != index }
                        viewModel.updateAdditionalCosts(parseCostDrafts(costDrafts))
                    },
                )
            }
            FilterChip(
                selected = false,
                onClick = { costDrafts = costDrafts + CostDraft() },
                label = { Text("+ Añadir otro costo") },
            )
        }

        SectionCard(title = "Plataformas") {
            Text(
                text = "SIRC solo procesará ofertas de las plataformas activas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RidePlatform.entries.forEach { platform ->
                    FilterChip(
                        selected = platform in config.platforms,
                        onClick = { viewModel.togglePlatform(platform) },
                        label = { Text(platform.displayName) },
                    )
                }
            }
        }

        SectionCard(title = "Objetivos de ganancia") {
            NumericField(
                label = "Objetivo de ganancia por km",
                value = config.thresholds.minProfitPerKm,
                resetKey = state.reloadTick,
                onValue = { viewModel.updateThresholds(config.thresholds.copy(minProfitPerKm = it)) },
            )
            NumericField(
                label = "Objetivo de ganancia por hora",
                value = config.thresholds.minProfitPerHour,
                resetKey = state.reloadTick,
                onValue = { viewModel.updateThresholds(config.thresholds.copy(minProfitPerHour = it)) },
            )
        }

        SectionCard(title = "Overlay") {
            val overlay = state.overlayConfig
            ToggleRow(
                label = "Mostrar decisión (insignia)",
                checked = overlay.showDecision,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(showDecision = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia",
                checked = overlay.showProfit,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(showProfit = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia por hora",
                checked = overlay.showProfitPerHour,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(showProfitPerHour = it)) },
            )
            ToggleRow(
                label = "Mostrar ganancia por km",
                checked = overlay.showProfitPerKm,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(showProfitPerKm = it)) },
            )
            ToggleRow(
                label = "Mostrar distancia y duración",
                checked = overlay.showTripSummary,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(showTripSummary = it)) },
            )
            ToggleRow(
                label = "Modo compacto",
                checked = overlay.compactMode,
                onCheckedChange = { viewModel.updateOverlay(overlay.copy(compactMode = it)) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Opacidad ${overlay.opacityPercent}%",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Slider(
                    value = overlay.opacityPercent.toFloat(),
                    onValueChange = { viewModel.updateOverlay(overlay.copy(opacityPercent = it.toInt())) },
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
                value = overlay.historyLimit.toString(),
                onValueChange = { new ->
                    new.toIntOrNull()?.takeIf { it > 0 }?.let {
                        viewModel.updateOverlay(overlay.copy(historyLimit = it))
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
            onClick = {
                viewModel.updateAdditionalCosts(parseCostDrafts(costDrafts))
                viewModel.save()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.saved) "Guardado ✓" else "Guardar configuración")
        }
        OutlinedButton(
            onClick = viewModel::discard,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Descartar cambios")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NumericField(
    label: String,
    value: Double,
    resetKey: Any?,
    onValue: (Double) -> Unit,
) {
    var text by rememberSaveable(label, resetKey) { mutableStateOf(formatNumber(value)) }
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
private fun IntField(
    label: String,
    value: Int,
    resetKey: Any?,
    onValue: (Int) -> Unit,
) {
    var text by rememberSaveable(label, resetKey) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            new.toIntOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelTypeSelector(
    selected: FuelType,
    onSelect: (FuelType) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FuelType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.displayName) },
            )
        }
    }
}

@Composable
private fun CostRow(
    draft: CostDraft,
    onLabel: (String) -> Unit,
    onAmount: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            label = "Nombre del costo",
            value = draft.label,
            onValueChange = onLabel,
            modifier = Modifier.weight(1f),
        )
        TextField(
            label = "Monto / km",
            value = draft.amount,
            onValueChange = onAmount,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(
            text = "✕",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onRemove),
        )
    }
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

private data class CostDraft(
    val label: String = "",
    val amount: String = "",
)

private fun List<CostDraft>.replaceAt(
    index: Int,
    draft: CostDraft,
): List<CostDraft> = toMutableList().also { it[index] = draft }

private fun CostDraft.toAdditionalCost(): AdditionalCost? {
    if (label.isBlank()) return null
    val parsed = amount.toDoubleOrNull() ?: return null
    if (parsed < 0.0) return null
    return AdditionalCost(label = label, costPerKm = parsed)
}

private fun parseCostDrafts(drafts: List<CostDraft>): List<AdditionalCost> = drafts.mapNotNull { it.toAdditionalCost() }

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
