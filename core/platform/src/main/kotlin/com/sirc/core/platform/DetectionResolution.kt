package com.sirc.core.platform

/**
 * Forma oficial de indicar cómo se detectó la plataforma.
 */
enum class DetectionResolution {
    /** La plataforma se resolvió por coincidencia exacta de packageName. */
    PACKAGE_MATCH,

    /** La plataforma se resolvió por keywords de detección (único candidato). */
    KEYWORD_CANDIDATE,

    /** Varios descriptores empataron en prioridad; no se elige ninguno. */
    AMBIGUOUS,

    /** Ningún descriptor coincidió. */
    NONE,
}
