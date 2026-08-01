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
import com.sirc.domain.repository.OfferEvaluationRepository
import com.sirc.feature.overlay.OverlayManager
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
) : ViewModel() {
    data class FlagStatus(
        val flag: FeatureFlag,
        val enabled: Boolean,
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
        val avgParseMillis: Double? = null,
        val avgEvaluationMillis: Double? = null,
        val avgOverlayMillis: Double? = null,
        val avgTotalMillis: Double? = null,
    )

    private val refreshTick = MutableStateFlow(0)

    private data class PipelineSnapshot(
        val capture: CaptureState,
        val pipelineState: OverlayState,
        val metrics: ProcessingMetrics,
        val overlayRunning: Boolean,
    )

    private val performance =
        combine(
            performanceTracker.averages,
            historyRepository.observe(limit = 1),
        ) { averages, latest ->
            averages to latest
        }

    val state: StateFlow<UiState> =
        combine(
            refreshTick,
            captureCoordinator.state,
            capturePipeline.state,
            capturePipeline.lastMetrics,
            overlayManager.isRunning,
        ) { _, capture, pipelineState, metrics, overlayRunning ->
            PipelineSnapshot(capture, pipelineState, metrics, overlayRunning)
        }.combine(performance) { snapshot, (averages, latest) ->
            build(
                snapshot.capture,
                snapshot.pipelineState,
                snapshot.metrics,
                snapshot.overlayRunning,
                averages,
                latest,
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

    private fun build(
        capture: CaptureState,
        pipelineState: OverlayState,
        metrics: ProcessingMetrics,
        overlayRunning: Boolean,
        averages: OfferTiming,
        latest: List<OfferEvaluationRecord>,
    ): UiState {
        val flags = FeatureFlag.entries.map { FlagStatus(it, featureFlags.isEnabled(it)) }
        val lastOffer = latest.firstOrNull()
        val lastTiming = performanceTracker.lastOffers.value.lastOrNull()
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
            avgParseMillis = averages.parseMillis,
            avgEvaluationMillis = averages.evaluationMillis,
            avgOverlayMillis = averages.overlayMillis,
            avgTotalMillis = averages.totalMillis,
        )
    }

    private fun approximateMemoryMb(): Double {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024.0 * 1024.0)
    }
}
