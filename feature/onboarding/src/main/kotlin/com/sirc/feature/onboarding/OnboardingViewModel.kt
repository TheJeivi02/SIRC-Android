package com.sirc.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverProfile
import com.sirc.domain.model.DriverVehicle
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.usecase.SaveDriverConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveDriverConfig: SaveDriverConfigUseCase,
) : ViewModel() {
    data class UiState(
        val draft: DriverConfig = DriverConfig.blank(),
        val step: Int = 0,
        val saving: Boolean = false,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun next() {
        _state.update { if (it.step < STEPS.last) it.copy(step = it.step + 1) else it }
    }

    fun back() {
        _state.update { if (it.step > 0) it.copy(step = it.step - 1) else it }
    }

    fun updateProfile(profile: DriverProfile) {
        _state.update { it.copy(draft = it.draft.copy(profile = profile)) }
    }

    fun updateVehicle(vehicle: DriverVehicle) {
        _state.update { it.copy(draft = it.draft.copy(vehicle = vehicle)) }
    }

    fun updateFuelPrice(value: Double) {
        _state.update { it.copy(draft = it.draft.copy(fuelPrice = value)) }
    }

    fun updateMaintenanceCost(value: Double) {
        _state.update { it.copy(draft = it.draft.copy(maintenanceCostPerKm = value)) }
    }

    fun updateAdditionalCosts(costs: List<AdditionalCost>) {
        _state.update { it.copy(draft = it.draft.copy(additionalCosts = costs)) }
    }

    fun togglePlatform(platform: RidePlatform) {
        _state.update {
            val platforms = it.draft.platforms.toMutableSet()
            if (!platforms.add(platform)) platforms.remove(platform)
            it.copy(draft = it.draft.copy(platforms = platforms))
        }
    }

    fun updateThresholds(thresholds: DecisionThresholds) {
        _state.update { it.copy(draft = it.draft.copy(thresholds = thresholds)) }
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            saveDriverConfig.save(_state.value.draft)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        val STEPS: IntRange = 0..5
    }
}
