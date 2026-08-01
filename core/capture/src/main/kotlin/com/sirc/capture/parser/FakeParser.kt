package com.sirc.capture.parser

import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.SnapshotSource
import com.sirc.domain.model.RidePlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser simulado: genera un [OfferSnapshot] con valores de prueba para
 * validar el flujo completo de captura. NO interpreta pantallas reales.
 */
@Singleton
class FakeParser @Inject constructor() : OfferParser {
    override fun parse(
        event: CaptureWindowEvent,
        session: OfferCaptureSession,
    ): OfferSnapshot? {
        val platform = RidePlatform.fromPackageName(event.packageName) ?: return null
        return OfferSnapshot(
            sessionId = session.id,
            platform = platform,
            capturedAtMillis = event.timestampMillis,
            source = SnapshotSource.FAKE,
            estimatedTotal = FAKE_ESTIMATED_TOTAL,
            distanceKm = FAKE_DISTANCE_KM,
            durationMin = FAKE_DURATION_MIN,
            rawData = FAKE_RAW_DATA,
            texts = event.texts,
        )
    }

    companion object {
        const val FAKE_ESTIMATED_TOTAL = 125.0
        const val FAKE_DISTANCE_KM = 8.5
        const val FAKE_DURATION_MIN = 22.0
        private const val FAKE_RAW_DATA = "data:simulated"
    }
}
