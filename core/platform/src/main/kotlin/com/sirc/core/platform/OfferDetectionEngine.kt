package com.sirc.core.platform

/**
 * Motor de Detección de Pantalla (O1).
 *
 * Clasifica el texto visible (OCR o accesibilidad) en un [ScreenType] ANTES de
 * intentar parsear una oferta, usando palabras clave ponderadas por pantalla.
 *
 * Reglas de diseño:
 * - Solo [ScreenType.REQUEST] produce ofertas evaluables; las demás pantallas
 *   se descartan aguas abajo sin llamar al parser.
 * - Las palabras clave se comparan en minúsculas y sin acentos para tolerar
 *   variantes del OCR.
 * - La [confidence] normaliza la suma de pesos de las palabras detectadas.
 */
class OfferDetectionEngine(
    private val rules: List<DetectionRule> = defaultRules(),
) {
    /**
     * Clasifica [texts]. Si ninguna pantalla supera el umbral mínimo, devuelve
     * [ScreenType.UNKNOWN] (que el pipeline trata como "no hay oferta").
     */
    fun detect(texts: List<String>): ScreenDetection {
        if (texts.isEmpty()) return ScreenDetection(ScreenType.UNKNOWN)
        val normalized = texts.map(::normalize)
        val candidates = mutableListOf<ScreenDetection>()
        for (rule in rules) {
            val matched = rule.keywords.filter { kw -> normalized.any { it.contains(normalize(kw)) } }
            if (matched.isEmpty()) continue
            candidates +=
                ScreenDetection(
                    type = rule.type,
                    confidence = rule.confidence(matched.size),
                )
        }
        if (candidates.isEmpty()) return ScreenDetection(ScreenType.UNKNOWN)
        // La pantalla con mayor confianza gana; los empates se resuelven por
        // el orden de prioridad de las reglas (REQUEST se evalúa primero).
        return candidates.maxByOrNull { it.confidence } ?: ScreenDetection(ScreenType.UNKNOWN)
    }

    /** Palabras clave reconocibles por tipo de pantalla (para depuración). */
    fun keywordsFor(type: ScreenType): List<String> =
        rules.filter { it.type == type }.flatMap { it.keywords }.distinct()

    companion object {
        fun defaultRules(): List<DetectionRule> =
            listOf(
                DetectionRule(ScreenType.REQUEST, HIGH_WEIGHT, requestKeywords),
                DetectionRule(ScreenType.ERROR, HIGH_WEIGHT, errorKeywords),
                DetectionRule(ScreenType.OFFLINE, MEDIUM_WEIGHT, offlineKeywords),
                DetectionRule(ScreenType.NAVIGATION, MEDIUM_WEIGHT, navigationKeywords),
                DetectionRule(ScreenType.TRIP, MEDIUM_WEIGHT, tripKeywords),
                DetectionRule(ScreenType.HOME, LOW_WEIGHT, homeKeywords),
            )

        private const val HIGH_WEIGHT = 3.0f
        private const val MEDIUM_WEIGHT = 2.0f
        private const val LOW_WEIGHT = 1.0f

        private val requestKeywords =
            listOf(
                "nueva solicitud",
                "solicitud de viaje",
                "toca para aceptar",
                "aceptar",
                "rechazar",
                "aceptar y recoger",
                "recoge a",
                "ganancia estimada",
                "tarifa estimada",
                "precio estimado",
                "tu viaje",
                "pickup",
                "recogida",
                "llegar a la zona",
                "radar",
                "explorar",
                "uber moto",
                "uber xl",
                "reservado",
                "programado",
                "6 pasajeros",
            )

        private val errorKeywords =
            listOf(
                "algo salió mal",
                "error",
                "inténtalo de nuevo",
                "intenta de nuevo",
                "no pudimos",
                "hubo un problema",
                "ups",
                "algo salio mal",
            )

        private val offlineKeywords =
            listOf(
                "estás desconectado",
                "sin conexión",
                "ir en línea",
                "vuelve en línea",
                "conectarte",
                "no estás recibiendo",
                "toca para conectarte",
                "estas desconectado",
                "en linea",
            )

        private val navigationKeywords =
            listOf(
                "gira a la",
                "siga derecho",
                "sigue derecho",
                "doble a la",
                "llegada en",
                "destino en",
                "en 500",
                "navegación",
            )

        private val tripKeywords =
            listOf(
                "en curso",
                "viaje en curso",
                "recogida completada",
                "finalizar",
                "llegando a tu destino",
                "pasajero a bordo",
                "inicia el viaje",
                "comenzar",
                "término",
            )

        private val homeKeywords =
            listOf(
                "dónde quieres ir",
                "buscar",
                "disponible",
                "en línea",
                "inicio",
                "viajes",
                "promociones",
                "gana más",
                "mis ganancias",
                "configuración",
            )

        /** Normaliza texto: minúsculas, sin acentos y sin espacios repetidos. */
        internal fun normalize(text: String): String {
            val lower = text.lowercase()
            val builder = StringBuilder(lower.length)
            for (ch in lower) {
                builder.append(STRIP_ACCENTS[ch] ?: ch)
            }
            return builder.toString().trim()
        }

        private val STRIP_ACCENTS =
            mapOf(
                'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u',
                'Á' to 'a', 'É' to 'e', 'Í' to 'i', 'Ó' to 'o', 'Ú' to 'u',
                'ü' to 'u', 'Ü' to 'u', 'ñ' to 'n', 'Ñ' to 'n',
            )
    }
}

/**
 * Regla de detección: un conjunto de palabras clave que, si aparece en el
 * texto, apunta a un [type] de pantalla con el [weight] dado.
 */
data class DetectionRule(
    val type: ScreenType,
    val weight: Float,
    val keywords: List<String>,
) {
    fun confidence(matchedCount: Int): Float = (weight * matchedCount / MAX_NORMALIZER).coerceIn(0f, 1f)

    companion object {
        private const val MAX_NORMALIZER = 8.0f
    }
}
