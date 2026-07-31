package com.sirc.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.OverlayConfig
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
        val driverCosts: DriverCosts = DriverCosts(2.0, 0.3, 1.0, "MXN"),
        val thresholds: DecisionThresholds = DecisionThresholds(20.0, 120.0),
        val overlayConfig: OverlayConfig = OverlayConfig(),
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDriverConfig.observeDriverCosts(),
                getDriverConfig.observeDecisionThresholds(),
                getOverlayConfig.observeConfig(),
            ) { costs, thresholds, overlay ->
                UiState(
                    driverCosts = costs,
                    thresholds = thresholds,
                    overlayConfig = overlay,
                    saved = false,
                )
            }.collect { _state.value = it }
        }
    }

    fun updateCosts(costs: DriverCosts) {
        _state.update { it.copy(driverCosts = costs) }
    }

    fun updateThresholds(thresholds: DecisionThresholds) {
        _state.update { it.copy(thresholds = thresholds) }
    }

    fun updateOverlay(config: OverlayConfig) {
        _state.update { it.copy(overlayConfig = config) }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            saveDriverConfig.saveDriverCosts(current.driverCosts)
            saveDriverConfig.saveDecisionThresholds(current.thresholds)
            saveOverlayConfig(current.overlayConfig)
            _state.update { it.copy(saved = true) }
        }
    }
}
