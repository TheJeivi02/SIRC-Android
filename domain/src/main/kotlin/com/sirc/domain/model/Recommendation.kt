package com.sirc.domain.model

/**
 * Recomendación accionable para el conductor, desacoplada de la UI.
 *
 * - [ACCEPT]: la oferta conviene (aceptar).
 * - [REJECT]: la oferta no conviene (rechazar).
 * - [WARNING]: la oferta es dudosa (revisar antes de decidir).
 */
enum class Recommendation {
    ACCEPT,
    REJECT,
    WARNING,
}
