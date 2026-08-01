package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict

/**
 * La distancia hasta el punto de recogida no debe superar el máximo configurado.
 *
 * Depende de [TripOffer.pickupDistanceKm]; si el dato no está disponible la
 * regla se considera cumplida (PASS) para no bloquear viajes sin ese dato.
 */
class MaximumPickupRule : OfferRule {
    override val name = "Recogida máxima"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.maxPickupKm
        val actual = context.offer.pickupDistanceKm ?: return notEvaluable(limit)
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
                    pass = "Recogida a $actualStr km (máx $limitStr km)",
                    fail = "Recogida a $actualStr km supera el máximo $limitStr km",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    private fun notEvaluable(limit: Double): RuleResult =
        RuleResult(
            ruleName = name,
            verdict = RuleVerdict.PASS,
            message = "Distancia de recogida no disponible",
            limitValue = limit,
        )

    companion object {
        private const val WARNING_MARGIN = 0.1
    }
}
