package com.sirc.capture.parser

import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot

/**
 * Traduce un evento capturado en un [OfferSnapshot].
 *
 * Interfaz de parsing conectada al motor de análisis real
 * ([com.sirc.core.platform.OfferParserOrchestrator]) vía [PlatformOfferParser].
 */
interface OfferParser {
    fun parse(
        event: CaptureWindowEvent,
        session: OfferCaptureSession,
    ): OfferSnapshot?
}
