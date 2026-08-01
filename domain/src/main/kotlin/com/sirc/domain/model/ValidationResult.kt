package com.sirc.domain.model

/**
 * Problema detectado por la validación cruzada de la oferta.
 */
enum class ValidationIssue {
    /** Falta o es inválido el monto total estimado. */
    INVALID_TOTAL,

    /** Falta o es inválida la distancia del viaje. */
    INVALID_DISTANCE,

    /** Falta o es inválida la duración estimada. */
    INVALID_DURATION,

    /** Precio por kilómetro fuera de un rango razonable. */
    UNREASONABLE_PRICE_PER_KM,

    /** Precio por hora fuera de un rango razonable. */
    UNREASONABLE_PRICE_PER_HOUR,

    /** Monto total negativo (imposible en una oferta válida). */
    NEGATIVE_TOTAL,

    /** Distancia de recogida mayor que la distancia total del viaje. */
    PICKUP_FARTHER_THAN_TRIP,
}

/** Resultado de la validación cruzada: lista de problemas o `null` si pasó. */
data class ValidationResult(
    val issues: List<ValidationIssue>,
) {
    val isValid: Boolean
        get() = issues.isEmpty()

    companion object {
        val valid: ValidationResult = ValidationResult(emptyList())
    }
}
