package com.sirc.capture.parser

import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot

/**
 * Traduce un evento capturado en un [OfferSnapshot].
 *
 * Interfaz preparada para recibir en el futuro un parser real (texto) o OCR;
 * hoy solo existe la implementación simulada [FakeParser].
 */
interface OfferParser {
    fun parse(
        event: CaptureWindowEvent,
        session: OfferCaptureSession,
    ): OfferSnapshot?
}
