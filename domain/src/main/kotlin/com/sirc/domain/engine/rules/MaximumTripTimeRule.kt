package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict

/**
 * La duración estimada del viaje no debe superar el máximo configurado.
 *
 * Si el viaje no reporta duración ([TripOffer.durationMin] nulo) se devuelve
 * PASS sin penalizar al conductor.
 */
class MaximumTripTimeRule : OfferRule {
    override val name = "Tiempo de viaje máximo"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.maxTripTimeMin
        val actual = context.offer.durationMin ?: return notEvaluable(limit)
        val verdict =
            when {
                actual > limit -> RuleVerdict.FAIL
                actual > limit * (1.0 - WARNING_MARGIN) -> RuleVerdict.WARNING
                else -> RuleVerdict.PASS
            }
        return RuleResult(
            ruleName = name,
            verdict = verdict,
            message =
                RuleMessages.message(
                    verdict,
                    pass = "Duración ${actual.toInt()} min (máx ${limit.toInt()} min)",
                    fail = "Duración ${actual.toInt()} min supera el máximo ${limit.toInt()} min",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    private fun notEvaluable(limit: Double): RuleResult =
        RuleResult(
            ruleName = name,
            verdict = RuleVerdict.PASS,
            message = "Duración no disponible",
            limitValue = limit,
        )

    companion object {
        private const val WARNING_MARGIN = 0.1
    }
}
