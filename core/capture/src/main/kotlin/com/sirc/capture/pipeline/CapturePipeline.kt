package com.sirc.capture.pipeline

import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import kotlinx.coroutines.flow.StateFlow

/**
 * Pipeline de captura de extremo a extremo.
 *
 * Accesibilidad → [CaptureRequest] → ScreenCapture → OCR → OfferParser →
 * CaptureRepository. Expone el [OverlayState] actual del ciclo de vida.
 */
interface CapturePipeline {
    val state: StateFlow<OverlayState>

    /** Procesa una solicitud y devuelve el snapshot generado, si lo hubo. */
    suspend fun process(request: CaptureRequest): OfferSnapshot?
}
