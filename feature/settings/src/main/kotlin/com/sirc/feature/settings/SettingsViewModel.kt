package com.sirc.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.engine.ProfitEvaluationEngine
import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.DriverProfile
import com.sirc.domain.model.DriverVehicle
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.usecase.GetDriverConfigUseCase
import com.sirc.domain.usecase.GetOverlayConfigUseCase
import com.sirc.domain.usecase.SaveDriverConfigUseCase
import com.sirc.domain.usecase.SaveOverlayConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getDriverConfig: GetDriverConfigUseCase,
    private val saveDriverConfig: SaveDriverConfigUseCase,
    private val getOverlayConfig: GetOverlayConfigUseCase,
    private val saveOverlayConfig: SaveOverlayConfigUseCase,
) : ViewModel() {
    data class UiState(
        val config: DriverConfig = DriverConfig.default(),
        val overlayConfig: OverlayConfig = OverlayConfig(),
        val saved: Boolean = false,
        val reloadTick: Int = 0,
    ) {
        /**
         * Costo por km que usa el motor: derivado exclusivamente de combustible/
         * consumo + mantenimiento + costos adicionales (única fuente de verdad).
         * No se edita de forma manual; se actualiza al cambiar sus componentes.
         */
        val derivedCostPerKm: Double
            get() = ProfitEvaluationEngine.driverCosts(config).costPerKm
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Última configuración persistida; referencia para "Descartar cambios". */
    private var persistedConfig: DriverConfig = DriverConfig.default()

    init {
        viewModelScope.launch {
            combine(
                getDriverConfig.observeDriverConfig(),
                getOverlayConfig.observeConfig(),
            ) { config, overlay ->
                UiState(
                    config = config ?: DriverConfig.default(),
                    overlayConfig = overlay,
                    saved = false,
                )
            }.collect { emitted ->
                persistedConfig = emitted.config
                val current = _state.value
                if (current.config == emitted.config) {
                    _state.value = current.copy(overlayConfig = emitted.overlayConfig)
                } else {
                    _state.value = emitted.copy(reloadTick = current.reloadTick + 1)
                }
            }
        }
    }

    fun updateProfile(profile: DriverProfile) {
        _state.update { it.copy(config = it.config.copy(profile = profile)) }
    }

    fun updateVehicle(vehicle: DriverVehicle) {
        _state.update { it.copy(config = it.config.copy(vehicle = vehicle)) }
    }

    fun updateCosts(costs: DriverCosts) {
        _state.update { it.copy(config = it.config.copy(costs = costs)) }
    }

    fun updateFuelPrice(value: Double) {
        _state.update { it.copy(config = it.config.copy(fuelPrice = value)) }
    }

    fun updateMaintenanceCost(value: Double) {
        _state.update { it.copy(config = it.config.copy(maintenanceCostPerKm = value)) }
    }

    fun updateAdditionalCosts(costs: List<AdditionalCost>) {
        _state.update { it.copy(config = it.config.copy(additionalCosts = costs)) }
    }

    fun togglePlatform(platform: RidePlatform) {
        _state.update {
            val platforms = it.config.platforms.toMutableSet()
            if (!platforms.add(platform)) platforms.remove(platform)
            it.copy(config = it.config.copy(platforms = platforms))
        }
    }

    fun updateThresholds(thresholds: DecisionThresholds) {
        _state.update { it.copy(config = it.config.copy(thresholds = thresholds)) }
    }

    fun updateCurrency(currency: String) {
        _state.update { it.copy(config = it.config.copy(profile = it.config.profile.copy(currency = currency))) }
    }

    fun updateOverlay(config: OverlayConfig) {
        _state.update { it.copy(overlayConfig = config) }
    }

/** Descarta los cambios sin guardar y restaura la configuración persistida. */
    fun discard() {
        _state.update {
            it.copy(
                config = persistedConfig,
                overlayConfig = it.overlayConfig,
                saved = false,
                reloadTick = it.reloadTick + 1,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val normalized = normalizeCostPerKm(current.config)
            saveDriverConfig.save(normalized)
            saveOverlayConfig(current.overlayConfig)
            _state.update { it.copy(config = normalized, saved = true) }
        }
    }

    /**
     * Mantiene la columna persistida de costo/km coherente con el valor derivado
     * que usa el motor: nunca se persiste un costo/km manual distinto del derivado.
     */
    private fun normalizeCostPerKm(config: DriverConfig): DriverConfig {
        val derived = ProfitEvaluationEngine.driverCosts(config).costPerKm
        return config.copy(costs = config.costs.copy(costPerKm = derived))
    }
}
