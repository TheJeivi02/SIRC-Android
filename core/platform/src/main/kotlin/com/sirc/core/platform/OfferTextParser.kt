package com.sirc.core.platform

import javax.inject.Inject

/** Candidato a monto detectado en el texto visible. */
data class AmountCandidate(
    val value: Double,
    val currency: String?,
    val context: String,
)

/** Resultado del análisis heurístico del texto visible en pantalla. */
data class ParsedOfferText(
    val amounts: List<AmountCandidate>,
    val distancesKm: List<Double>,
    val durationsMin: List<Double>,
)

/**
 * Analizador de texto puro (sin Android) que extrae candidatos de monto,
 * distancia y duración a partir de los textos visibles.
 *
 * Heurístico por diseño: los textos de las apps de transporte varían. La
 * decisión final de cada campo la toman los [GenericPlatformExtractor] usando
 * las palabras clave propias de cada plataforma.
 */
class OfferTextParser @Inject constructor() {
    fun parse(texts: List<String>): ParsedOfferText {
        val amountCandidates = mutableListOf<AmountCandidate>()
        val distances = mutableListOf<Double>()
        val durations = mutableListOf<Double>()

        for (text in texts) {
            for (match in AMOUNT_RUN.findAll(text)) {
                val value = parseAmount(match.groupValues[3]) ?: continue
                if (value <= 0.0 || value > MAX_AMOUNT) continue
                val context = contextAround(text, match.range)
                if (looksLikeUnit(context)) continue
                val currency =
                    match.groupValues[4].ifBlank { match.groupValues[1] }
                        .ifBlank { currencyFromSymbol(match.groupValues[2]) }
                amountCandidates += AmountCandidate(value, currency, context)
            }
            for (match in DISTANCE_REGEX.findAll(text)) {
                val km = parseDouble(match.groupValues[1]) ?: continue
                if (km in MIN_DISTANCE_KM..MAX_DISTANCE_KM) distances += km
            }
            collectDurations(text, durations)
        }

        return ParsedOfferText(
            amounts = dedupeAmounts(amountCandidates),
            distancesKm = distances.distinct(),
            durationsMin = durations.distinct(),
        )
    }

    private fun collectDurations(
        text: String,
        out: MutableList<Double>,
    ) {
        for (match in HOURS_REGEX.findAll(text)) {
            val hours = match.groupValues[1].toIntOrNull() ?: continue
            val extraMinutes = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val minutes = hours * 60 + extraMinutes
            if (minutes in 1..MAX_DURATION_MIN) out += minutes.toDouble()
        }
        for (match in MINUTES_REGEX.findAll(text)) {
            val minutes = match.groupValues[1].toIntOrNull() ?: continue
            if (minutes in 1..MAX_DURATION_MIN) out += minutes.toDouble()
        }
    }

    private fun contextAround(
        text: String,
        range: IntRange,
    ): String {
        val start = (range.first - 20).coerceAtLeast(0)
        val end = (range.last + 30).coerceAtMost(text.length)
        return text.substring(start, end).trim()
    }

    /** Excluye montos que en realidad son distancias, tiempos, ratings, etc. */
    private fun looksLikeUnit(context: String): Boolean {
        val normalized = context.lowercase()
        return UNIT_WORDS.any { normalized.contains(it) }
    }

    private fun dedupeAmounts(amounts: List<AmountCandidate>): List<AmountCandidate> {
        val seen = mutableSetOf<Pair<Double, String?>>()
        return amounts.filter { seen.add(it.value to it.currency) }
    }

    private fun parseAmount(raw: String): Double? {
        val lastComma = raw.lastIndexOf(',')
        val lastDot = raw.lastIndexOf('.')
        val sep = maxOf(lastComma, lastDot)
        val decimals = raw.length - sep - 1
        val normalized =
            if (sep >= 0 && decimals in 1..2) {
                raw.substring(0, sep).replace(",", "").replace(".", "") + "." + raw.substring(sep + 1)
            } else {
                raw.replace(",", "").replace(".", "")
            }
        return normalized.toDoubleOrNull()
    }

    private fun parseDouble(raw: String): Double? = raw.replace(",", ".").toDoubleOrNull()

    companion object {
        private const val MAX_AMOUNT = 1_000_000.0
        private const val MIN_DISTANCE_KM = 0.3
        private const val MAX_DISTANCE_KM = 400.0
        private const val MAX_DURATION_MIN = 600

        val AMOUNT_RUN =
            Regex(
                """(?:(MXN|USD|EUR|BRL|COP|PEN|ARS|CLP)\s*)?(R\$\s*|\$\s*|S/\s*)?([0-9][0-9.,]*)""" +
                    """(?:\s*(MXN|USD|EUR|BRL|COP|PEN|ARS|CLP))?""",
                RegexOption.IGNORE_CASE,
            )
        val DISTANCE_REGEX =
            Regex(
                """(\d{1,3}(?:[.,]\d{1,2})?)\s*(km|kms|kilometro|kilometros|kilómetro|kilómetros)""",
                RegexOption.IGNORE_CASE,
            )
        val HOURS_REGEX =
            Regex(
                """(\d{1,2})\s*(?:h|hr|hrs|hora|horas)\s*(?:(\d{1,2})\s*(?:m|min|mins|minuto|minutos))?""",
                RegexOption.IGNORE_CASE,
            )
        val MINUTES_REGEX =
            Regex(
                """(\d{1,3})\s*(min|mins|minuto|minutos)""",
                RegexOption.IGNORE_CASE,
            )

        private val UNIT_WORDS =
            listOf(
                "km", "kilometr", "kilómetr",
                "min", "minuto", "hora", "horas", "hrs",
                "%", "calif", "estrella", "rating", "usuario",
            )

        private fun currencyFromSymbol(symbol: String?): String? =
            when (symbol?.trim()) {
                "R$" -> "BRL"
                "S/" -> "PEN"
                else -> null
            }
    }
}
