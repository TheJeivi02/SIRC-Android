package com.sirc.capture.coordinator

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureSessionStatus
import com.sirc.capture.model.CaptureState
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.observer.WindowObserver
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.repository.CaptureRepository
import com.sirc.core.platform.PlatformDetectionEngine
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordina toda la captura de ofertas de extremo a extremo.
 *
 * Consume eventos de [WindowObserver] y snapshots del [CapturePipeline].
 * No parsea ni guarda snapshots — eso es responsabilidad del pipeline.
 * Gestiona la sesión activa y expone el estado al panel de depuración.
 *
 * La resolución de plataforma por paquete se delega en
 * [PlatformDetectionEngine] (única fuente de verdad, igual que el pipeline).
 */
@Singleton
class OfferCaptureCoordinator @Inject constructor(
    private val windowObserver: WindowObserver,
    private val pipeline: CapturePipeline,
    private val captureRepository: CaptureRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
    private val detectionEngine: PlatformDetectionEngine,
) {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    fun start() {
        if (collectJob?.isActive == true) return
        collectJob =
            scope.launch {
                launch {
                    windowObserver.windowEvents.collect { event -> onWindowEvent(event) }
                }
                launch {
                    pipeline.snapshots.collect { snapshot -> onSnapshot(snapshot) }
                }
            }
        _state.update { it.copy(isCapturing = true) }
        logger.info(TAG, "captura iniciada")
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        _state.update { it.copy(isCapturing = false) }
        logger.info(TAG, "captura detenida")
    }

    /** Limpia el estado del panel sin detener la captura. */
    fun reset() {
        captureRepository.clear()
        _state.update { CaptureState(isCapturing = it.isCapturing) }
    }

    internal suspend fun onWindowEvent(event: CaptureWindowEvent) {
        if (!featureFlags.isEnabled(FeatureFlag.CAPTURE)) return
        runCatching { doProcess(event) }
            .onFailure { logger.error(TAG, "error procesando evento: ${it.message}") }
    }

    private fun doProcess(event: CaptureWindowEvent) {
        logger.debug(TAG, "evento: ${event.packageName} · ${event.eventType}")
        val platform =
            detectionEngine.detect(emptyList(), event.timestampMillis, event.packageName).descriptor?.platform
        if (platform == null) {
            closeActiveSession()
            record(event)
            return
        }

        ensureSession(platform, event.timestampMillis)
        _state.update {
            it.copy(eventsProcessed = it.eventsProcessed + 1, recentEvents = recentEventsWith(event))
        }
    }

    internal fun onSnapshot(snapshot: OfferSnapshot) {
        val session = _state.value.activeSession ?: return
        _state.update {
            it.copy(
                activeSession = session.copy(capturedSnapshotCount = session.capturedSnapshotCount + 1),
                lastSnapshot = snapshot,
                lastProcessingTimeMillis = pipeline.lastMetrics.value.totalMillis,
            )
        }
    }

    private fun ensureSession(
        platform: RidePlatform,
        startedAtMillis: Long,
    ): OfferCaptureSession {
        val current = _state.value.activeSession
        val samePlatform = current?.packageName == platform.packageName
        if (current != null && samePlatform && current.status == CaptureSessionStatus.ACTIVE) {
            return current
        }
        val session =
            OfferCaptureSession(
                id = "session-${System.nanoTime()}",
                startedAtMillis = startedAtMillis,
                packageName = platform.packageName,
                status = CaptureSessionStatus.ACTIVE,
            )
        _state.update { it.copy(activeSession = session) }
        logger.info(TAG, "nueva sesión de captura: ${platform.displayName}")
        return session
    }

    private fun closeActiveSession() {
        val current = _state.value.activeSession ?: return
        _state.update {
            it.copy(activeSession = current.copy(status = CaptureSessionStatus.CLOSED))
        }
    }

    private fun record(event: CaptureWindowEvent) {
        _state.update {
            it.copy(
                eventsProcessed = it.eventsProcessed + 1,
                recentEvents = recentEventsWith(event),
            )
        }
    }

    private fun recentEventsWith(event: CaptureWindowEvent): List<CaptureWindowEvent> =
        (listOf(event.copy(texts = emptyList())) + _state.value.recentEvents).take(MAX_RECENT_EVENTS)

    companion object {
        private const val TAG = "OfferCapture"
        private const val MAX_RECENT_EVENTS = 20
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
