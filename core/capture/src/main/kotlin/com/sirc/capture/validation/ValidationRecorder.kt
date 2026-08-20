package com.sirc.capture.validation

import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Acumulador en memoria de [ValidationEvent] para el modo de validación.
 *
 * Mantiene un buffer acotado (los más recientes), permite limpiarlo, obtener
 * un resumen ([ValidationSummary]) y exportar un informe legible
 * ([buildReport]) para compartir con soporte. Totalmente desacoplado de
 * Android.
 */
@Singleton
class ValidationRecorder @Inject constructor() {
    private val lock = ReentrantLock()
    private val events = mutableListOf<ValidationEvent>()

    fun record(event: ValidationEvent) {
        lock.withLock {
            events += event
            val overflow = events.size - MAX_EVENTS
            if (overflow > 0) {
                events.removeAll(events.take(overflow))
            }
        }
    }

    fun clear() {
        lock.withLock { events.clear() }
    }

    fun snapshot(): List<ValidationEvent> = lock.withLock { events.toList() }

    fun summary(): ValidationSummary =
        lock.withLock {
            ValidationSummary(
                total = events.size,
                captureErrors = events.count { it is ValidationEvent.CaptureError },
                ocrFailed = events.count { it is ValidationEvent.OcrFailed },
                parseFailed = events.count { it is ValidationEvent.ParseFailed },
                discarded = events.count { it is ValidationEvent.FrameDiscarded },
                ruleFailed = events.count { it is ValidationEvent.RuleFailed },
                rejected = events.count { it is ValidationEvent.OfferRejected },
            )
        }

    /** Exporta un informe de validación legible para compartir con soporte. */
    fun buildReport(): String {
        val summary = summary()
        val events = snapshot()
        val discardedByReason =
            events
                .filterIsInstance<ValidationEvent.FrameDiscarded>()
                .groupingBy { it.reason.name }
                .eachCount()
        return buildString {
            appendLine("SIRC · Informe de validación")
            appendLine("Fecha: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("== Resumen ==")
            appendLine("Total eventos: ${summary.total}")
            appendLine("Errores de captura: ${summary.captureErrors}")
            appendLine("Errores OCR: ${summary.ocrFailed}")
            appendLine("Errores de parseo: ${summary.parseFailed}")
            appendLine(
                "Capturas descartadas: ${summary.discarded} " +
                    discardedDetail(discardedByReason),
            )
            appendLine("Ofertas rechazadas: ${summary.rejected}")
            appendLine()
            appendLine("== Detalle ==")
            if (events.isEmpty()) {
                appendLine("Sin eventos registrados.")
            } else {
                events.forEach { event ->
                    appendLine(format(event))
                }
            }
        }
    }

    private fun format(event: ValidationEvent): String =
        when (event) {
            is ValidationEvent.OcrFailed ->
                "${timestamp(event.timestampMillis)} OCR_ERROR · ${event.message}"

            is ValidationEvent.ParseFailed ->
                "${timestamp(event.timestampMillis)} PARSE_ERROR · ${event.message}"

            is ValidationEvent.CaptureError ->
                "${timestamp(event.timestampMillis)} CAPTURE_ERROR · ${event.message}"

            is ValidationEvent.FrameDiscarded ->
                "${timestamp(event.timestampMillis)} DESCARTE · ${event.reason.name}"

            is ValidationEvent.RuleFailed ->
                "${timestamp(event.timestampMillis)} REGLA_${event.verdict} · ${event.ruleName} · ${event.message}"

            is ValidationEvent.OfferRejected ->
                "${timestamp(event.timestampMillis)} RECHAZO · ${event.reason}"
        }

    private fun timestamp(millis: Long): String = String.format(Locale.ROOT, "%1\$tH:%1\$tM:%1\$tS", millis)

    private fun discardedDetail(discardedByReason: Map<String, Int>): String =
        if (discardedByReason.isEmpty()) {
            ""
        } else {
            discardedByReason.entries.joinToString(" ") { "${it.key}:${it.value}" }
        }

    companion object {
        private const val MAX_EVENTS = 500
    }
}

/** Conteo de eventos de validación por tipo. */
data class ValidationSummary(
    val total: Int,
    val captureErrors: Int,
    val ocrFailed: Int,
    val parseFailed: Int,
    val discarded: Int,
    val ruleFailed: Int,
    val rejected: Int,
)
