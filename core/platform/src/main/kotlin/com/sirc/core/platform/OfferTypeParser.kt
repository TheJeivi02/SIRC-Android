package com.sirc.core.platform

import com.sirc.domain.model.TripOffer

/**
 * Resultado del orquestador de parsing: la [type] de oferta detectada y la
 * [offer] extraída (si fue posible). [detectionMillis] es el tiempo de la
 * detección previa (Debug).
 */
data class ParsedOffer(
    val type: OfferType,
    val offer: TripOffer?,
    val detectionMillis: Double = 0.0,
) {
    companion object {
        fun none(
            type: OfferType = OfferType.GENERIC,
            detectionMillis: Double = 0.0,
        ): ParsedOffer = ParsedOffer(type = type, offer = null, detectionMillis = detectionMillis)
    }
}

/**
 * Interfaz común de los parsers especializados por tipo de oferta (O2).
 *
 * Cada parser decide si le corresponde ([matches]) y, en ese caso, extrae la
 * [TripOffer]. La detección del tipo ocurre ANTES del parsing; si un parser
 * matchea pero no logra extraer la oferta, el orquestador cede al siguiente.
 */
interface OfferTypeParser {
    val type: OfferType

    fun matches(texts: List<String>): Boolean

    fun extract(
        texts: List<String>,
        timestampMillis: Long,
    ): TripOffer?
}
