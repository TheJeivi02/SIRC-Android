package com.sirc.capture.metrics

/**
 * Últimos tiempos de procesamiento del pipeline, expuestos al panel de
 * depuración (solo Debug).
 */
data class ProcessingMetrics(
    val ocrMillis: Double? = null,
    val detectionMillis: Double? = null,
    val parseMillis: Double? = null,
    val totalMillis: Double? = null,
)
