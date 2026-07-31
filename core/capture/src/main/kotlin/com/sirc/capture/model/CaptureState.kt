package com.sirc.capture.model

/**
 * Estado observable del pipeline de captura, consumido por el panel de
 * depuración.
 */
data class CaptureState(
    val isCapturing: Boolean = false,
    val activeSession: OfferCaptureSession? = null,
    val lastSnapshot: OfferSnapshot? = null,
    val lastProcessingTimeMillis: Double? = null,
    val eventsProcessed: Int = 0,
    val recentEvents: List<CaptureWindowEvent> = emptyList(),
)
