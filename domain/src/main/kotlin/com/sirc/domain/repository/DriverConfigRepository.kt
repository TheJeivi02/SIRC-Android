package com.sirc.domain.repository

import com.sirc.domain.model.DriverConfig
import kotlinx.coroutines.flow.Flow

interface DriverConfigRepository {
    /** Devuelve la configuración completa, o null si el conductor aún no la ha completado. */
    suspend fun getDriverConfig(): DriverConfig?

    /** Observa la configuración completa; null cuando no existe. */
    fun observeDriverConfig(): Flow<DriverConfig?>

    /** Indica si el conductor ya completó la configuración inicial. */
    fun isConfigured(): Flow<Boolean>

    /** Persiste la configuración completa del conductor. */
    suspend fun save(driverConfig: DriverConfig)
}
