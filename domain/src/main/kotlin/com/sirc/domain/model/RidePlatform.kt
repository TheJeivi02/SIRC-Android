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
    ;

    companion object {
        /**
         * Resuelve la plataforma por paquete (coincidencia exacta).
         *
         * DEPRECADO desde WP-E3-05A: la resolución de plataforma se unificó en
         * `PlatformDetectionEngine` (descriptor-driven, con normalización), única
         * fuente de verdad del pipeline. Este mapeo duplica esa lógica.
         */
        @Deprecated("Usar PlatformDetectionEngine para resolver la plataforma por packageName")
        fun fromPackageName(packageName: String): RidePlatform? = entries.firstOrNull { it.packageName == packageName }
    }
}
