package com.sirc.core.platform

import com.sirc.domain.model.TripOffer

/**
 * Parser genérico de variantes, dirigido por un [OfferTypeVariant] del
 * descriptor de la plataforma.
 *
 * Reemplaza a los parsers especializados por tipo (antes `Uber*Parser`): no
 * contiene ningún valor hardcodeado de plataforma; solo consume el descriptor.
 */
class GenericOfferTypeParser(
    private val variant: OfferTypeVariant,
    private val extractor: GenericPlatformExtractor,
) : OfferTypeParser {
    override val type: OfferType = variant.type

    override fun matches(texts: List<String>): Boolean {
        val normalized = texts.map { OfferDetectionEngine.normalize(it) }
        return variant.keywords.any { kw -> normalized.any { it.contains(OfferDetectionEngine.normalize(kw)) } }
    }

    override fun extract(
        texts: List<String>,
        timestampMillis: Long,
    ): TripOffer? {
        val offer = extractor.extract(texts, timestampMillis) ?: return null
        return offer
    }
}
