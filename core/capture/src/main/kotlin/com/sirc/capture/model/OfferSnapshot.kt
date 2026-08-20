package com.sirc.capture.model

import com.sirc.core.platform.CaptureInputType
import com.sirc.domain.model.RidePlatform

/**
 * Estado capturado de una oferta, inmutable.
 *
 * Contiene información analizada desde textos/OCR de pantallas reales
 * ([SnapshotSource.REAL]).
 */
data class OfferSnapshot(
    val sessionId: String,
    val platform: RidePlatform,
    val capturedAtMillis: Long,
    val source: SnapshotSource,
    val estimatedTotal: Double,
    val distanceKm: Double?,
    val durationMin: Double,
    val rawData: String? = null,
    val texts: List<String> = emptyList(),
    val detectionMillis: Double? = null,
    val origin: CaptureInputType = CaptureInputType.UNKNOWN,
)

/** Origen de los datos del [OfferSnapshot]. */
enum class SnapshotSource {
    FAKE,
    REAL,
}
