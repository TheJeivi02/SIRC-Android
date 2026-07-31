package com.sirc.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sirc.core.ui.components.LabeledValue
import com.sirc.core.ui.components.SectionCard
import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverProfile
import com.sirc.domain.model.DriverVehicle
import com.sirc.domain.model.FuelType
import com.sirc.domain.model.RidePlatform

@Composable
internal fun PerfilStep(
    profile: DriverProfile,
    onUpdate: (DriverProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SIRC usará esta información para calcular tu rentabilidad en tu zona.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            label = "Nombre (opcional)",
            value = profile.name.orEmpty(),
            onValueChange = { onUpdate(profile.copy(name = it.ifBlank { null })) },
        )
        TextField(
            label = "País",
            value = profile.country,
            onValueChange = { onUpdate(profile.copy(country = it)) },
        )
        TextField(
            label = "Ciudad",
            value = profile.city,
            onValueChange = { onUpdate(profile.copy(city = it)) },
        )
        Text(text = "Moneda", style = MaterialTheme.typography.titleMedium)
        CurrencySelector(
            selected = profile.currency,
            onSelect = { onUpdate(profile.copy(currency = it)) },
        )
    }
}

@Composable
internal fun VehiculoStep(
    vehicle: DriverVehicle,
    onUpdate: (DriverVehicle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            label = "Nombre del vehículo",
            value = vehicle.name,
            onValueChange = { onUpdate(vehicle.copy(name = it)) },
        )
        TextField(
            label = "Marca",
            value = vehicle.brand,
            onValueChange = { onUpdate(vehicle.copy(brand = it)) },
        )
        TextField(
            label = "Modelo",
            value = vehicle.model,
            onValueChange = { onUpdate(vehicle.copy(model = it)) },
        )
        IntField(
            label = "Año",
            value = vehicle.year,
            onValue = { onUpdate(vehicle.copy(year = it)) },
        )
        Text(text = "Tipo de combustible", style = MaterialTheme.typography.titleMedium)
        FuelTypeSelector(
            selected = vehicle.fuelType,
            onSelect = { onUpdate(vehicle.copy(fuelType = it)) },
        )
        NumericField(
            label = "Consumo (km/L o km/kWh)",
            value = vehicle.consumptionKmPerUnit,
            onValue = { onUpdate(vehicle.copy(consumptionKmPerUnit = it)) },
        )
    }
}

@Composable
internal fun CostosStep(
    fuelPrice: Double,
    maintenanceCostPerKm: Double,
    costDrafts: List<CostDraft>,
    onFuelPrice: (Double) -> Unit,
    onMaintenanceCost: (Double) -> Unit,
    onCostDraftsChange: (List<CostDraft>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumericField(
            label = "Precio del combustible por litro",
            value = fuelPrice,
            onValue = onFuelPrice,
        )
        NumericField(
            label = "Mantenimiento estimado por km",
            value = maintenanceCostPerKm,
            onValue = onMaintenanceCost,
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
                onLabel = { new -> onCostDraftsChange(costDrafts.replaceAt(index, draft.copy(label = new))) },
                onAmount = { new -> onCostDraftsChange(costDrafts.replaceAt(index, draft.copy(amount = new))) },
                onRemove = { onCostDraftsChange(costDrafts.filterIndexed { i, _ -> i != index }) },
            )
        }
        FilterChip(
            selected = false,
            onClick = { onCostDraftsChange(costDrafts + CostDraft()) },
            label = { Text("+ Añadir otro costo") },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlataformasStep(
    platforms: Set<RidePlatform>,
    onToggle: (RidePlatform) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Selecciona las apps en las que trabajas. Puedes activar varias a la vez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RidePlatform.entries.forEach { platform ->
                FilterChip(
                    selected = platform in platforms,
                    onClick = { onToggle(platform) },
                    label = { Text(platform.displayName) },
                )
            }
        }
    }
}

@Composable
internal fun ObjetivosStep(
    thresholds: DecisionThresholds,
    onUpdate: (DecisionThresholds) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Son los dos indicadores principales: SIRC marcará CONVIENE cuando un viaje los supere.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumericField(
            label = "Ganancia mínima por km",
            value = thresholds.minProfitPerKm,
            onValue = { onUpdate(thresholds.copy(minProfitPerKm = it)) },
        )
        NumericField(
            label = "Ganancia mínima por hora",
            value = thresholds.minProfitPerHour,
            onValue = { onUpdate(thresholds.copy(minProfitPerHour = it)) },
        )
    }
}

@Composable
internal fun ResumenStep(config: DriverConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(title = "Perfil") {
            LabeledValue(label = "Nombre", value = config.profile.name.orEmpty().ifBlank { "—" })
            LabeledValue(label = "País", value = config.profile.country)
            LabeledValue(label = "Ciudad", value = config.profile.city)
            LabeledValue(label = "Moneda", value = config.profile.currency)
        }
        SectionCard(title = "Vehículo") {
            LabeledValue(label = "Nombre", value = config.vehicle.name.ifBlank { "—" })
            LabeledValue(label = "Marca", value = config.vehicle.brand)
            LabeledValue(label = "Modelo", value = config.vehicle.model)
            LabeledValue(label = "Año", value = config.vehicle.year.toString())
            LabeledValue(label = "Combustible", value = config.vehicle.fuelType.displayName)
            LabeledValue(label = "Consumo", value = formatNumber(config.vehicle.consumptionKmPerUnit))
        }
        SectionCard(title = "Costos") {
            LabeledValue(label = "Combustible / litro", value = formatNumber(config.fuelPrice))
            LabeledValue(label = "Mantenimiento / km", value = formatNumber(config.maintenanceCostPerKm))
            if (config.additionalCosts.isEmpty()) {
                LabeledValue(label = "Otros costos", value = "—")
            } else {
                config.additionalCosts.forEach {
                    LabeledValue(label = it.label, value = formatNumber(it.costPerKm))
                }
            }
        }
        SectionCard(title = "Plataformas") {
            LabeledValue(
                label = "Activas",
                value = config.platforms.map { it.displayName }.sorted().joinToString(", "),
            )
        }
        SectionCard(title = "Objetivos") {
            LabeledValue(label = "Ganancia mínima por km", value = formatNumber(config.thresholds.minProfitPerKm))
            LabeledValue(label = "Ganancia mínima por hora", value = formatNumber(config.thresholds.minProfitPerHour))
        }
    }
}

private fun List<CostDraft>.replaceAt(
    index: Int,
    draft: CostDraft,
): List<CostDraft> = toMutableList().also { it[index] = draft }

internal data class CostDraft(
    val label: String = "",
    val amount: String = "",
)

private fun CostDraft.toAdditionalCost(): AdditionalCost? {
    if (label.isBlank()) return null
    val parsed = amount.toDoubleOrNull() ?: return null
    if (parsed < 0.0) return null
    return AdditionalCost(label = label, costPerKm = parsed)
}

internal fun parseCostDrafts(drafts: List<CostDraft>): List<AdditionalCost> =
    drafts.mapNotNull { it.toAdditionalCost() }

internal fun costDraftsValid(drafts: List<CostDraft>): Boolean =
    drafts.all { it.label.isBlank() || it.amount.toDoubleOrNull()?.let { amount -> amount >= 0.0 } == true }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrencySelector(
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CURRENCIES.forEach { (code, label) ->
            FilterChip(
                selected = code == selected,
                onClick = { onSelect(code) },
                label = { Text(label) },
            )
        }
    }
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
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
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

private val CURRENCIES =
    listOf(
        "MXN" to "MXN · Peso mexicano",
        "USD" to "USD · Dólar",
        "COP" to "COP · Peso colombiano",
        "BRL" to "BRL · Real",
        "PEN" to "PEN · Sol",
        "ARS" to "ARS · Peso argentino",
        "CLP" to "CLP · Peso chileno",
        "EUR" to "EUR · Euro",
    )
