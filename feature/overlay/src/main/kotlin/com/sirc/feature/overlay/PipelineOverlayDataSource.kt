package com.sirc.feature.overlay

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.OfferPerformanceTracker
import com.sirc.capture.metrics.OfferTiming
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.domain.model.OfferEvaluationRecord
import com.sirc.domain.model.OfferEvaluationResult
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.TripOffer
import com.sirc.domain.repository.OfferEvaluationRepository
import com.sirc.domain.repository.OverlayConfigRepository
import com.sirc.domain.usecase.EvaluateDetailedOfferUseCase
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
 * en un [OverlayUiState] con la evaluación real de la oferta
 * ([EvaluateDetailedOfferUseCase]), su desglose de costos y la recomendación.
 * También registra el historial temporal y los tiempos por etapa (Debug).
 */
@Singleton
class PipelineOverlayDataSource @Inject constructor(
    private val pipeline: CapturePipeline,
    private val evaluateUseCase: EvaluateDetailedOfferUseCase,
    private val configRepository: OverlayConfigRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
    private val performanceTracker: OfferPerformanceTracker,
    private val historyRepository: OfferEvaluationRepository,
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
                recommendation = null,
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
            val evaluationStart = System.nanoTime()
            runCatching { evaluateUseCase(offer) }
                .onSuccess { result ->
                    val evaluationMillis = elapsedMillis(evaluationStart)
                    logger.debug(METRICS_TAG, "evaluación: ${format(evaluationMillis)} ms")
                    val overlayStart = System.nanoTime()
                    persist(snapshot, result)
                    show(result)
                    val overlayMillis = elapsedMillis(overlayStart)
                    performanceTracker.merge(
                        OfferTiming(
                            evaluationMillis = evaluationMillis,
                            overlayMillis = overlayMillis,
                        ),
                    )
                    logger.debug(METRICS_TAG, "overlay: ${format(overlayMillis)} ms")
                }
                .onFailure { error ->
                    logger.error(TAG, "error evaluando oferta: ${error.message}")
                }
        }
    }

    private suspend fun persist(
        snapshot: OfferSnapshot,
        result: OfferEvaluationResult,
    ) {
        val metrics = result.evaluation.metrics
        historyRepository.add(
            OfferEvaluationRecord(
                id = 0L,
                timestampMillis = snapshot.capturedAtMillis,
                platform = snapshot.platform,
                price = metrics.estimatedTotal,
                distanceKm = metrics.distanceKm,
                durationMin = metrics.durationMin,
                ocrText = snapshot.texts,
                parserResult = snapshot.rawData,
                evaluation = result.evaluation,
                recommendation = result.recommendation.recommendation,
                confidencePercent = result.recommendation.confidencePercent,
            ),
        )
    }

    private fun show(result: OfferEvaluationResult) {
        _uiState.update {
            it.copy(
                evaluation = result.evaluation,
                recommendation = result.recommendation,
                visible = true,
            )
        }
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
        evaluation: com.sirc.domain.model.ProfitEvaluation?,
    ): Boolean = status != OverlayState.DISABLED || evaluation != null

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    private fun format(millis: Double): String = String.format(LOCALE, "%.1f", millis)

    companion object {
        private const val TAG = "PipelineOverlay"
        private const val METRICS_TAG = "OverlayMetrics"
        private const val MIN_TTL_SECONDS = 10L
        private const val NANOS_PER_MILLI = 1_000_000.0
        private val LOCALE = java.util.Locale.ROOT
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
        rawText = texts.ifEmpty { listOfNotNull(rawData) },
    )
