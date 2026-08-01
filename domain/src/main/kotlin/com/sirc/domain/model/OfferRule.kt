package com.sirc.domain.model

/**
 * Contrato de una regla de decisión independiente.
 *
 * Cada regla evalúa un único aspecto del viaje y devuelve un [RuleResult]
 * con veredicto PASS/WARNING/FAIL. Las reglas NO se conocen entre sí: el
 * [com.sirc.domain.engine.RuleEngine] las ejecuta todas y agrega el resultado.
 */
interface OfferRule {
    val name: String

    fun evaluate(context: RuleContext): RuleResult
}
