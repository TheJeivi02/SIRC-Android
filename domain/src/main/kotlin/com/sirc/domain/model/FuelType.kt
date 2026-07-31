package com.sirc.domain.model

/** Tipos de combustible/energía soportados por el perfil del vehículo. */
enum class FuelType(
    val displayName: String,
) {
    GASOLINE("Gasolina"),
    DIESEL("Diésel"),
    HYBRID("Híbrido"),
    ELECTRIC("Eléctrico"),
    LPG("GLP"),
    OTHER("Otro"),
}
