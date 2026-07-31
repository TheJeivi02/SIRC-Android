package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer

/**
 * Extrae una [TripOffer] estructurada a partir de los textos visibles en pantalla.
 *
 * Cada plataforma aporta su propia estrategia de reconocimiento basada en
 * palabras clave y patrones. SIRC solo LEE la información visible: nunca
 * interactúa con la interfaz de la aplicación de transporte.
 */
interface PlatformExtractor {
    val platform: RidePlatform

    fun extract(
        texts: List<String>,
        timestampMillis: Long,
    ): TripOffer?
}
