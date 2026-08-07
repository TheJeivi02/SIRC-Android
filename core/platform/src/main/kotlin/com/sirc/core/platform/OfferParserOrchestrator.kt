package com.sirc.core.platform

/**
 * Orquestador de Parsing (O2).
 *
 * Consume un [DetectionResult] ya resuelto por [PlatformDetectionEngine] y
 * extrae la oferta del texto visible: si la detección indica una pantalla de
 * oferta ([ScreenDetection.isRequest]), prueba las variantes del descriptor en
 * orden de especificidad. Si ninguna matchea o extrae, cae al extractor
 * genérico de la plataforma.
 *
 * Es 100 % descriptor-driven y **no ejecuta detección**: la resolución de
 * plataforma ocurre antes, en [PlatformDetectionEngine] (única fuente de
 * verdad, WP-E3-02 / WP-E3-05A). Este es el único punto de entrada que usa el
 * pipeline de captura.
 */
class OfferParserOrchestrator(
    private val platformRegistry: PlatformDescriptorRegistry,
) {
    /**
     * Clasifica [texts] usando un [DetectionResult] ya resuelto.
     *
     * No re-resuelve la plataforma ni ejecuta detección.
     *
     * @param result detección previa (descriptor + pantalla) del
     *   [PlatformDetectionEngine].
     * @return [ParsedOffer] con el tipo detectado y la oferta extraída, o
     *   `offer = null` si la pantalla no era una solicitud, el descriptor es nulo
     *   o no se pudo parsear.
     */
    fun parse(
        result: DetectionResult,
        texts: List<String>,
        timestampMillis: Long,
        detectionMillis: Double = 0.0,
    ): ParsedOffer {
        if (!result.isRecognized || !result.screenDetection.isRequest) {
            return ParsedOffer.none()
        }
        val descriptor = result.descriptor ?: return ParsedOffer.none()
        return parseWith(
            descriptor = descriptor,
            screenDetection = result.screenDetection,
            texts = texts,
            timestampMillis = timestampMillis,
            detectionMillis = detectionMillis,
        )
    }

    private fun parseWith(
        descriptor: PlatformDescriptor,
        screenDetection: ScreenDetection,
        texts: List<String>,
        timestampMillis: Long,
        detectionMillis: Double,
    ): ParsedOffer {
        val normalized = texts.map(OfferDetectionEngine::normalize)
        for (parser in platformRegistry.variantParsersFor(descriptor.platform)) {
            if (parser.matches(normalized)) {
                val offer = parser.extract(texts, timestampMillis)
                if (offer != null) {
                    return ParsedOffer(
                        type = parser.type,
                        offer = offer,
                        detectionMillis = detectionMillis,
                    )
                }
            }
        }

        val generic = platformRegistry.extractorFor(descriptor.platform)?.extract(texts, timestampMillis)
        return ParsedOffer(
            type = OfferType.GENERIC,
            offer = generic,
            detectionMillis = detectionMillis,
        )
    }
}
