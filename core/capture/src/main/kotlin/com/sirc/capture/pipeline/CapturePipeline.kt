package com.sirc.capture.pipeline

import com.sirc.capture.metrics.ProcessingMetrics
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pipeline de captura de extremo a extremo.
 *
 * [CaptureRequest] → (OCR si hay imagen) → detección de plataforma →
 * OfferParser → CaptureRepository. Expone el [OverlayState] actual del ciclo
 * de vida, los [snapshots] producidos y las [lastMetrics] de rendimiento
 * (Debug).
 */
interface CapturePipeline {
    val state: StateFlow<OverlayState>

    /** Snapshots producidos y guardados, para consumidores del overlay. */
    val snapshots: SharedFlow<OfferSnapshot>

    /** Últimas métricas por etapa (captura/OCR/parser/total). */
    val lastMetrics: StateFlow<ProcessingMetrics>

    /** Procesa una solicitud y devuelve el snapshot generado, si lo hubo. */
    suspend fun process(request: CaptureRequest): OfferSnapshot?
}
