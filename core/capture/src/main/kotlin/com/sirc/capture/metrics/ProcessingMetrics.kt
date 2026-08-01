package com.sirc.capture.metrics

/**
 * Últimos tiempos de procesamiento del pipeline, expuestos al panel de
 * depuración (solo Debug).
 */
data class ProcessingMetrics(
    val captureMillis: Double? = null,
    val ocrMillis: Double? = null,
    val parseMillis: Double? = null,
    val totalMillis: Double? = null,
)
