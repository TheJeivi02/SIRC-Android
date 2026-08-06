package com.sirc.core.platform

import com.sirc.domain.model.TripOffer

/**
 * Variante de oferta de una plataforma, tal como la describe su descriptor.
 *
 * Es un value type puro (tipo + keywords + refinamiento opcional) que alimenta
 * al parser genérico ([GenericOfferTypeParser]). No es un subdescriptor de
 * [PlatformDescriptor]: es la semilla del futuro subdescriptor
 * `OfferTypeDescriptor`, que podrá absorberlo cuando se descomponga
 * `PlatformDescriptor` sin romper la API pública.
 */
data class OfferTypeVariant(
    val type: OfferType,
    val keywords: List<String>,
    /** Refinamiento opcional de la oferta extraída (p. ej. duración default). */
    val refine: ((TripOffer, List<String>) -> TripOffer)? = null,
)
