package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Orquestador de Parsing (O2).
 *
 * Decide qué [OfferTypeParser] aplicar al texto visible: primero detecta si
 * realmente hay una pantalla de oferta ([OfferDetectionEngine]) y, si es así,
 * prueba los parsers especializados en orden de especificidad. Si ninguno
 * matchea o extrae, cae al extractor genérico por plataforma.
 *
 * Es el único punto de entrada que usa el pipeline de captura.
 */
class OfferParserOrchestrator(
    private val detectionEngine: OfferDetectionEngine,
    private val specializedParsers: List<OfferTypeParser>,
    private val registry: ExtractorRegistry = ExtractorRegistry(OfferTextParser()),
) {
    /**
     * Clasifica [texts] y extrae la oferta.
     *
     * @param platform plataforma ya conocida por el pipeline (package name).
     * @return [ParsedOffer] con el tipo detectado y la oferta extraída, o
     *   `offer = null` si la pantalla no era una solicitud o no se pudo parsear.
     */
    fun parse(
        texts: List<String>,
        timestampMillis: Long,
        platform: RidePlatform,
    ): ParsedOffer {
        val parseStart = System.nanoTime()
        val detectionStart = System.nanoTime()
        val detection = detectionEngine.detect(texts)
        val detectionMillis = elapsedMillis(detectionStart)
        if (!detection.isRequest) {
            return ParsedOffer.none(detectionMillis = detectionMillis)
        }

        val normalized = texts.map(OfferDetectionEngine::normalize)
        if (platform == RidePlatform.UBER) {
            for (parser in specializedParsers) {
                if (parser.matches(normalized)) {
                    val offer = parser.extract(texts, timestampMillis)
                    if (offer != null) {
                        return ParsedOffer(
                            type = parser.type,
                            offer = offer,
                            detectionMillis = detectionMillis,
                            parsingMillis = elapsedMillis(parseStart),
                        )
                    }
                }
            }
        }
        val generic = registry.forPlatform(platform).extract(texts, timestampMillis)
        return ParsedOffer(
            type = OfferType.GENERIC,
            offer = generic,
            detectionMillis = detectionMillis,
            parsingMillis = elapsedMillis(parseStart),
        )
    }

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
