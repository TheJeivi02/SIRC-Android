package com.sirc.capture.pipeline

import com.sirc.capture.cache.CaptureFrameCache
import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.CaptureMetrics
import com.sirc.capture.metrics.OfferPerformanceTracker
import com.sirc.capture.metrics.OfferTiming
import com.sirc.capture.metrics.ProcessingMetrics
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureSessionStatus
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.model.ScreenFrame
import com.sirc.capture.model.WindowEventType
import com.sirc.capture.ocr.OcrEngine
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.CaptureRepository
import com.sirc.capture.screen.ScreenCapture
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación por defecto del [CapturePipeline].
 *
 * Recibe un [CaptureRequest], captura el frame ([ScreenCapture]), aplica OCR
 * si hay imagen ([OcrEngine]), parsea la oferta ([OfferParser]) y la persiste
 * en el [CaptureRepository]. Deduplica capturas idénticas con la
 * [CaptureFrameCache] y registra métricas de rendimiento. Totalmente
 * desacoplado de Android.
 */
@Singleton
class DefaultCapturePipeline @Inject constructor(
    private val screenCapture: ScreenCapture,
    private val ocrEngine: OcrEngine,
    private val parser: OfferParser,
    private val repository: CaptureRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
    private val cache: CaptureFrameCache,
    private val metrics: CaptureMetrics,
    private val performanceTracker: OfferPerformanceTracker,
) : CapturePipeline {
    private val _state = MutableStateFlow(OverlayState.DISABLED)
    private val _snapshots = MutableSharedFlow<OfferSnapshot>(extraBufferCapacity = 8)
    private val _lastMetrics = MutableStateFlow(ProcessingMetrics())
    private var lastOcrMillis = 0.0

    override val state: StateFlow<OverlayState> = _state.asStateFlow()

    override val snapshots: SharedFlow<OfferSnapshot> = _snapshots.asSharedFlow()

    override val lastMetrics: StateFlow<ProcessingMetrics> = _lastMetrics.asStateFlow()

    override suspend fun process(request: CaptureRequest): OfferSnapshot? {
        if (!featureFlags.isEnabled(FeatureFlag.CAPTURE)) return null
        _state.value = OverlayState.WAITING
        return runCatching { processInternal(request) }
            .getOrElse { error ->
                _state.value = OverlayState.ERROR
                logger.error(TAG, "error en el pipeline: ${error.message}")
                null
            }
    }

    private suspend fun processInternal(request: CaptureRequest): OfferSnapshot? {
        val totalStartNanos = System.nanoTime()

        val captureStartNanos = System.nanoTime()
        val frame = screenCapture.capture(request) ?: return fail()
        val captureMillis = elapsedMillis(captureStartNanos)
        metrics.onCapture(captureMillis)
        _state.value = OverlayState.CAPTURING

        if (!cache.isNew(frame)) {
            logger.debug(TAG, "frame idéntico ya procesado, omitido")
            metrics.onTotal(elapsedMillis(totalStartNanos))
            return idle()
        }

        val texts = resolveTexts(frame)
        if (texts.isEmpty()) return idle()

        _state.value = OverlayState.PROCESSING
        val platform = RidePlatform.fromPackageName(frame.packageName)
        if (platform == null) return idle()

        val session =
            OfferCaptureSession(
                id = "pipeline-${frame.requestId}",
                startedAtMillis = frame.timestampMillis,
                packageName = frame.packageName,
                status = CaptureSessionStatus.ACTIVE,
            )
        val event =
            CaptureWindowEvent(
                eventId = frame.requestId,
                packageName = frame.packageName,
                eventType = WindowEventType.WINDOW_STATE_CHANGED,
                timestampMillis = frame.timestampMillis,
                textCount = texts.size,
                fingerprint = texts.joinToString("|").hashCode().toString(),
                texts = texts,
            )

        val parseStartNanos = System.nanoTime()
        val snapshot = parser.parse(event, session)
        val parseMillis = elapsedMillis(parseStartNanos)
        metrics.onParse(parseMillis)
        val totalMillis = elapsedMillis(totalStartNanos)
        metrics.onTotal(totalMillis)

        if (snapshot != null) {
            cache.markProcessed(frame)
            repository.save(snapshot)
            performanceTracker.record(
                OfferTiming(
                    captureMillis = captureMillis,
                    ocrMillis = if (frame.imageData != null) lastOcrMillis else null,
                    parseMillis = parseMillis,
                    totalMillis = totalMillis,
                ),
            )
            _snapshots.tryEmit(snapshot)
            logger.debug(TAG, "snapshot ${snapshot.platform} guardado")
        }
        _lastMetrics.value =
            ProcessingMetrics(
                captureMillis = captureMillis,
                ocrMillis = if (frame.imageData != null) lastOcrMillis else null,
                parseMillis = parseMillis,
                totalMillis = totalMillis,
            )
        _state.value = OverlayState.WAITING
        return snapshot
    }

    private suspend fun resolveTexts(frame: ScreenFrame): List<String> {
        val imageData = frame.imageData ?: return frame.texts
        if (!featureFlags.isEnabled(FeatureFlag.OCR)) return frame.texts
        val ocrStartNanos = System.nanoTime()
        val texts = ocrEngine.recognize(imageData)
        lastOcrMillis = elapsedMillis(ocrStartNanos)
        metrics.onOcr(lastOcrMillis)
        return texts
    }

    private fun fail(): OfferSnapshot? {
        _state.value = OverlayState.ERROR
        return null
    }

    private fun idle(): OfferSnapshot? {
        _state.value = OverlayState.WAITING
        return null
    }

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    companion object {
        private const val TAG = "CapturePipeline"
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
