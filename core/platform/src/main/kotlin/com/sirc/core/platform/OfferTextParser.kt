package com.sirc.core.platform

import javax.inject.Inject

/** Candidato a monto detectado en el texto visible. */
data class AmountCandidate(
    val value: Double,
    val currency: String?,
    val context: String,
    /** true si el candidato trae un marcador de moneda (símbolo o código). */
    val hasCurrencyMarker: Boolean = false,
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
            val normalizedText = normalizeSpaces(text)
            for (match in AMOUNT_RUN.findAll(normalizedText)) {
                val value = parseAmount(match.groupValues[3]) ?: continue
                if (value <= 0.0 || value > MAX_AMOUNT) continue
                if (hasLeadingZero(match.groupValues[3])) continue
                val context = contextAround(normalizedText, match.range)
                if (looksLikeUnit(context)) continue
                val hasCurrencyMarker =
                    match.groupValues[1].isNotBlank() ||
                        match.groupValues[2].isNotBlank() ||
                        match.groupValues[4].isNotBlank()
                // Sin marcador de moneda (símbolo o código) no es un monto
                // fiable: ignora direcciones, ratings, conteos y ruido del OCR.
                if (!hasCurrencyMarker) continue
                val currency =
                    match.groupValues[4].ifBlank { match.groupValues[1] }
                        .ifBlank { currencyFromSymbol(match.groupValues[2]) }
                amountCandidates += AmountCandidate(value, currency, context, hasCurrencyMarker)
            }
            for (match in DISTANCE_REGEX.findAll(normalizedText)) {
                val km = parseDouble(match.groupValues[1]) ?: continue
                if (km in MIN_DISTANCE_KM..MAX_DISTANCE_KM) distances += km
            }
            collectDurations(normalizedText, durations)
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

    /** Rechaza números con cero a la izquierda ("090", "012") que el OCR da
     *  como ruido (p. ej. "$090 incluido"); permite decimales como "0.50". */
    private fun hasLeadingZero(raw: String): Boolean = raw.length > 1 && raw[0] == '0' && raw[1].isDigit()

    private fun dedupeAmounts(amounts: List<AmountCandidate>): List<AmountCandidate> =
        amounts.groupBy { it.value to it.currency }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.context.length } }

    private fun parseAmount(rawInput: String): Double? {
        // El regex puede dejar un separador colgando ("4.50," o "1.234,");
        // recortar evita que se mezcle con el exponente al normalizar.
        val raw = rawInput.trimEnd('.', ',')
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

    /**
     * Normaliza separadores de espacio Unicode (NBSP U+00A0, U+202F, U+2000-200A,
     * etc.) a espacio ASCII para que los regex `\s` los reconozcan. Solo actúa
     * sobre separadores de espacio; no altera texto ni signos (FASE 10-D).
     */
    private fun normalizeSpaces(text: String): String {
        if (text.length <= 0) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(if (Character.getType(ch) == Character.SPACE_SEPARATOR.toInt()) ' ' else ch)
        }
        return sb.toString()
    }

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
