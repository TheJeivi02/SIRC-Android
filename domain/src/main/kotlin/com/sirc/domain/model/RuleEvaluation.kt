package com.sirc.domain.model

/**
 * Resultado agregado de evaluar todas las reglas sobre una oferta.
 *
 * Conserva los [results] individuales y expone atajos ([allPassed],
 * [failures], [warnings]) para que el overlay y el panel de depuración
 * muestren el desglose sin volver a evaluar.
 */
data class RuleEvaluation(
    val results: List<RuleResult>,
) {
    val allPassed: Boolean
        get() = results.all { it.verdict == RuleVerdict.PASS }

    val failures: List<RuleResult>
        get() = results.filter { it.verdict == RuleVerdict.FAIL }

    val warnings: List<RuleResult>
        get() = results.filter { it.verdict == RuleVerdict.WARNING }

    val hasFailures: Boolean
        get() = failures.isNotEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()
}
