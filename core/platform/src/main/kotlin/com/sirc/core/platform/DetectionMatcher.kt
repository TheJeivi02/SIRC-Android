package com.sirc.core.platform

/**
 * Matcher puro, determinista y sin estado de la detección genérica.
 *
 * Normaliza (minúsculas/sin acentos) vía [OfferDetectionEngine.normalize] y
 * compara contra los campos de [PlatformDescriptor]. No conoce de OCR ni de
 * ninguna fuente de textos.
 */
object DetectionMatcher {
    /** True si [packageName] coincide (normalizado) con alguno de [packageNames]. */
    fun matchesPackage(
        packageNames: List<String>,
        packageName: String,
    ): Boolean {
        val normalized = OfferDetectionEngine.normalize(packageName)
        return packageNames.any { OfferDetectionEngine.normalize(it) == normalized }
    }

    /**
     * Puntúa [descriptor] por los identificadores fuertes de plataforma
     * ([PlatformDescriptor.platformKeywords]) presentes en [normalizedTexts].
     * Solo marcas y patrones específicos de cada plataforma puntúan: las
     * keywords genéricas de pantalla (aceptar, viaje, oferta, tarifa...) no
     * determinan la plataforma y evitan falsos positivos (G2).
     */
    fun matchScore(
        descriptor: PlatformDescriptor,
        normalizedTexts: List<String>,
    ): Int {
        val keywords = descriptor.platformKeywords.distinct()
        return keywords.count { keyword ->
            normalizedTexts.any { it.contains(OfferDetectionEngine.normalize(keyword)) }
        }
    }
}
