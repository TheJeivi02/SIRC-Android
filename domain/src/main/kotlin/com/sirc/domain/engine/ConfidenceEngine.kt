package com.sirc.domain.engine

import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.RuleEvaluation
import com.sirc.domain.model.TripOffer
import javax.inject.Inject

/**
 * Niveles de confianza derivados de la calidad de los datos de la oferta.
 */
enum class ConfidenceLevel {
    /** Datos suficientes y coherentes: la señal es clara. */
    HIGH,

    /** Datos completos pero con señales en conflicto o al límite. */
    MEDIUM,

    /** Datos insuficientes o contradictorios: no se debe decidir con esto. */
    LOW,
}

/**
 * Resultado de la evaluación de confianza de una oferta.
 *
 * [percent] siempre está entre 0 y 100; [isActionable] indica si la señal es
 * lo bastante sólida como para recomendar aceptar/rechazar (LOW = no).
 */
data class ConfidenceResult(
    val level: ConfidenceLevel,
    val percent: Int,
    val reasons: List<String>,
) {
    val isActionable: Boolean
        get() = level != ConfidenceLevel.LOW
}

/**
 * Motor de Confianza (O3): cuantifica qué tan fiable es la oferta detectada
 * antes de recomendar aceptar o rechazar.
 *
 * Combina la completitud de los datos ([TripOffer.hasEnoughData]) con la
 * coherencia de las métricas (precio/km y precio/hora razonables) y la
 * coherencia de las reglas ([RuleEvaluation] con fallos o advertencias).
 *
 * Cuando el nivel es LOW se debe mostrar "Información insuficiente" en lugar
 * de una recomendación de aceptar/rechazar.
 */
class ConfidenceEngine @Inject constructor() {
    /**
     * @param offer oferta detectada en pantalla.
     * @param metrics métricas derivadas por el [ProfitEngine].
     * @param ruleEvaluation resultado de las reglas (opcional, usado solo para
     *   afinar la confianza).
     */
    fun assess(
        offer: TripOffer,
        metrics: ProfitMetrics,
        ruleEvaluation: RuleEvaluation? = null,
    ): ConfidenceResult {
        val reasons = mutableListOf<String>()

        if (!offer.hasEnoughData) {
            reasons += "Faltan datos del viaje (monto, distancia o duración)"
        }
        if (!metricsAreCoherent(metrics)) {
            reasons += "Métricas incoherentes (precio por km o por hora fuera de rango)"
        }
        if (offer.currency == null) {
            reasons += "Moneda no detectada"
        }

        var percent = BASE_PERCENT
        var level = ConfidenceLevel.HIGH

        if (!offer.hasEnoughData) {
            percent -= MISSING_DATA_PENALTY
            level = ConfidenceLevel.LOW
        }
        if (!metricsAreCoherent(metrics)) {
            percent -= INCOHERENT_PENALTY
            level = ConfidenceLevel.LOW
        }
        if (offer.currency == null) {
            percent -= MISSING_CURRENCY_PENALTY
        }

        if (ruleEvaluation != null) {
            when {
                ruleEvaluation.hasFailures -> {
                    percent -= RULE_FAIL_PENALTY
                    level = ConfidenceLevel.MEDIUM
                }

                ruleEvaluation.hasWarnings -> {
                    percent -= RULE_WARNING_PENALTY
                    if (level == ConfidenceLevel.HIGH) {
                        level = ConfidenceLevel.MEDIUM
                    }
                }

                else -> percent += RULE_CLEAN_BONUS
            }
        }

        return ConfidenceResult(
            level = level,
            percent = percent.coerceIn(0, 100),
            reasons = reasons,
        )
    }

    /**
     * Un precio por km o por hora fuera de rango delata un parseo erróneo
     * (decimales corridos, unidades equivocadas, etc.). Las métricas null
     * (datos faltantes) no delatan un parseo erróneo y se tratan como sanas.
     */
    private fun metricsAreCoherent(metrics: ProfitMetrics): Boolean {
        val perKm = metrics.profitPerKm ?: 0.0
        val perHour = metrics.profitPerHour ?: 0.0
        val sanePerKm = perKm <= MAX_REASONABLE_PROFIT_PER_KM
        val sanePerHour = perHour <= MAX_REASONABLE_PROFIT_PER_HOUR
        return perKm.isFinite() && perHour.isFinite() && sanePerKm && sanePerHour
    }

    companion object {
        private const val BASE_PERCENT = 80
        private const val MISSING_DATA_PENALTY = 40
        private const val INCOHERENT_PENALTY = 35
        private const val MISSING_CURRENCY_PENALTY = 5
        private const val RULE_FAIL_PENALTY = 25
        private const val RULE_WARNING_PENALTY = 15
        private const val RULE_CLEAN_BONUS = 10

        /** Más allá de esto casi seguro es un parseo erróneo del monto. */
        const val MAX_REASONABLE_PROFIT_PER_KM = 500.0
        const val MAX_REASONABLE_PROFIT_PER_HOUR = 5000.0
    }
}
