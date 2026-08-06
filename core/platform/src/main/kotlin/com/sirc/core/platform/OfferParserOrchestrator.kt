package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Orquestador de Parsing (O2).
 *
 * Decide qué variante de oferta aplicar al texto visible: primero resuelve el
 * descriptor de la plataforma, detecta si realmente hay una pantalla de oferta
 * ([OfferDetectionEngine] con las reglas del descriptor) y, si es así, prueba
 * las variantes del descriptor en orden de especificidad. Si ninguna matchea o
 * extrae, cae al extractor genérico de la plataforma.
 *
 * Es 100 % descriptor-driven: no contiene ninguna referencia a una plataforma
 * concreta. Los parsers y el motor de detección los resuelve el
 * [PlatformDescriptorRegistry]. Es el único punto de entrada que usa el
 * pipeline de captura.
 */
class OfferParserOrchestrator(
    private val platformRegistry: PlatformDescriptorRegistry,
) {
    /**
     * Clasifica [texts] y extrae la oferta.
     *
     * @param platform plataforma ya conocida por el pipeline (package name).
     * @return [ParsedOffer] con el tipo detectado y la oferta extraída, o
     *   `offer = null` si la pantalla no era una solicitud, la plataforma no
     *   está registrada o no se pudo parsear.
     */
    fun parse(
        texts: List<String>,
        timestampMillis: Long,
        platform: RidePlatform,
    ): ParsedOffer {
        val parseStart = System.nanoTime()
        if (platformRegistry.descriptorFor(platform) == null) {
            return ParsedOffer.none()
        }

        val detectionStart = System.nanoTime()
        val detectionEngine = platformRegistry.detectionEngineFor(platform) ?: return ParsedOffer.none()
        val detection = detectionEngine.detect(texts)
        val detectionMillis = elapsedMillis(detectionStart)
        if (!detection.isRequest) {
            return ParsedOffer.none(detectionMillis = detectionMillis)
        }

        val normalized = texts.map(OfferDetectionEngine::normalize)
        for (parser in platformRegistry.variantParsersFor(platform)) {
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

        val generic = platformRegistry.extractorFor(platform)?.extract(texts, timestampMillis)
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
