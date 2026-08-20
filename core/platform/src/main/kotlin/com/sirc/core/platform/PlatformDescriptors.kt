package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Descriptores de las plataformas soportadas.
 *
 * Seed de [PlatformDescriptorRegistry]: define cada plataforma con los valores
 * que antes estaban hardcodeados en el motor (keywords de detección, variantes,
 * keywords de extracción y moneda). No añade plataformas nuevas; preserva el
 * comportamiento actual de Uber, DiDi, Cabify e InDrive.
 *
 * Las reglas de detección se siembran con las reglas por defecto del
 * [OfferDetectionEngine] (idénticas a las globales previas), de modo que el
 * comportamiento de detección no cambia.
 */
object PlatformDescriptors {
    val UBER: PlatformDescriptor =
        PlatformDescriptor(
            platform = RidePlatform.UBER,
            packageNames = listOf("com.ubercab", "com.ubercab.driver"),
            platformKeywords =
                listOf("uber", "uberx", "uberxl", "ubermoto", "uber xl", "uber moto"),
            detectionRules = OfferDetectionEngine.defaultRules(),
            offerTypes =
                listOf(
                    OfferTypeVariant(
                        type = OfferType.UBER_MOTO,
                        keywords = listOf("uber moto", "ubermoto", "moto", "viaje en moto", "2 ruedas"),
                    ),
                    OfferTypeVariant(
                        type = OfferType.UBER_XL,
                        keywords = listOf("uber xl", "uberxl", "xl", "6 pasajeros", "hasta 6"),
                    ),
                    OfferTypeVariant(
                        type = OfferType.UBER_RADAR,
                        keywords = listOf("radar", "explorar", "ve quién", "toca para ver", "zona de recogida"),
                    ),
                    OfferTypeVariant(
                        type = OfferType.UBER_RESERVATION,
                        keywords = listOf("reserva", "reservado", "programado", "viaje futuro", "recogida programada"),
                    ),
                    OfferTypeVariant(
                        type = OfferType.UBER_REQUEST,
                        keywords =
                            listOf(
                                "nueva solicitud",
                                "solicitud de viaje",
                                "aceptar",
                                "rechazar",
                                "ganancia estimada",
                            ),
                    ),
                ),
            extractorKeywords =
                PlatformKeywords(
                    totalKeywords = listOf("total", "recibe", "neto", "cobro", "ingreso", "pago"),
                    fareKeywords = listOf("tarifa", "fare", "precio"),
                ),
            defaultCurrency = "MXN",
        )

    val DIDI: PlatformDescriptor =
        PlatformDescriptor(
            platform = RidePlatform.DIDI,
            platformKeywords = listOf("didi", "didimovil", "didi movil", "didi chofer"),
            detectionRules = OfferDetectionEngine.defaultRules(),
            offerTypes = emptyList(),
            extractorKeywords =
                PlatformKeywords(
                    totalKeywords = listOf("total", "monto", "ingreso", "ganancia"),
                    fareKeywords = listOf("tarifa", "precio"),
                ),
            defaultCurrency = "MXN",
        )

    val CABIFY: PlatformDescriptor =
        PlatformDescriptor(
            platform = RidePlatform.CABIFY,
            platformKeywords = listOf("cabify", "cabifyxl", "cabify xl"),
            detectionRules = OfferDetectionEngine.defaultRules(),
            offerTypes = emptyList(),
            extractorKeywords =
                PlatformKeywords(
                    totalKeywords = listOf("total", "recibe", "neto"),
                    fareKeywords = listOf("tarifa", "precio", "fare"),
                ),
            defaultCurrency = "EUR",
        )

    val INDRIVE: PlatformDescriptor =
        PlatformDescriptor(
            platform = RidePlatform.INDRIVE,
            packageNames = listOf("com.leadingsoft.ride.driver", "sinet.startup.inDriver"),
            platformKeywords = listOf("indriver", "indrive", "in driver", "in drive"),
            detectionRules = OfferDetectionEngine.defaultRules(),
            offerTypes = emptyList(),
            extractorKeywords =
                PlatformKeywords(
                    totalKeywords = listOf("oferta", "precio", "total", "monto", "aceptar"),
                    fareKeywords = listOf("pago", "tarifa"),
                ),
            defaultCurrency = "MXN",
        )

    fun all(): List<PlatformDescriptor> = listOf(UBER, DIDI, CABIFY, INDRIVE)
}
