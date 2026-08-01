package com.sirc.domain.engine.rules

import com.sirc.domain.model.RuleVerdict

/**
 * Construye mensajes legibles por regla según el [RuleVerdict].
 *
 * Centraliza el texto para que las reglas no repitan la misma lógica y el
 * mensaje sea consistente en el overlay y el panel de depuración.
 */
internal object RuleMessages {
    fun message(
        verdict: RuleVerdict,
        pass: String,
        fail: String,
    ): String =
        when (verdict) {
            RuleVerdict.PASS -> pass
            RuleVerdict.WARNING -> "Al límite: $fail"
            RuleVerdict.FAIL -> fail
        }

    fun format(value: Double?): String = value?.let { "${round2(it)}" } ?: "—"

    private fun round2(value: Double): Double = (value * 100).let { kotlin.math.round(it) } / 100.0
}
