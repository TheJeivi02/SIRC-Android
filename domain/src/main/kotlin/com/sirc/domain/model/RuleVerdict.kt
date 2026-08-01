package com.sirc.domain.model

/**
 * Resultado de una regla de decisión.
 *
 * - [PASS]: el viaje cumple la regla.
 * - [WARNING]: el viaje está al límite de la regla (revisar).
 * - [FAIL]: el viaje incumple la regla (no conviene).
 */
enum class RuleVerdict {
    PASS,
    WARNING,
    FAIL,
}
