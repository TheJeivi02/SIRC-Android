package com.sirc.capture.android.metrics

import android.content.Context
import android.content.pm.ApplicationInfo
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.CaptureMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Métricas de rendimiento del pipeline, disponibles solo en compilaciones
 * Debug. En producción no hacen nada.
 */
@Singleton
class DebugCaptureMetrics @Inject constructor(
    @ApplicationContext context: Context,
    private val logger: SircLogger,
) : CaptureMetrics {
    private val enabled = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onCapture(millis: Double) {
        if (enabled) logger.debug(TAG, "captura: ${format(millis)} ms")
    }

    override fun onOcr(millis: Double) {
        if (enabled) logger.debug(TAG, "ocr: ${format(millis)} ms")
    }

    override fun onParse(millis: Double) {
        if (enabled) logger.debug(TAG, "parseo: ${format(millis)} ms")
    }

    override fun onTotal(millis: Double) {
        if (enabled) logger.info(TAG, "tiempo total: ${format(millis)} ms")
    }

    private fun format(millis: Double): String = String.format(LOCALE, "%.1f", millis)

    companion object {
        private const val TAG = "PipelineMetrics"
        private val LOCALE = java.util.Locale.ROOT
    }
}
