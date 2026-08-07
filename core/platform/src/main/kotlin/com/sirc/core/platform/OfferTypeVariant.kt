package com.sirc.core.platform

/**
 * Variante de oferta de una plataforma, tal como la describe su descriptor.
 *
 * Es un value type puro (tipo + keywords) que alimenta al parser genérico
 * ([GenericOfferTypeParser]). No es un subdescriptor de [PlatformDescriptor]:
 * es la semilla del futuro subdescriptor `OfferTypeDescriptor`, que podrá
 * absorberlo cuando se descomponga `PlatformDescriptor` sin romper la API
 * pública.
 */
data class OfferTypeVariant(
    val type: OfferType,
    val keywords: List<String>,
)
