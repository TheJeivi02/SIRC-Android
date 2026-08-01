package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import javax.inject.Inject

/**
 * Base común de los parsers especializados.
 *
 * Centraliza la normalización de texto y la extracción final vía el
 * [GenericPlatformExtractor] de Uber, de modo que cada parser especializado
 * solo aporta sus palabras clave ([keywords]) y, opcionalmente, refina la
 * oferta ([refine]).
 */
abstract class BaseOfferTypeParser(
    private val extractor: GenericPlatformExtractor =
        GenericPlatformExtractor(RidePlatform.UBER, PlatformDescriptors.UBER),
) : OfferTypeParser {
    protected abstract val keywords: List<String>

    override fun matches(texts: List<String>): Boolean {
        val normalized = texts.map { OfferDetectionEngine.normalize(it) }
        return keywords.any { kw -> normalized.any { it.contains(OfferDetectionEngine.normalize(kw)) } }
    }

    override fun extract(
        texts: List<String>,
        timestampMillis: Long,
    ): TripOffer? {
        val offer = extractor.extract(texts, timestampMillis) ?: return null
        return refine(offer, texts)
    }

    /** Permite ajustar la oferta según la variante (p. ej. duración default). */
    protected open fun refine(
        offer: TripOffer,
        texts: List<String>,
    ): TripOffer = offer
}

/**
 * Parser de la solicitud estándar de Uber (X, Comfort, etc.).
 */
class UberRequestParser @Inject constructor() : BaseOfferTypeParser() {
    override val type: OfferType = OfferType.UBER_REQUEST
    override val keywords: List<String> =
        listOf(
            "nueva solicitud",
            "solicitud de viaje",
            "aceptar",
            "rechazar",
            "ganancia estimada",
        )
}

/**
 * Parser de ofertas de radar (explorar el mapa buscando viajes cercanos).
 */
class UberRadarParser @Inject constructor() : BaseOfferTypeParser() {
    override val type: OfferType = OfferType.UBER_RADAR
    override val keywords: List<String> =
        listOf(
            "radar",
            "explorar",
            "ve quién",
            "toca para ver",
            "zona de recogida",
        )
}

/**
 * Parser de viajes reservados o programados.
 */
class UberReservationParser @Inject constructor() : BaseOfferTypeParser() {
    override val type: OfferType = OfferType.UBER_RESERVATION
    override val keywords: List<String> =
        listOf(
            "reserva",
            "reservado",
            "programado",
            "viaje futuro",
            "recogida programada",
        )
}

/**
 * Parser de viajes en Uber Moto (2 ruedas).
 */
class UberMotoParser @Inject constructor() : BaseOfferTypeParser() {
    override val type: OfferType = OfferType.UBER_MOTO
    override val keywords: List<String> =
        listOf(
            "uber moto",
            "ubermoto",
            "moto",
            "viaje en moto",
            "2 ruedas",
        )
}

/**
 * Parser de viajes en Uber XL (vehículos para 6+ pasajeros).
 */
class UberXlParser @Inject constructor() : BaseOfferTypeParser() {
    override val type: OfferType = OfferType.UBER_XL
    override val keywords: List<String> =
        listOf(
            "uber xl",
            "uberxl",
            "xl",
            "6 pasajeros",
            "hasta 6",
        )
}
