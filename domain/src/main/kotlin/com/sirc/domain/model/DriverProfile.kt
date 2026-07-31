package com.sirc.domain.model

/**
 * Perfil básico del conductor.
 *
 * @param name nombre (opcional, solo informativo).
 * @param country país donde opera.
 * @param city ciudad principal de operación.
 * @param currency código de moneda (ISO 4217) usada en costos y ganancias.
 */
data class DriverProfile(
    val name: String?,
    val country: String,
    val city: String,
    val currency: String,
)
