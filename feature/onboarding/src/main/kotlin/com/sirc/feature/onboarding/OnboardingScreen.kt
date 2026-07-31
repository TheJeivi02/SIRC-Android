package com.sirc.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.domain.model.DriverConfig

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft

    var costDrafts by remember { mutableStateOf(listOf<CostDraft>()) }

    fun onContinue() {
        if (state.step == COSTS_STEP) {
            viewModel.updateAdditionalCosts(parseCostDrafts(costDrafts))
        }
        viewModel.next()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
    ) {
        Text(
            text = "Configuración inicial",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Paso ${state.step + 1} de ${STEP_TITLES.size} · ${STEP_TITLES[state.step]}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.step + 1).toFloat() / STEP_TITLES.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            when (state.step) {
                0 -> PerfilStep(profile = draft.profile, onUpdate = viewModel::updateProfile)
                1 -> VehiculoStep(vehicle = draft.vehicle, onUpdate = viewModel::updateVehicle)
                2 ->
                    CostosStep(
                        fuelPrice = draft.fuelPrice,
                        maintenanceCostPerKm = draft.maintenanceCostPerKm,
                        costDrafts = costDrafts,
                        onFuelPrice = viewModel::updateFuelPrice,
                        onMaintenanceCost = viewModel::updateMaintenanceCost,
                        onCostDraftsChange = { costDrafts = it },
                    )
                3 -> PlataformasStep(platforms = draft.platforms, onToggle = viewModel::togglePlatform)
                4 -> ObjetivosStep(thresholds = draft.thresholds, onUpdate = viewModel::updateThresholds)
                5 -> ResumenStep(config = draft)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = viewModel::back,
                enabled = state.step > 0 && !state.saving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Atrás")
            }
            Button(
                onClick = { if (state.step == LAST_STEP) viewModel.save() else onContinue() },
                enabled = canProceed(state.step, draft, costDrafts) && !state.saving,
                modifier = Modifier.weight(2f),
            ) {
                Text(if (state.step == LAST_STEP) "Guardar y comenzar" else "Continuar")
            }
        }
    }
}

private fun canProceed(
    step: Int,
    draft: DriverConfig,
    costDrafts: List<CostDraft>,
): Boolean =
    when (step) {
        0 -> draft.profile.country.isNotBlank() && draft.profile.city.isNotBlank()
        1 ->
            draft.vehicle.brand.isNotBlank() &&
                draft.vehicle.model.isNotBlank() &&
                draft.vehicle.year in 1980..2100 &&
                draft.vehicle.consumptionKmPerUnit > 0.0
        2 -> draft.fuelPrice > 0.0 && draft.maintenanceCostPerKm >= 0.0 && costDraftsValid(costDrafts)
        3 -> draft.platforms.isNotEmpty()
        4 -> draft.thresholds.minProfitPerKm >= 0.0 && draft.thresholds.minProfitPerHour >= 0.0
        5 -> true
        else -> false
    }

private const val COSTS_STEP = 2
private const val LAST_STEP = 5

private val STEP_TITLES =
    listOf(
        "Perfil del conductor",
        "Tu vehículo",
        "Costos básicos",
        "Plataformas",
        "Objetivos de rentabilidad",
        "Resumen",
    )

@Composable
internal fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun NumericField(
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
internal fun IntField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
) {
    var text by rememberSaveable(label) { mutableStateOf(value.toString()) }
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

internal fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
