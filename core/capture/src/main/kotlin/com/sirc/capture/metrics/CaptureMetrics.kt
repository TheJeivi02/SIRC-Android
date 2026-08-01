package com.sirc.capture.metrics

/**
 * Métricas de rendimiento por etapa del pipeline de captura.
 *
 * Las implementaciones reales solo deben loguear en compilaciones Debug; el
 * [NoOpCaptureMetrics] se usa en pruebas y en entornos no Android.
 */
interface CaptureMetrics {
    fun onCapture(millis: Double) = Unit

    fun onOcr(millis: Double) = Unit

    fun onParse(millis: Double) = Unit

    fun onTotal(millis: Double) = Unit
}

/** Métricas sin operación: útil para pruebas y builds de producción. */
object NoOpCaptureMetrics : CaptureMetrics
