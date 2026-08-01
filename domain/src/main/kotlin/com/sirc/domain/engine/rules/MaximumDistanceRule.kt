package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict

/**
 * La distancia total del viaje no debe superar el máximo configurado.
 *
 * Si el viaje no reporta distancia ([TripOffer.distanceKm] nulo) no se puede
 * evaluar y se devuelve PASS sin penalizar al conductor.
 */
class MaximumDistanceRule : OfferRule {
    override val name = "Distancia máxima"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.maxDistanceKm
        val actual = context.offer.distanceKm ?: return notEvaluable(limit)
        val verdict =
            when {
                actual > limit -> RuleVerdict.FAIL
                actual > limit * (1.0 - WARNING_MARGIN) -> RuleVerdict.WARNING
                else -> RuleVerdict.PASS
            }
        val actualStr = RuleMessages.format(actual)
        val limitStr = RuleMessages.format(limit)
        return RuleResult(
            ruleName = name,
            verdict = verdict,
            message =
                RuleMessages.message(
                    verdict,
                    pass = "Distancia $actualStr km (máx $limitStr km)",
                    fail = "Distancia $actualStr km supera el máximo $limitStr km",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    private fun notEvaluable(limit: Double): RuleResult =
        RuleResult(
            ruleName = name,
            verdict = RuleVerdict.PASS,
            message = "Distancia no disponible",
            limitValue = limit,
        )

    companion object {
        private const val WARNING_MARGIN = 0.1
    }
}
