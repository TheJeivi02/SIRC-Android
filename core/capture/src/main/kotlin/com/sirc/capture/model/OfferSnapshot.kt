package com.sirc.capture.model

import com.sirc.domain.model.RidePlatform

/**
 * Estado capturado de una oferta, inmutable.
 *
 * Por ahora contiene únicamente información simulada ([SnapshotSource.FAKE]);
 * cuando se conecte el parser/OCR real pasará a [SnapshotSource.REAL].
 */
data class OfferSnapshot(
    val sessionId: String,
    val platform: RidePlatform,
    val capturedAtMillis: Long,
    val source: SnapshotSource,
    val estimatedTotal: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val rawData: String? = null,
    val texts: List<String> = emptyList(),
)

/** Origen de los datos del [OfferSnapshot]. */
enum class SnapshotSource {
    FAKE,
    REAL,
}
