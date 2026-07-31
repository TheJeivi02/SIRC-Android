package com.sirc.feature.overlay

import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.TripOffer
import com.sirc.domain.usecase.AddOfferHistoryUseCase
import com.sirc.domain.usecase.EvaluateOfferUseCase
import com.sirc.domain.usecase.GetOverlayConfigUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evalúa las ofertas que llegan del Accessibility Service y publica el estado
 * del overlay (visible/oculto + métricas). También persiste el historial.
 */
@Singleton
class OfferEvaluator @Inject constructor(
    private val eventBus: OfferEventBus,
    private val evaluateUseCase: EvaluateOfferUseCase,
    private val getOverlayConfigUseCase: GetOverlayConfigUseCase,
    private val addOfferHistoryUseCase: AddOfferHistoryUseCase,
    private val engine: ProfitEngine,
) {
    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hideJob: Job? = null

    init {
        scope.launch {
            eventBus.offer.collect { offer ->
                if (offer == null) {
                    hide()
                } else {
                    evaluateAndShow(offer)
                }
            }
        }
        scope.launch {
            getOverlayConfigUseCase.observeConfig().collect { config: OverlayConfig ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    private suspend fun evaluateAndShow(offer: TripOffer) {
        val evaluation = evaluateUseCase(offer)
        persist(evaluation)
        show(evaluation)
    }

    private suspend fun persist(evaluation: ProfitEvaluation) {
        val m = evaluation.metrics
        addOfferHistoryUseCase(
            OfferHistoryEntry(
                platform = evaluation.offer.platform,
                timestampMillis = evaluation.offer.timestampMillis,
                estimatedTotal = m.estimatedTotal,
                distanceKm = m.distanceKm,
                durationMin = m.durationMin,
                estimatedProfit = m.estimatedProfit,
                decision = evaluation.decision,
                summary = buildSummary(evaluation),
            ),
        )
    }

    private fun buildSummary(evaluation: ProfitEvaluation): String {
        val m = evaluation.metrics
        val platform = evaluation.offer.platform.displayName
        val total = engine.formatCurrency(m.estimatedTotal, evaluation.offer.currency)
        val distance = if (m.distanceKm > 0) "${m.distanceKm} km" else null
        val duration = if (m.durationMin > 0) engine.formatHours(m.durationMin) else null
        return listOfNotNull(platform, total, distance, duration).joinToString(" · ")
    }

    private fun show(evaluation: ProfitEvaluation) {
        _uiState.update { it.copy(evaluation = evaluation, visible = true) }
        hideJob?.cancel()
        val ttlSeconds = _uiState.value.config.ttlSeconds.coerceAtLeast(10)
        hideJob =
            scope.launch {
                delay(ttlSeconds * 1000)
                hide()
            }
    }

    private fun hide() {
        hideJob?.cancel()
        _uiState.update { it.copy(visible = false) }
    }
}
