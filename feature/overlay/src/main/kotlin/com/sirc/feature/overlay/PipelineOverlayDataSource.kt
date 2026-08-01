package com.sirc.feature.overlay

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.OfferPerformanceTracker
import com.sirc.capture.metrics.OfferTiming
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.validation.ValidationEvent
import com.sirc.capture.validation.ValidationRecorder
import com.sirc.domain.engine.ConfidenceEngine
import com.sirc.domain.engine.ConfidenceResult
import com.sirc.domain.engine.RuleEngine
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.OfferEvaluationRecord
import com.sirc.domain.model.OfferEvaluationResult
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleEvaluation
import com.sirc.domain.model.RuleThresholds
import com.sirc.domain.model.RuleVerdict
import com.sirc.domain.model.TripOffer
import com.sirc.domain.repository.DriverConfigRepository
import com.sirc.domain.repository.OfferEvaluationRepository
import com.sirc.domain.repository.OfferHistoryRepository
import com.sirc.domain.repository.OverlayConfigRepository
import com.sirc.domain.session.CaptureSessionManager
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
 * También registra el historial (temporal + persistente), alimenta la sesión
 * de captura ([CaptureSessionManager]) y los tiempos por etapa (Debug).
 */
@Singleton
class PipelineOverlayDataSource @Inject constructor(
    private val pipeline: CapturePipeline,
    private val evaluateUseCase: EvaluateDetailedOfferUseCase,
    private val configRepository: OverlayConfigRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
    private val performanceTracker: OfferPerformanceTracker,
    private val evaluationRepository: OfferEvaluationRepository,
    private val historyRepository: OfferHistoryRepository,
    private val driverConfigRepository: DriverConfigRepository,
    private val ruleEngine: RuleEngine,
    private val confidenceEngine: ConfidenceEngine,
    private val sessionManager: CaptureSessionManager,
    private val validationRecorder: ValidationRecorder,
) : OverlayDataSource {
    private val _uiState = MutableStateFlow(OverlayUiState(config = OverlayConfig()))
    override val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hideJob: Job? = null
    private var snapshotInFlight = false

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
        if (state == OverlayState.ERROR) sessionManager.recordError()
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
        if (snapshotInFlight) return
        snapshotInFlight = true
        sessionManager.start()
        scope.launch {
            try {
                val evaluationStart = System.nanoTime()
                val result = evaluateUseCase(offer)
                val evaluationMillis = elapsedMillis(evaluationStart)
                logger.debug(METRICS_TAG, "evaluación: ${format(evaluationMillis)} ms")
                val rulesStart = System.nanoTime()
                val analysis = analyze(offer, result, snapshot)
                val rulesMillis = elapsedMillis(rulesStart)
                recordValidationEvents(snapshot, result, analysis)
                val overlayStart = System.nanoTime()
                show(result, analysis)
                persist(
                    snapshot,
                    result,
                    analysis,
                    OfferTiming(rulesMillis = rulesMillis, evaluationMillis = evaluationMillis),
                )
                sessionManager.recordOffer(result.evaluation.decision)
                val overlayMillis = elapsedMillis(overlayStart)
                performanceTracker.merge(
                    OfferTiming(
                        rulesMillis = rulesMillis,
                        evaluationMillis = evaluationMillis,
                        overlayMillis = overlayMillis,
                    ),
                )
                logger.debug(
                    METRICS_TAG,
                    "reglas: ${format(rulesMillis)} ms · overlay: ${format(overlayMillis)} ms",
                )
            } catch (error: Throwable) {
                sessionManager.recordError()
                logger.error(TAG, "error evaluando oferta: ${error.message}")
            } finally {
                snapshotInFlight = false
            }
        }
    }

    private suspend fun persist(
        snapshot: OfferSnapshot,
        result: OfferEvaluationResult,
        analysis: Analysis,
        timing: OfferTiming,
    ) {
        val metrics = result.evaluation.metrics
        evaluationRepository.add(
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
        historyRepository.add(
            OfferHistoryEntry(
                platform = snapshot.platform,
                timestampMillis = snapshot.capturedAtMillis,
                estimatedTotal = metrics.estimatedTotal,
                distanceKm = metrics.distanceKm,
                durationMin = metrics.durationMin,
                estimatedProfit = metrics.estimatedProfit,
                decision = result.evaluation.decision,
                summary = result.recommendation.mainReason,
                offerType = analysis.offerType,
                confidencePercent = analysis.confidence.percent,
                confidenceLevel = analysis.confidence.level.name,
                ruleSummary = ruleSummary(analysis),
                reasons = result.evaluation.reasons.joinToString(", "),
                recommendation = result.recommendation.recommendation,
                processingMillis = processingMillis(timing),
                evaluationMillis = timing.evaluationMillis,
                rulesMillis = timing.rulesMillis,
            ),
        )
    }

    private suspend fun analyze(
        offer: TripOffer,
        result: OfferEvaluationResult,
        snapshot: OfferSnapshot,
    ): Analysis {
        val driverConfig = driverConfigRepository.getDriverConfig() ?: DriverConfig.default()
        val ruleEvaluation =
            if (featureFlags.isEnabled(FeatureFlag.RULES)) {
                ruleEngine.evaluate(
                    RuleContext(
                        offer = offer,
                        metrics = result.evaluation.metrics,
                        thresholds = RuleThresholds.from(driverConfig),
                    ),
                )
            } else {
                RuleEvaluation(emptyList())
            }
        val confidence = confidenceEngine.assess(offer, result.evaluation.metrics, ruleEvaluation)
        return Analysis(offerTypeFrom(snapshot.rawData), ruleEvaluation, confidence)
    }

    /** Registra reglas fallidas y ofertas rechazadas para el modo de validación. */
    private fun recordValidationEvents(
        snapshot: OfferSnapshot,
        result: OfferEvaluationResult,
        analysis: Analysis,
    ) {
        analysis.ruleEvaluation.results
            .filter { it.verdict == RuleVerdict.FAIL }
            .forEach { ruleResult ->
                validationRecorder.record(
                    ValidationEvent.RuleFailed(
                        timestampMillis = snapshot.capturedAtMillis,
                        ruleName = ruleResult.ruleName,
                        verdict = ruleResult.verdict.name,
                        message = ruleResult.message,
                    ),
                )
            }
        if (result.recommendation.recommendation == Recommendation.REJECT) {
            validationRecorder.record(
                ValidationEvent.OfferRejected(
                    timestampMillis = snapshot.capturedAtMillis,
                    reason = result.recommendation.mainReason,
                ),
            )
        }
    }

    private fun show(
        result: OfferEvaluationResult,
        analysis: Analysis,
    ) {
        _uiState.update {
            it.copy(
                evaluation = result.evaluation,
                recommendation = result.recommendation,
                offerType = analysis.offerType,
                confidence = analysis.confidence,
                ruleEvaluation = analysis.ruleEvaluation,
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

    private fun ruleSummary(analysis: Analysis): String =
        analysis.ruleEvaluation.results.joinToString(" | ") { "${it.ruleName}:${it.verdict.name}" }

    private fun processingMillis(timing: OfferTiming): Double =
        timing.totalMillis ?: ((timing.rulesMillis ?: 0.0) + (timing.evaluationMillis ?: 0.0))

    companion object {
        private const val TAG = "PipelineOverlay"
        private const val METRICS_TAG = "OverlayMetrics"
        private const val MIN_TTL_SECONDS = 10L
        private const val NANOS_PER_MILLI = 1_000_000.0
        private val LOCALE = java.util.Locale.ROOT
    }
}

private const val OFFER_TYPE_PREFIX = "type="

/** Análisis de reglas y confianza producido junto a la evaluación. */
private data class Analysis(
    val offerType: String?,
    val ruleEvaluation: RuleEvaluation,
    val confidence: ConfidenceResult,
)

/** Extrae el tipo de oferta (p. ej. UBER_REQUEST) reportado por el parser. */
private fun offerTypeFrom(rawData: String?): String? =
    rawData
        ?.substringAfter(OFFER_TYPE_PREFIX, missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }

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
