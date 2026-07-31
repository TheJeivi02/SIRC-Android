package com.sirc.feature.overlay

import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import com.sirc.domain.repository.OverlayConfigRepository
import com.sirc.domain.usecase.EvaluateOfferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos simulada para SPRINT 2.
 *
 * Emite ofertas de ejemplo (una por plataforma) que se evalúan con el
 * [ProfitEngine] real vía [EvaluateOfferUseCase], de modo que el overlay
 * muestra métricas y decisión verdaderas sobre datos de entrada simulados.
 * No persiste nada en el historial.
 */
@Singleton
class SimulatedOverlayDataSource @Inject constructor(
    private val configRepository: OverlayConfigRepository,
    private val evaluateUseCase: EvaluateOfferUseCase,
) : OverlayDataSource {
    private val _uiState = MutableStateFlow(OverlayUiState(config = OverlayConfig()))
    override val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null
    private var hideJob: Job? = null
    private var platformIndex = 0

    init {
        scope.launch {
            configRepository.observeOverlayConfig().collect { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    override fun start() {
        if (simulationJob?.isActive == true) return
        simulationJob =
            scope.launch {
                while (true) {
                    emitSimulatedOffer()
                    delay(EMIT_INTERVAL_MS)
                }
            }
    }

    override fun stop() {
        simulationJob?.cancel()
        simulationJob = null
        hideJob?.cancel()
        _uiState.update { it.copy(evaluation = null, visible = false) }
    }

    private suspend fun emitSimulatedOffer() {
        val platform = RidePlatform.entries[platformIndex % RidePlatform.entries.size]
        platformIndex++

        val offer = simulatedOffer(platform)
        val evaluation = evaluateUseCase(offer)

        _uiState.update { it.copy(evaluation = evaluation, visible = true) }

        hideJob?.cancel()
        val ttlSeconds = _uiState.value.config.ttlSeconds.coerceAtLeast(MIN_TTL_SECONDS)
        hideJob =
            scope.launch {
                delay(ttlSeconds * 1000L)
                _uiState.update { it.copy(visible = false) }
            }
    }

    private fun simulatedOffer(platform: RidePlatform): TripOffer =
        TripOffer(
            platform = platform,
            timestampMillis = System.currentTimeMillis(),
            estimatedTotal = ESTIMATED_TOTAL,
            distanceKm = DISTANCE_KM,
            durationMin = DURATION_MIN,
            currency = CURRENCY,
        )

    companion object {
        private const val EMIT_INTERVAL_MS = 20_000L
        private const val MIN_TTL_SECONDS = 10L

        /** Oferta simulada que produce una decisión PROFITABLE con los costos por defecto. */
        private const val ESTIMATED_TOTAL = 150.0
        private const val DISTANCE_KM = 20.0
        private const val DURATION_MIN = 45.0
        private const val CURRENCY = "MXN"
    }
}
