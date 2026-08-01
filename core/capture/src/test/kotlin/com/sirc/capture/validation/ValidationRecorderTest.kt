package com.sirc.capture.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationRecorderTest {
    private val recorder = ValidationRecorder()

    @Test
    fun `inicia vacio`() {
        assertEquals(0, recorder.summary().total)
        assertTrue(recorder.snapshot().isEmpty())
    }

    @Test
    fun `resumen cuenta los eventos por tipo`() {
        recorder.record(ValidationEvent.CaptureError(0L, "capture"))
        recorder.record(ValidationEvent.OcrFailed(1L, "ocr"))
        recorder.record(ValidationEvent.ParseFailed(2L, "parse"))
        recorder.record(ValidationEvent.FrameDiscarded(3L, DiscardReason.DUPLICATE))
        recorder.record(ValidationEvent.RuleFailed(4L, "ganancia", "FAIL", "bajo"))
        recorder.record(ValidationEvent.OfferRejected(5L, "no conviene"))

        val summary = recorder.summary()

        assertEquals(6, summary.total)
        assertEquals(1, summary.captureErrors)
        assertEquals(1, summary.ocrFailed)
        assertEquals(1, summary.parseFailed)
        assertEquals(1, summary.discarded)
        assertEquals(1, summary.ruleFailed)
        assertEquals(1, summary.rejected)
    }

    @Test
    fun `buffer se acota a los eventos mas recientes`() {
        repeat(600) { index ->
            recorder.record(ValidationEvent.FrameDiscarded(index.toLong(), DiscardReason.NO_TEXTS))
        }

        assertEquals(500, recorder.snapshot().size)
        assertEquals(500, recorder.summary().total)
        assertEquals(100L, recorder.snapshot().first().timestampMillis)
    }

    @Test
    fun `clear vacia el buffer`() {
        recorder.record(ValidationEvent.OcrFailed(1L, "ocr"))

        recorder.clear()

        assertEquals(0, recorder.summary().total)
    }

    @Test
    fun `buildReport incluye resumen y detalle`() {
        recorder.record(ValidationEvent.OcrFailed(1_700_000_000_000L, "OCR boom"))
        recorder.record(ValidationEvent.FrameDiscarded(1_700_000_000_001L, DiscardReason.DUPLICATE))

        val report = recorder.buildReport()

        assertTrue(report.contains("SIRC · Informe de validación"))
        assertTrue(report.contains("Errores OCR: 1"))
        assertTrue(report.contains("Capturas descartadas: 1"))
        assertTrue(report.contains("OCR_ERROR · OCR boom"))
        assertTrue(report.contains("DESCARTE · DUPLICATE"))
    }
}
