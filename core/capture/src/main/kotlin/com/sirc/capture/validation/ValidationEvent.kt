package com.sirc.capture.validation

/**
 * Evento de validación registrado por el pipeline y la evaluación.
 *
 * Agrupa los incidentes que interesan al modo de validación (RC1): fallos de
 * OCR y parseo, capturas descartadas, reglas fallidas y ofertas rechazadas.
 */
sealed interface ValidationEvent {
    val timestampMillis: Long

    /** El OCR no pudo reconocer texto de un frame con imagen. */
    data class OcrFailed(
        override val timestampMillis: Long,
        val message: String,
    ) : ValidationEvent

    /** El parser no pudo producir un snapshot a partir de un evento. */
    data class ParseFailed(
        override val timestampMillis: Long,
        val message: String,
    ) : ValidationEvent

    /** Un error no controlado interrumpió el pipeline o la infraestructura. */
    data class CaptureError(
        override val timestampMillis: Long,
        val message: String,
    ) : ValidationEvent

    /** Un frame no llegó al pipeline por un motivo controlado. */
    data class FrameDiscarded(
        override val timestampMillis: Long,
        val reason: DiscardReason,
    ) : ValidationEvent

    /** Una regla de decisión terminó en FAIL para la oferta evaluada. */
    data class RuleFailed(
        override val timestampMillis: Long,
        val ruleName: String,
        val verdict: String,
        val message: String,
    ) : ValidationEvent

    /** La recomendación final fue REJECT para la oferta evaluada. */
    data class OfferRejected(
        override val timestampMillis: Long,
        val reason: String,
    ) : ValidationEvent
}

/** Motivo por el que un frame capturado se descartó antes de parsear. */
enum class DiscardReason {
    /** Frame idéntico a uno ya procesado (deduplicación por caché). */
    DUPLICATE,

    /** El frame no contenía textos útiles. */
    NO_TEXTS,

    /** El paquete no corresponde a una plataforma soportada. */
    UNSUPPORTED_PLATFORM,
}
