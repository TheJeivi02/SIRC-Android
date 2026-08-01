package com.sirc.domain.model

/**
 * Entrada del historial persistente de ofertas evaluadas (Room).
 *
 * Además de los datos básicos, conserva el análisis detallado: tipo de oferta,
 * confianza, resumen de reglas, razones y tiempos de procesamiento, de modo
 * que el historial sirva también de diagnóstico sin depender del panel de
 * depuración.
 */
data class OfferHistoryEntry(
    val id: Long = 0L,
    val platform: RidePlatform,
    val timestampMillis: Long,
    val estimatedTotal: Double?,
    val distanceKm: Double?,
    val durationMin: Double?,
    val estimatedProfit: Double,
    val decision: Decision,
    val summary: String,
    val offerType: String? = null,
    val confidencePercent: Int? = null,
    val confidenceLevel: String? = null,
    val ruleSummary: String? = null,
    val reasons: String? = null,
    val recommendation: Recommendation? = null,
    val processingMillis: Double? = null,
    val evaluationMillis: Double? = null,
    val rulesMillis: Double? = null,
)
