package com.sirc.feature.overlay

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation
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
 * Fuente de datos real del overlay: consume el [CapturePipeline].
 *
 * Traduce el estado del pipeline ([OverlayState]) y los snapshots producidos
 * en un [OverlayUiState] con evaluación de rentabilidad real
 * ([EvaluateOfferUseCase]). Reemplaza a la fuente simulada: el overlay muestra
 * ahora el estado real del procesamiento (WAITING/CAPTURING/PROCESSING/ERROR)
 * y el resultado del análisis.
 */
@Singleton
class PipelineOverlayDataSource @Inject constructor(
    private val pipeline: CapturePipeline,
    private val evaluateUseCase: EvaluateOfferUseCase,
    private val configRepository: OverlayConfigRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
) : OverlayDataSource {
    private val _uiState = MutableStateFlow(OverlayUiState(config = OverlayConfig()))
    override val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hideJob: Job? = null

    init {
        scope.launch {
            configRepository.observeOverlayConfig().collect { config: OverlayConfig ->
                _uiState.update { it.copy(config = config) }
            }
        }
        scope.launch {
            pipeline.state.collect { state -> onPipelineState(state) }
        }
        scope.launch {
            pipeline.snapshots.collect { snapshot -> onSnapshot(snapshot) }
        }
    }

    override fun start() = Unit

    override fun stop() {
        hideJob?.cancel()
        _uiState.update {
            it.copy(
                evaluation = null,
                status = OverlayState.DISABLED,
                visible = false,
            )
        }
    }

    private fun onPipelineState(state: OverlayState) {
        val status = if (featureFlags.isEnabled(FeatureFlag.OVERLAY)) state else OverlayState.DISABLED
        _uiState.update {
            it.copy(
                status = status,
                visible = visibleFor(status, it.evaluation),
            )
        }
    }

    private fun onSnapshot(snapshot: OfferSnapshot) {
        val offer = snapshot.toTripOffer() ?: return
        scope.launch {
            runCatching { evaluateUseCase(offer) }
                .onSuccess { evaluation -> show(evaluation) }
                .onFailure { error -> logger.error(TAG, "error evaluando oferta: ${error.message}") }
        }
    }

    private fun show(evaluation: ProfitEvaluation) {
        _uiState.update { it.copy(evaluation = evaluation, visible = true) }
        hideJob?.cancel()
        val ttlSeconds = _uiState.value.config.ttlSeconds.coerceAtLeast(MIN_TTL_SECONDS)
        hideJob =
            scope.launch {
                delay(ttlSeconds * 1000)
                _uiState.update { it.copy(visible = false) }
            }
    }

    private fun visibleFor(
        status: OverlayState,
        evaluation: ProfitEvaluation?,
    ): Boolean = status != OverlayState.DISABLED || evaluation != null

    companion object {
        private const val TAG = "PipelineOverlay"
        private const val MIN_TTL_SECONDS = 10L
    }
}

/** Convierte un snapshot del pipeline en una [TripOffer] evaluable. */
private fun OfferSnapshot.toTripOffer(): TripOffer =
    TripOffer(
        platform = platform,
        timestampMillis = capturedAtMillis,
        estimatedTotal = estimatedTotal,
        distanceKm = distanceKm,
        durationMin = durationMin,
        rawText = listOfNotNull(rawData),
    )
