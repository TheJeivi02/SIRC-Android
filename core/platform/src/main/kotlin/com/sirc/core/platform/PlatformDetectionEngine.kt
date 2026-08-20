package com.sirc.core.platform

/**
 * Motor genérico de detección de plataforma (WP-E3-02).
 *
 * Recorre los descriptores del registry (O(n)) en una sola pasada y resuelve
 * la plataforma por paquete o por keywords. No conoce el origen de los textos:
 * su contrato se limita a [texts] y [packageName] opcional. No contiene lógica
 * de parsing.
 */
class PlatformDetectionEngine(
    private val registry: PlatformDescriptorRegistry,
) {
    fun detect(
        texts: List<String>,
        packageName: String? = null,
        origin: CaptureInputType = CaptureInputType.UNKNOWN,
    ): DetectionResult {
        val normalized = texts.map(OfferDetectionEngine::normalize)

        // Etapa 1: paquete.
        if (packageName != null) {
            for (descriptor in registry.descriptors) {
                if (DetectionMatcher.matchesPackage(descriptor.packageNames, packageName)) {
                    val engine = registry.detectionEngineFor(descriptor.platform) ?: continue
                    return DetectionResult(
                        resolution = DetectionResolution.PACKAGE_MATCH,
                        origin = origin,
                        descriptor = descriptor,
                        screenDetection = engine.detect(texts),
                        sourcePackage = packageName,
                    )
                }
            }
        }

        // Etapa 2: keywords. Candidato válido = pantalla distinta de UNKNOWN.
        val candidates = mutableListOf<DetectionCandidate>()
        for (descriptor in registry.descriptors) {
            val engine = registry.detectionEngineFor(descriptor.platform) ?: continue
            val detection = engine.detect(texts)
            if (detection.type != ScreenType.UNKNOWN) {
                candidates +=
                    DetectionCandidate(
                        descriptor = descriptor,
                        screenDetection = detection,
                        matchScore = DetectionMatcher.matchScore(descriptor, normalized),
                    )
            }
        }

        if (candidates.isEmpty()) {
            return DetectionResult(resolution = DetectionResolution.NONE, origin = origin)
        }

        // Etapas 3-4: único candidato con evidencia fuerte o empate.
        val topScore = candidates.maxOf { it.matchScore }
        val winners = candidates.filter { it.matchScore == topScore }
        return if (winners.size == 1 && topScore > 0) {
            DetectionResult(
                resolution = DetectionResolution.KEYWORD_CANDIDATE,
                origin = origin,
                descriptor = winners.single().descriptor,
                screenDetection = winners.single().screenDetection,
                candidates = candidates,
            )
        } else {
            DetectionResult(
                resolution = DetectionResolution.AMBIGUOUS,
                origin = origin,
                candidates = candidates,
            )
        }
    }
}
