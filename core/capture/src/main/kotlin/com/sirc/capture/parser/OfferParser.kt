package com.sirc.capture.parser

import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.OfferSnapshot
import com.sirc.core.platform.DetectionResult

/**
 * Traduce una solicitud de captura en un [OfferSnapshot].
 *
 * Interfaz de parsing conectada al motor de análisis real
 * ([com.sirc.core.platform.OfferParserOrchestrator]) vía [PlatformOfferParser].
 */
interface OfferParser {
    fun parse(
        request: CaptureRequest,
        result: DetectionResult,
        detectionMillis: Double,
    ): OfferSnapshot?
}
