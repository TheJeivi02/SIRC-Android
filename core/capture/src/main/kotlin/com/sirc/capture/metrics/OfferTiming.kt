package com.sirc.capture.metrics

/**
 * Tiempos por etapa de una oferta procesada, expuestos al panel de depuración
 * (solo Debug).
 *
 * Las etapas [captureMillis], [ocrMillis], [detectionMillis], [parseMillis] y
 * [totalMillis] las registra el pipeline; [rulesMillis], [evaluationMillis] y
 * [overlayMillis] las registra el overlay al analizar y mostrar el resultado.
 */
data class OfferTiming(
    val captureMillis: Double? = null,
    val ocrMillis: Double? = null,
    val detectionMillis: Double? = null,
    val parseMillis: Double? = null,
    val rulesMillis: Double? = null,
    val evaluationMillis: Double? = null,
    val overlayMillis: Double? = null,
    val totalMillis: Double? = null,
) {
    /** Combina con [other] conservando el primer valor no nulo de cada etapa. */
    fun merge(other: OfferTiming): OfferTiming =
        OfferTiming(
            captureMillis = other.captureMillis ?: captureMillis,
            ocrMillis = other.ocrMillis ?: ocrMillis,
            detectionMillis = other.detectionMillis ?: detectionMillis,
            parseMillis = other.parseMillis ?: parseMillis,
            rulesMillis = other.rulesMillis ?: rulesMillis,
            evaluationMillis = other.evaluationMillis ?: evaluationMillis,
            overlayMillis = other.overlayMillis ?: overlayMillis,
            totalMillis = other.totalMillis ?: totalMillis,
        )
}
