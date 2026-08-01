package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict

/**
 * La ganancia por kilómetro debe superar el mínimo configurado.
 */
class MinimumProfitPerKmRule : OfferRule {
    override val name = "Ganancia/km mínima"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.minProfitPerKm
        val actual = context.metrics.profitPerKm
        val verdict =
            when {
                actual < limit -> RuleVerdict.FAIL
                actual < limit * (1.0 + WARNING_MARGIN) -> RuleVerdict.WARNING
                else -> RuleVerdict.PASS
            }
        return RuleResult(
            ruleName = name,
            verdict = verdict,
            message =
                RuleMessages.message(
                    verdict,
                    pass = "Ganancia/km ${RuleMessages.format(actual)} (mín ${RuleMessages.format(limit)})",
                    fail = "Ganancia/km ${RuleMessages.format(actual)} menor al mínimo ${RuleMessages.format(limit)}",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    companion object {
        private const val WARNING_MARGIN = 0.1
    }
}
