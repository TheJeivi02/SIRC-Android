package com.sirc.capture.parser

import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.SnapshotSource
import com.sirc.core.platform.DetectionResult
import com.sirc.core.platform.OfferParserOrchestrator
import com.sirc.core.platform.OfferType
import com.sirc.core.platform.ParsedOffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser real que conecta el pipeline de captura con el motor de análisis de
 * [com.sirc.core.platform] (O2).
 *
 * Usa el [OfferParserOrchestrator] para detectar el tipo de pantalla y extraer
 * la oferta. Devuelve `null` cuando la pantalla no es una solicitud
 * (HOME/TRIP/NAVIGATION/OFFLINE/ERROR) o no se pudo extraer la oferta.
 */
@Singleton
class PlatformOfferParser @Inject constructor(
    private val orchestrator: OfferParserOrchestrator,
) : OfferParser {
    override fun parse(
        request: CaptureRequest,
        result: DetectionResult,
        detectionMillis: Double,
    ): OfferSnapshot? {
        val platform = result.descriptor?.platform ?: return null
        if (request.texts.isEmpty()) return null

        val parsed: ParsedOffer =
            orchestrator.parse(
                result = result,
                texts = request.texts,
                timestampMillis = request.timestampMillis,
                detectionMillis = detectionMillis,
            )
        val offer = parsed.offer ?: return null

        return OfferSnapshot(
            sessionId = "pipeline-${request.id}",
            platform = platform,
            capturedAtMillis = request.timestampMillis,
            source = SnapshotSource.REAL,
            estimatedTotal = offer.estimatedTotal ?: return null,
            distanceKm = offer.distanceKm ?: 0.0,
            durationMin = offer.durationMin ?: 0.0,
            rawData = rawDataFor(parsed.type),
            texts = request.texts,
            origin = request.origin,
            detectionMillis = parsed.detectionMillis,
        )
    }

    private fun rawDataFor(type: OfferType): String = "type=${type.name}"
}
