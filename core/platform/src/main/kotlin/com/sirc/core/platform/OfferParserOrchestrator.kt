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
 * concreta. Resuelve la plataforma por [RidePlatform] (flujo actual) o por
 * packageName vía [PlatformDetectionEngine]. Es el único punto de entrada que
 * usa el pipeline de captura.
 */
class OfferParserOrchestrator(
    private val platformRegistry: PlatformDescriptorRegistry,
) {
    private val detectionEngine = PlatformDetectionEngine(platformRegistry)

    /**
     * Clasifica [texts] y extrae la oferta (flujo por plataforma conocida).
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
        val descriptor = platformRegistry.descriptorFor(platform) ?: return ParsedOffer.none()
        val engine = platformRegistry.detectionEngineFor(platform) ?: return ParsedOffer.none()

        val detectionStart = System.nanoTime()
        val detection = engine.detect(texts)
        val detectionMillis = elapsedMillis(detectionStart)
        if (!detection.isRequest) {
            return ParsedOffer.none(detectionMillis = detectionMillis)
        }

        return parseWith(
            descriptor = descriptor,
            screenDetection = detection,
            texts = texts,
            timestampMillis = timestampMillis,
            parseStartNanos = parseStart,
            detectionMillis = detectionMillis,
        )
    }

    /**
     * Clasifica [texts] y extrae la oferta (flujo por packageName).
     *
     * Usa [PlatformDetectionEngine] para resolver la plataforma y la pantalla en
     * una sola pasada. Si la detección es ambigua o no encuentra plataforma,
     * devuelve `offer = null`.
     */
    fun parse(
        texts: List<String>,
        timestampMillis: Long,
        packageName: String,
    ): ParsedOffer {
        val parseStart = System.nanoTime()
        val result = detectionEngine.detect(texts, timestampMillis, packageName)
        if (!result.isRecognized || !result.screenDetection.isRequest) {
            return ParsedOffer.none()
        }
        val descriptor = result.descriptor ?: return ParsedOffer.none()
        return parseWith(
            descriptor = descriptor,
            screenDetection = result.screenDetection,
            texts = texts,
            timestampMillis = timestampMillis,
            parseStartNanos = parseStart,
            detectionMillis = 0.0,
        )
    }

    /**
     * Clasifica [texts] usando un [DetectionResult] ya resuelto.
     *
     * Útil para el pipeline unificado donde la detección ocurre antes.
     * No re-resuelve la plataforma ni ejecuta detección.
     *
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
        val parseStart = System.nanoTime()
        if (!result.isRecognized || !result.screenDetection.isRequest) {
            return ParsedOffer.none()
        }
        val descriptor = result.descriptor ?: return ParsedOffer.none()
        return parseWith(
            descriptor = descriptor,
            screenDetection = result.screenDetection,
            texts = texts,
            timestampMillis = timestampMillis,
            parseStartNanos = parseStart,
            detectionMillis = detectionMillis,
        )
    }

    private fun parseWith(
        descriptor: PlatformDescriptor,
        screenDetection: ScreenDetection,
        texts: List<String>,
        timestampMillis: Long,
        parseStartNanos: Long,
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
                        parsingMillis = elapsedMillis(parseStartNanos),
                    )
                }
            }
        }

        val generic = platformRegistry.extractorFor(descriptor.platform)?.extract(texts, timestampMillis)
        return ParsedOffer(
            type = OfferType.GENERIC,
            offer = generic,
            detectionMillis = detectionMillis,
            parsingMillis = elapsedMillis(parseStartNanos),
        )
    }

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
