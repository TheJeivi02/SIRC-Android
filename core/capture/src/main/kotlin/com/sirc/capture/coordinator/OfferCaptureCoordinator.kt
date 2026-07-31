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
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.CaptureRepository
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
 * Consume los eventos del [WindowObserver], mantiene la [OfferCaptureSession]
 * activa, produce [OfferSnapshot] con el [OfferParser] y los guarda en el
 * [CaptureRepository]. Está completamente desacoplado de Android: solo depende
 * de interfaces y modelos de `:core:capture`.
 */
@Singleton
class OfferCaptureCoordinator @Inject constructor(
    private val windowObserver: WindowObserver,
    private val parser: OfferParser,
    private val captureRepository: CaptureRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
) {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    fun start() {
        if (collectJob?.isActive == true) return
        collectJob =
            scope.launch {
                windowObserver.windowEvents.collect { event -> process(event) }
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

    internal suspend fun process(event: CaptureWindowEvent) {
        if (!featureFlags.isEnabled(FeatureFlag.CAPTURE)) return
        runCatching { doProcess(event) }
            .onFailure { logger.error(TAG, "error procesando evento: ${it.message}") }
    }

    private fun doProcess(event: CaptureWindowEvent) {
        logger.debug(TAG, "evento: ${event.packageName} · ${event.eventType}")
        val platform = RidePlatform.fromPackageName(event.packageName)
        if (platform == null) {
            closeActiveSession()
            record(event)
            return
        }

        val session = ensureSession(platform, event.timestampMillis)
        val startNanos = System.nanoTime()
        val snapshot =
            if (featureFlags.isEnabled(FeatureFlag.PARSER)) parser.parse(event, session) else null
        val elapsedMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

        if (snapshot != null) {
            captureRepository.save(snapshot)
            logger.debug(TAG, "snapshot ${snapshot.platform} guardado ($elapsedMillis ms)")
        }

        val updatedSession =
            if (snapshot != null) {
                session.copy(capturedSnapshotCount = session.capturedSnapshotCount + 1)
            } else {
                session
            }

        _state.update {
            it.copy(
                activeSession = updatedSession,
                lastSnapshot = snapshot ?: it.lastSnapshot,
                lastProcessingTimeMillis = elapsedMillis,
                eventsProcessed = it.eventsProcessed + 1,
                recentEvents = recentEventsWith(it, event),
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
                recentEvents = recentEventsWith(it, event),
            )
        }
    }

    private fun recentEventsWith(
        state: CaptureState,
        event: CaptureWindowEvent,
    ): List<CaptureWindowEvent> = (listOf(event.copy(texts = emptyList())) + state.recentEvents).take(MAX_RECENT_EVENTS)

    companion object {
        private const val TAG = "OfferCapture"
        private const val MAX_RECENT_EVENTS = 20
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
