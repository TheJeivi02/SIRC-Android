package com.sirc.capture.parser

import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.SnapshotSource
import com.sirc.core.platform.OfferParserOrchestrator
import com.sirc.core.platform.OfferType
import com.sirc.domain.model.RidePlatform
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
        event: CaptureWindowEvent,
        session: OfferCaptureSession,
    ): OfferSnapshot? {
        val platform = RidePlatform.fromPackageName(event.packageName) ?: return null
        if (event.texts.isEmpty()) return null

        val parsed =
            orchestrator.parse(
                texts = event.texts,
                timestampMillis = event.timestampMillis,
                platform = platform,
            )
        val offer = parsed.offer ?: return null

        return OfferSnapshot(
            sessionId = session.id,
            platform = platform,
            capturedAtMillis = event.timestampMillis,
            source = SnapshotSource.REAL,
            estimatedTotal = offer.estimatedTotal ?: return null,
            distanceKm = offer.distanceKm ?: 0.0,
            durationMin = offer.durationMin ?: 0.0,
            rawData = rawDataFor(parsed.type),
            texts = event.texts,
            detectionMillis = parsed.detectionMillis,
        )
    }

    private fun rawDataFor(type: OfferType): String = "type=${type.name}"
}
