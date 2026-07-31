package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import javax.inject.Inject

/**
 * Extractores por plataforma.
 *
 * El MVP usa un extractor genérico parametrizado con palabras clave por
 * plataforma. Agregar una plataforma nueva = definir su descriptor de palabras
 * clave (sin tocar el núcleo del producto).
 */
data class PlatformKeywords(
    val totalKeywords: List<String>,
    val fareKeywords: List<String>,
)

object PlatformDescriptors {
    val UBER =
        PlatformKeywords(
            totalKeywords = listOf("total", "recibe", "neto", "cobro", "ingreso", "pago"),
            fareKeywords = listOf("tarifa", "fare", "precio"),
        )
    val DIDI =
        PlatformKeywords(
            totalKeywords = listOf("total", "monto", "ingreso", "ganancia"),
            fareKeywords = listOf("tarifa", "precio"),
        )
    val CABIFY =
        PlatformKeywords(
            totalKeywords = listOf("total", "recibe", "neto"),
            fareKeywords = listOf("tarifa", "precio", "fare"),
        )
    val INDRIVE =
        PlatformKeywords(
            totalKeywords = listOf("oferta", "precio", "total", "monto"),
            fareKeywords = listOf("pago", "tarifa"),
        )
}

class GenericPlatformExtractor(
    override val platform: RidePlatform,
    private val keywords: PlatformKeywords,
    private val parser: OfferTextParser = OfferTextParser(),
) : PlatformExtractor {
    override fun extract(
        texts: List<String>,
        timestampMillis: Long,
    ): TripOffer? {
        val cleaned = sanitize(texts)
        val parsed = parser.parse(cleaned)
        val total = chooseAmount(parsed, keywords) ?: return null
        if (parsed.distancesKm.isEmpty() && parsed.durationsMin.isEmpty()) return null

        return TripOffer(
            platform = platform,
            timestampMillis = timestampMillis,
            estimatedTotal = total.value,
            distanceKm = parsed.distancesKm.maxOrNull(),
            durationMin = parsed.durationsMin.maxOrNull(),
            currency = total.currency ?: DEFAULT_CURRENCY[platform],
            rawText = cleaned.take(MAX_RAW_TEXTS),
        )
    }

    private fun sanitize(texts: List<String>): List<String> =
        texts.map { it.trim() }
            .filter { it.isNotBlank() && it.length <= MAX_TEXT_LENGTH }
            .distinct()

    /** Elige el monto que representa el total a cobrar, priorizando contexto por palabras clave. */
    private fun chooseAmount(
        parsed: ParsedOfferText,
        keywords: PlatformKeywords,
    ): AmountCandidate? {
        if (parsed.amounts.isEmpty()) return null

        fun score(amount: AmountCandidate): Int {
            val ctx = amount.context.lowercase()
            var s = 0
            if (amount.currency != null) s += 2
            if (keywords.totalKeywords.any { ctx.contains(it) }) s += 4
            if (keywords.fareKeywords.any { ctx.contains(it) }) s += 1
            return s
        }

        // Si nada indica un monto, preferimos el de mayor valor (el total suele ser el mayor).
        val scored = parsed.amounts.filter { score(it) > 0 }
        return if (scored.isNotEmpty()) {
            scored.maxByOrNull { score(it) * 1000 + it.value.toInt() }
        } else {
            parsed.amounts.maxByOrNull { it.value }
        }
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 300
        private const val MAX_RAW_TEXTS = 60

        private val DEFAULT_CURRENCY =
            mapOf(
                RidePlatform.UBER to "MXN",
                RidePlatform.DIDI to "MXN",
                RidePlatform.CABIFY to "EUR",
                RidePlatform.INDRIVE to "MXN",
            )
    }
}

class ExtractorRegistry @Inject constructor(private val parser: OfferTextParser) {
    private val extractors: Map<RidePlatform, PlatformExtractor> =
        mapOf(
            RidePlatform.UBER to GenericPlatformExtractor(RidePlatform.UBER, PlatformDescriptors.UBER, parser),
            RidePlatform.DIDI to GenericPlatformExtractor(RidePlatform.DIDI, PlatformDescriptors.DIDI, parser),
            RidePlatform.CABIFY to GenericPlatformExtractor(RidePlatform.CABIFY, PlatformDescriptors.CABIFY, parser),
            RidePlatform.INDRIVE to GenericPlatformExtractor(RidePlatform.INDRIVE, PlatformDescriptors.INDRIVE, parser),
        )

    fun forPlatform(platform: RidePlatform): PlatformExtractor = extractors.getValue(platform)
}
