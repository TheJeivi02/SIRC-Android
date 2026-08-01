package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict

/**
 * La ganancia por hora debe superar el mínimo configurado.
 */
class MinimumProfitPerHourRule : OfferRule {
    override val name = "Ganancia/hora mínima"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.minProfitPerHour
        val actual = context.metrics.profitPerHour
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
                    pass = "Ganancia/hora ${RuleMessages.format(actual)} (mín ${RuleMessages.format(limit)})",
                    fail = "Ganancia/hora ${RuleMessages.format(actual)} menor al mínimo ${RuleMessages.format(limit)}",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    companion object {
        private const val WARNING_MARGIN = 0.1
    }
}
