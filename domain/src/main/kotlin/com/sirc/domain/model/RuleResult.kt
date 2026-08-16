package com.sirc.domain.model

/**
 * Resultado de evaluar una regla sobre una oferta.
 *
 * Conserva el [verdict] (PASS/WARNING/FAIL), un [message] legible para el
 * conductor y los valores comparados ([actualValue] vs [limitValue]) para el
 * panel de depuración.
 */
data class RuleResult(
    val ruleName: String,
    val verdict: RuleVerdict,
    val message: String,
    val actualValue: Double? = null,
    val limitValue: Double? = null,
) {
    val passed: Boolean
        get() = verdict == RuleVerdict.PASS
}
