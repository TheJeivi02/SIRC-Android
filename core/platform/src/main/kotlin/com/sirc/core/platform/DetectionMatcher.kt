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
     * Puntúa [descriptor] por las keywords de detección presentes en
     * [normalizedTexts]. Por ahora el score es el número de keywords (de todas
     * las [DetectionRule]s del descriptor, sin duplicados); el nombre permite
     * evolucionar el algoritmo sin romper la API.
     */
    fun matchScore(
        descriptor: PlatformDescriptor,
        normalizedTexts: List<String>,
    ): Int {
        val keywords = descriptor.detectionRules.flatMap { it.keywords }.distinct()
        return keywords.count { keyword ->
            normalizedTexts.any { it.contains(OfferDetectionEngine.normalize(keyword)) }
        }
    }
}
