package com.sirc.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.capture.coordinator.OfferCaptureCoordinator
import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.metrics.OfferPerformanceTracker
import com.sirc.capture.metrics.OfferTiming
import com.sirc.capture.metrics.ProcessingMetrics
import com.sirc.capture.model.CaptureState
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.domain.model.OfferEvaluationRecord
import com.sirc.domain.model.RuleVerdict
import com.sirc.domain.repository.OfferEvaluationRepository
import com.sirc.domain.session.CaptureSessionManager
import com.sirc.domain.session.SessionStats
import com.sirc.feature.overlay.OverlayDataSource
import com.sirc.feature.overlay.OverlayManager
import com.sirc.feature.overlay.OverlayUiState
import com.sirc.feature.overlay.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Estado del pipeline de captura + Feature Flags para el panel de depuración. */
@HiltViewModel
class DebugPanelViewModel @Inject constructor(
    private val captureCoordinator: OfferCaptureCoordinator,
    private val capturePipeline: CapturePipeline,
    private val featureFlags: FeatureFlags,
    private val overlayManager: OverlayManager,
    private val permissions: PermissionManager,
    private val performanceTracker: OfferPerformanceTracker,
    private val historyRepository: OfferEvaluationRepository,
    private val overlayDataSource: OverlayDataSource,
    private val sessionManager: CaptureSessionManager,
) : ViewModel() {
    data class FlagStatus(
        val flag: FeatureFlag,
        val enabled: Boolean,
    )

    data class RuleRow(
        val name: String,
        val verdict: RuleVerdict,
        val message: String,
    )

    data class UiState(
        val accessibilityEnabled: Boolean = false,
        val overlayRunning: Boolean = false,
        val captureEnabled: Boolean = true,
        val parserEnabled: Boolean = true,
        val ocrEnabled: Boolean = true,
        val debugPanelEnabled: Boolean = true,
        val overlayFlagEnabled: Boolean = true,
        val overlayState: OverlayState = OverlayState.DISABLED,
        val isCapturing: Boolean = false,
        val activeSession: OfferCaptureSession? = null,
        val lastSnapshot: OfferSnapshot? = null,
        val lastProcessingTimeMillis: Double? = null,
        val lastCaptureMillis: Double? = null,
        val lastOcrMillis: Double? = null,
        val lastDetectionMillis: Double? = null,
        val lastParseMillis: Double? = null,
        val lastTotalMillis: Double? = null,
        val approximateMemoryMb: Double = 0.0,
        val eventsProcessed: Int = 0,
        val recentEvents: List<CaptureWindowEvent> = emptyList(),
        val flags: List<FlagStatus> = emptyList(),
        val lastOffer: OfferEvaluationRecord? = null,
        val lastTiming: OfferTiming? = null,
        val avgCaptureMillis: Double? = null,
        val avgOcrMillis: Double? = null,
        val avgDetectionMillis: Double? = null,
        val avgParseMillis: Double? = null,
        val avgRulesMillis: Double? = null,
        val avgEvaluationMillis: Double? = null,
        val avgOverlayMillis: Double? = null,
        val avgTotalMillis: Double? = null,
        val offerType: String? = null,
        val confidencePercent: Int? = null,
        val confidenceLevel: String? = null,
        val confidenceReasons: List<String> = emptyList(),
        val ruleResults: List<RuleRow> = emptyList(),
        val session: SessionStats = SessionStats(),
    )

    private val refreshTick = MutableStateFlow(0)

    private data class PipelineSnapshot(
        val capture: CaptureState,
        val pipelineState: OverlayState,
        val metrics: ProcessingMetrics,
        val overlayRunning: Boolean,
        val overlayUi: OverlayUiState,
    )

    private val performance =
        combine(
            performanceTracker.averages,
            historyRepository.observe(limit = 1),
            sessionManager.stats,
        ) { averages, latest, session ->
            Triple(averages, latest, session)
        }

    val state: StateFlow<UiState> =
        combine(
            refreshTick,
            captureCoordinator.state,
            capturePipeline.state,
            capturePipeline.lastMetrics,
            overlayManager.isRunning,
        ) { _, capture, pipelineState, metrics, overlayRunning ->
            PipelineSnapshot(
                capture = capture,
                pipelineState = pipelineState,
                metrics = metrics,
                overlayRunning = overlayRunning,
                overlayUi = overlayDataSource.uiState.value,
            )
        }.combine(overlayDataSource.uiState) { snapshot, overlayUi ->
            snapshot.copy(overlayUi = overlayUi)
        }.combine(performance) { snapshot, (averages, latest, session) ->
            build(
                snapshot.capture,
                snapshot.pipelineState,
                snapshot.metrics,
                snapshot.overlayRunning,
                averages,
                latest,
                snapshot.overlayUi,
                session,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                build(
                    captureCoordinator.state.value,
                    capturePipeline.state.value,
                    capturePipeline.lastMetrics.value,
                    overlayManager.isRunning.value,
                    performanceTracker.averages.value,
                    emptyList(),
                    overlayDataSource.uiState.value,
                    sessionManager.stats.value,
                ),
        )

    fun toggleFlag(flag: FeatureFlag) {
        featureFlags.setEnabled(flag, !featureFlags.isEnabled(flag))
        refresh()
    }

    fun startCapture() {
        captureCoordinator.start()
        refresh()
    }

    fun stopCapture() {
        captureCoordinator.stop()
        refresh()
    }

    fun reset() {
        captureCoordinator.reset()
        refresh()
    }

    fun refresh() {
        refreshTick.update { it + 1 }
    }

    fun startSession() {
        sessionManager.start()
        refresh()
    }

    fun pauseSession() {
        sessionManager.pause()
        refresh()
    }

    fun resumeSession() {
        sessionManager.resume()
        refresh()
    }

    fun stopSession() {
        sessionManager.stop()
        refresh()
    }

    /** Exporta un diagnóstico legible (Modo Beta) para compartir con soporte. */
    fun buildDiagnosticsReport(): String {
        val session = sessionManager.stats.value
        val avg = performanceTracker.averages.value
        val last = performanceTracker.lastOffers.value.lastOrNull()
        return buildString {
            appendLine("SIRC · Informe de diagnóstico")
            appendLine("Fecha: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("== Sesión ==")
            appendLine("Estado: ${session.status.name}")
            appendLine("Duración activa: ${session.activeSeconds}s")
            appendLine("Ofertas procesadas: ${session.offersProcessed}")
            appendLine("Aceptadas: ${session.offersAccepted} · Rechazadas: ${session.offersRejected}")
            appendLine("Errores: ${session.errors}")
            appendLine()
            appendLine("== Rendimiento (promedio) ==")
            appendLine("Captura: ${avg.captureMillis ?: "-"} ms")
            appendLine("OCR: ${avg.ocrMillis ?: "-"} ms")
            appendLine("Detección: ${avg.detectionMillis ?: "-"} ms")
            appendLine("Parseo: ${avg.parseMillis ?: "-"} ms")
            appendLine("Reglas: ${avg.rulesMillis ?: "-"} ms")
            appendLine("Evaluación: ${avg.evaluationMillis ?: "-"} ms")
            appendLine("Overlay: ${avg.overlayMillis ?: "-"} ms")
            appendLine("Total: ${avg.totalMillis ?: "-"} ms")
            appendLine()
            appendLine("== Última oferta ==")
            if (last != null) {
                appendLine("Captura: ${last.captureMillis ?: "-"} ms")
                appendLine("OCR: ${last.ocrMillis ?: "-"} ms")
                appendLine("Parseo: ${last.parseMillis ?: "-"} ms")
                appendLine("Reglas: ${last.rulesMillis ?: "-"} ms")
                appendLine("Evaluación: ${last.evaluationMillis ?: "-"} ms")
                appendLine("Overlay: ${last.overlayMillis ?: "-"} ms")
                appendLine("Total: ${last.totalMillis ?: "-"} ms")
            } else {
                appendLine("Sin datos.")
            }
            appendLine()
            appendLine("== Flags ==")
            FeatureFlag.entries.forEach { flag ->
                appendLine("${flag.name}: ${featureFlags.isEnabled(flag)}")
            }
            appendLine()
            appendLine("Memoria aproximada: ${approximateMemoryMb()} MB")
        }
    }

    private fun build(
        capture: CaptureState,
        pipelineState: OverlayState,
        metrics: ProcessingMetrics,
        overlayRunning: Boolean,
        averages: OfferTiming,
        latest: List<OfferEvaluationRecord>,
        overlayUi: OverlayUiState,
        session: SessionStats,
    ): UiState {
        val flags = FeatureFlag.entries.map { FlagStatus(it, featureFlags.isEnabled(it)) }
        val lastOffer = latest.firstOrNull()
        val lastTiming = performanceTracker.lastOffers.value.lastOrNull()
        val ruleResults =
            overlayUi.ruleEvaluation?.results?.map {
                RuleRow(name = it.ruleName, verdict = it.verdict, message = it.message)
            } ?: emptyList()
        return UiState(
            accessibilityEnabled = permissions.hasAccessibilityPermission(),
            overlayRunning = overlayRunning,
            captureEnabled = featureFlags.isEnabled(FeatureFlag.CAPTURE),
            parserEnabled = featureFlags.isEnabled(FeatureFlag.PARSER),
            ocrEnabled = featureFlags.isEnabled(FeatureFlag.OCR),
            debugPanelEnabled = featureFlags.isEnabled(FeatureFlag.DEBUG_PANEL),
            overlayFlagEnabled = featureFlags.isEnabled(FeatureFlag.OVERLAY),
            overlayState = pipelineState,
            isCapturing = capture.isCapturing,
            activeSession = capture.activeSession,
            lastSnapshot = capture.lastSnapshot,
            lastProcessingTimeMillis = capture.lastProcessingTimeMillis,
            lastCaptureMillis = metrics.captureMillis,
            lastOcrMillis = metrics.ocrMillis,
            lastDetectionMillis = metrics.detectionMillis,
            lastParseMillis = metrics.parseMillis,
            lastTotalMillis = metrics.totalMillis,
            approximateMemoryMb = approximateMemoryMb(),
            eventsProcessed = capture.eventsProcessed,
            recentEvents = capture.recentEvents,
            flags = flags,
            lastOffer = lastOffer,
            lastTiming = lastTiming,
            avgCaptureMillis = averages.captureMillis,
            avgOcrMillis = averages.ocrMillis,
            avgDetectionMillis = averages.detectionMillis,
            avgParseMillis = averages.parseMillis,
            avgRulesMillis = averages.rulesMillis,
            avgEvaluationMillis = averages.evaluationMillis,
            avgOverlayMillis = averages.overlayMillis,
            avgTotalMillis = averages.totalMillis,
            offerType = overlayUi.offerType,
            confidencePercent = overlayUi.confidence?.percent,
            confidenceLevel = overlayUi.confidence?.level?.name,
            confidenceReasons = overlayUi.confidence?.reasons.orEmpty(),
            ruleResults = ruleResults,
            session = session,
        )
    }

    private fun approximateMemoryMb(): Double {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024.0 * 1024.0)
    }
}
