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
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.ocr.OcrEngine
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.CaptureRepository
import com.sirc.capture.validation.DiscardReason
import com.sirc.capture.validation.ValidationEvent
import com.sirc.capture.validation.ValidationRecorder
import com.sirc.core.platform.PlatformDetectionEngine
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
 * Recibe un [CaptureRequest], aplica deduplicación, OCR si hay imagen,
 * detección de plataforma única, y parseo. Deduplica capturas idénticas con la
 * [CaptureFrameCache] y registra métricas de rendimiento. Totalmente
 * desacoplado de Android.
 */
@Singleton
class DefaultCapturePipeline @Inject constructor(
    private val detectionEngine: PlatformDetectionEngine,
    private val ocrEngine: OcrEngine,
    private val parser: OfferParser,
    private val repository: CaptureRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
    private val cache: CaptureFrameCache,
    private val metrics: CaptureMetrics,
    private val performanceTracker: OfferPerformanceTracker,
    private val validationRecorder: ValidationRecorder,
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
                validationRecorder.record(
                    ValidationEvent.CaptureError(System.currentTimeMillis(), error.message ?: "fallo del pipeline"),
                )
                null
            }
    }

    private suspend fun processInternal(request: CaptureRequest): OfferSnapshot? {
        val totalStartNanos = System.nanoTime()

        logger.info(TAG, "request recibido: origin=${request.origin} package=${request.packageName}")

        if (!cache.isNew(request)) {
            logger.debug(TAG, "captura idéntica ya procesada, omitida")
            validationRecorder.record(
                ValidationEvent.FrameDiscarded(request.timestampMillis, DiscardReason.DUPLICATE),
            )
            metrics.onTotal(elapsedMillis(totalStartNanos))
            return idle()
        }

        val texts = resolveTexts(request)
        if (texts.isEmpty()) {
            validationRecorder.record(
                ValidationEvent.FrameDiscarded(request.timestampMillis, DiscardReason.NO_TEXTS),
            )
            return idle()
        }

        _state.value = OverlayState.PROCESSING
        val detectionStartNanos = System.nanoTime()
        val result = detectionEngine.detect(texts, request.packageName, request.origin)
        val detectionMillis = elapsedMillis(detectionStartNanos)
        if (!result.isRecognized) {
            logger.info(TAG, "detección: no reconocida (${result.resolution}) en ${"%.1f".format(detectionMillis)} ms")
            validationRecorder.record(
                ValidationEvent.FrameDiscarded(request.timestampMillis, DiscardReason.UNSUPPORTED_PLATFORM),
            )
            return idle()
        }
        logger.info(
            TAG,
            "detección: ${result.descriptor?.platform} / ${result.screenDetection.type} " +
                "en ${"%.1f".format(detectionMillis)} ms",
        )

        if (!featureFlags.isEnabled(FeatureFlag.PARSER)) return idle()

        val parseStartNanos = System.nanoTime()
        val snapshot =
            try {
                parser.parse(request, result, detectionMillis)
            } catch (error: Throwable) {
                validationRecorder.record(
                    ValidationEvent.ParseFailed(
                        System.currentTimeMillis(),
                        error.message ?: "parseo fallido",
                    ),
                )
                logger.warn(TAG, "parseo fallido: ${error.message}")
                null
            }
        val parseMillis = elapsedMillis(parseStartNanos)
        metrics.onParse(parseMillis)
        val totalMillis = elapsedMillis(totalStartNanos)
        metrics.onTotal(totalMillis)

        if (snapshot != null) {
            cache.markProcessed(request)
            repository.save(snapshot)
            performanceTracker.record(
                OfferTiming(
                    captureMillis = 0.0,
                    ocrMillis = if (request.imageData != null) lastOcrMillis else null,
                    detectionMillis = snapshot.detectionMillis,
                    parseMillis = parseMillis,
                    totalMillis = totalMillis,
                ),
            )
            _snapshots.tryEmit(snapshot)
            logger.debug(TAG, "snapshot ${snapshot.platform} guardado")
            logger.info(
                TAG,
                "snapshot ${snapshot.platform} guardado: parse ${"%.1f".format(parseMillis)} ms · " +
                    "total ${"%.1f".format(totalMillis)} ms",
            )
        } else {
            logger.info(TAG, "sin oferta parseable: pantalla ${result.screenDetection.type}")
        }
        _lastMetrics.value =
            ProcessingMetrics(
                captureMillis = 0.0,
                ocrMillis = if (request.imageData != null) lastOcrMillis else null,
                detectionMillis = snapshot?.detectionMillis,
                parseMillis = parseMillis,
                totalMillis = totalMillis,
            )
        _state.value = OverlayState.WAITING
        return snapshot
    }

    private suspend fun resolveTexts(request: CaptureRequest): List<String> {
        val imageData = request.imageData ?: return request.texts
        if (!featureFlags.isEnabled(FeatureFlag.OCR)) return request.texts
        val ocrStartNanos = System.nanoTime()
        val texts =
            try {
                ocrEngine.recognize(imageData)
            } catch (error: Throwable) {
                validationRecorder.record(
                    ValidationEvent.OcrFailed(System.currentTimeMillis(), error.message ?: "OCR fallido"),
                )
                logger.warn(TAG, "OCR fallido: ${error.message}; se usan los textos de accesibilidad")
                request.texts
            }
        lastOcrMillis = elapsedMillis(ocrStartNanos)
        metrics.onOcr(lastOcrMillis)
        return texts
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
