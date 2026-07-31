package com.sirc.capture.model

/**
 * Sesión activa de captura.
 *
 * Representa el periodo en el que se observa la ventana de una misma
 * plataforma; se cierra cuando la ventana deja de ser relevante.
 */
data class OfferCaptureSession(
    val id: String,
    val startedAtMillis: Long,
    val packageName: String,
    val status: CaptureSessionStatus,
    val capturedSnapshotCount: Int = 0,
)

/** Estado de una [OfferCaptureSession]. */
enum class CaptureSessionStatus {
    ACTIVE,
    CLOSED,
}
