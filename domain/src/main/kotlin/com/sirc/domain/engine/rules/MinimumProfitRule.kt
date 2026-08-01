package com.sirc.domain.engine.rules

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict
import kotlin.math.abs

/**
 * La ganancia neta estimada debe ser positiva (cubrir los costos).
 *
 * Regla base: un viaje que no cubre sus costos nunca conviene, aunque los
 * indicadores por kilómetro/hora parezcan buenos.
 */
class MinimumProfitRule : OfferRule {
    override val name = "Ganancia mínima"

    override fun evaluate(context: RuleContext): RuleResult {
        val limit = context.thresholds.minProfit
        val actual = context.metrics.estimatedProfit
        val verdict =
            when {
                actual < limit -> RuleVerdict.FAIL
                actual < WARNING_MARGIN * abs(limit).coerceAtLeast(MIN_LIMIT_ABS) -> RuleVerdict.WARNING
                else -> RuleVerdict.PASS
            }
        return RuleResult(
            ruleName = name,
            verdict = verdict,
            message =
                RuleMessages.message(
                    verdict,
                    pass = "Ganancia estimada ${RuleMessages.format(actual)}",
                    fail = "La ganancia estimada ${RuleMessages.format(actual)} no cubre los costos",
                ),
            actualValue = actual,
            limitValue = limit,
        )
    }

    companion object {
        private const val WARNING_MARGIN = 0.1
        private const val MIN_LIMIT_ABS = 1.0
    }
}
