package com.sirc.domain.model

/**
 * Contrato de una regla de decisión independiente.
 *
 * Cada regla evalúa un único aspecto del viaje y devuelve un [RuleResult]
 * con veredicto PASS/WARNING/FAIL. Las reglas NO se conocen entre sí: el
 * [com.sirc.domain.engine.RuleEngine] (ahora LEGACY, WP-E1-02) las ejecuta
 * todas y agrega el resultado.
 *
 * ⚠️ **LEGACY** — Fuera de la ruta de producción desde WP-E1-02.
 * `ProfitEngine` es el único motor de decisión en producción.
 */
interface OfferRule {
    val name: String

    fun evaluate(context: RuleContext): RuleResult
}
