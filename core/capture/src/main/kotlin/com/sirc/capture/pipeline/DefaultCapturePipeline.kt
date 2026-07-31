package com.sirc.capture.pipeline

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.log.SircLogger
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación por defecto del [CapturePipeline].
 *
 * Recibe un [CaptureRequest], captura el frame ([ScreenCapture]), aplica OCR
 * si hay imagen ([OcrEngine]), parsea la oferta ([OfferParser]) y la persiste
 * en el [CaptureRepository]. Totalmente desacoplado de Android.
 */
@Singleton
class DefaultCapturePipeline @Inject constructor(
    private val screenCapture: ScreenCapture,
    private val ocrEngine: OcrEngine,
    private val parser: OfferParser,
    private val repository: CaptureRepository,
    private val featureFlags: FeatureFlags,
    private val logger: SircLogger,
) : CapturePipeline {
    private val _state = MutableStateFlow(OverlayState.DISABLED)

    override val state: StateFlow<OverlayState> = _state.asStateFlow()

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
        val frame = screenCapture.capture(request) ?: return fail()
        _state.value = OverlayState.CAPTURING

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

        val snapshot = parser.parse(event, session)
        if (snapshot != null) {
            repository.save(snapshot)
            logger.debug(TAG, "snapshot ${snapshot.platform} guardado")
        }
        _state.value = OverlayState.WAITING
        return snapshot
    }

    private suspend fun resolveTexts(frame: ScreenFrame): List<String> {
        val imageData = frame.imageData ?: return frame.texts
        if (!featureFlags.isEnabled(FeatureFlag.OCR)) return frame.texts
        return ocrEngine.recognize(imageData)
    }

    private fun fail(): OfferSnapshot? {
        _state.value = OverlayState.ERROR
        return null
    }

    private fun idle(): OfferSnapshot? {
        _state.value = OverlayState.WAITING
        return null
    }

    companion object {
        private const val TAG = "CapturePipeline"
    }
}
