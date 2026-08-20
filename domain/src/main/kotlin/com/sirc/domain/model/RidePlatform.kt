package com.sirc.domain.model

/**
 * Plataformas de movilidad soportadas.
 *
 * SIRC está diseñado como una plataforma multi-servicio: la lógica de negocio
 * no depende de ninguna plataforma específica.
 */
enum class RidePlatform(
    val packageName: String,
    val displayName: String,
) {
    UBER("com.ubercab", "Uber"),
    DIDI("com.didiglobal.passenger", "DiDi"),
    CABIFY("com.cabify.rider", "Cabify"),
    INDRIVE("com.leadingsoft.ride.driver", "InDrive"),
}
