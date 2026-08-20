package com.sirc.domain.model

/**
 * Entrada del historial temporal de ofertas evaluadas (en memoria).
 *
 * Conserva la traza completa del análisis: datos de la oferta, texto OCR,
 * resultado del parser y la evaluación + recomendación finales.
 */
data class OfferEvaluationRecord(
    val id: Long,
    val timestampMillis: Long,
    val platform: RidePlatform,
    val price: Double,
    val distanceKm: Double?,
    val durationMin: Double,
    val ocrText: List<String>,
    val parserResult: String?,
    val evaluation: ProfitEvaluation,
    val recommendation: Recommendation,
    val confidencePercent: Int,
)
