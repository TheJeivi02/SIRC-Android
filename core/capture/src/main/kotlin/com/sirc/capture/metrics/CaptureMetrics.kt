package com.sirc.capture.metrics

/**
 * Métricas de rendimiento por etapa del pipeline de captura.
 *
 * Las implementaciones reales solo deben loguear en compilaciones Debug.
 */
interface CaptureMetrics {
    fun onOcr(millis: Double) = Unit

    fun onParse(millis: Double) = Unit

    fun onTotal(millis: Double) = Unit
}
