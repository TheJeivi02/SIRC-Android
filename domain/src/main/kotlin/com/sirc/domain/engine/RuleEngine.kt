package com.sirc.domain.engine

import com.sirc.domain.engine.rules.MaximumDistanceRule
import com.sirc.domain.engine.rules.MaximumPickupRule
import com.sirc.domain.engine.rules.MaximumTripTimeRule
import com.sirc.domain.engine.rules.MinimumProfitPerHourRule
import com.sirc.domain.engine.rules.MinimumProfitPerKmRule
import com.sirc.domain.engine.rules.MinimumProfitRule
import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleEvaluation

/**
 * Motor de Reglas: ejecuta todas las [OfferRule] sobre un [RuleContext] y
 * agrega el resultado en una [RuleEvaluation].
 *
 * ⚠️ **LEGACY** — Fuera de la ruta de producción desde WP-E1-02.
 *
 * A partir de WP-E1-02, `ProfitEngine` es el único motor de decisión en
 * producción. `RuleEngine` se conserva en `:domain` exclusivamente para
 * uso en tests (ver `RuleEngineTest.kt`) y como base para futuros motores
 * de validación. No debe inyectarse ni invocar desde código de producción.
 */
class RuleEngine(
    private val rules: List<OfferRule> = defaultRules(),
) {
    fun evaluate(context: RuleContext): RuleEvaluation = RuleEvaluation(results = rules.map { it.evaluate(context) })

    companion object {
        fun defaultRules(): List<OfferRule> =
            listOf(
                MinimumProfitRule(),
                MinimumProfitPerKmRule(),
                MinimumProfitPerHourRule(),
                MaximumDistanceRule(),
                MaximumPickupRule(),
                MaximumTripTimeRule(),
            )
    }
}
