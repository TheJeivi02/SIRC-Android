package com.sirc.capture.flag

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sistema de Feature Flags: consulta y configuración en caliente de cada
 * característica. Preparado para deshabilitar piezas en producción.
 */
interface FeatureFlags {
    fun isEnabled(flag: FeatureFlag): Boolean

    fun setEnabled(
        flag: FeatureFlag,
        enabled: Boolean,
    )
}

/**
 * Implementación en memoria: todos los flags habilitados por defecto
 * (entorno de desarrollo) y configurables desde el panel de depuración.
 */
@Singleton
class InMemoryFeatureFlags @Inject constructor() : FeatureFlags {
    private val overrides = mutableMapOf<FeatureFlag, Boolean>()

    override fun isEnabled(flag: FeatureFlag): Boolean = overrides[flag] ?: true

    override fun setEnabled(
        flag: FeatureFlag,
        enabled: Boolean,
    ) {
        overrides[flag] = enabled
    }
}
