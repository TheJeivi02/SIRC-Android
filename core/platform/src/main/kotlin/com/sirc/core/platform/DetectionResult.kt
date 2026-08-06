package com.sirc.core.platform

/**
 * Resultado autocontenido de la detección genérica de plataforma.
 *
 * Encapsula toda la información necesaria para continuar el parseo
 * ([descriptor], [screenDetection]) y para diagnosticar por qué se llegó al
 * resultado ([resolution], [candidates], [sourcePackage]). No vuelve a recorrer
 * descriptores: el [OfferParserOrchestrator] consume este objeto tal cual.
 */
data class DetectionResult(
    val resolution: DetectionResolution,
    val origin: DetectionOrigin,
    val descriptor: PlatformDescriptor? = null,
    val screenDetection: ScreenDetection = ScreenDetection(ScreenType.UNKNOWN),
    val candidates: List<DetectionCandidate> = emptyList(),
    val sourcePackage: String? = null,
) {
    val isRecognized: Boolean
        get() = resolution == DetectionResolution.PACKAGE_MATCH || resolution == DetectionResolution.KEYWORD_CANDIDATE
}
