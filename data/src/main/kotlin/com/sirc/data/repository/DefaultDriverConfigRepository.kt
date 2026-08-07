package com.sirc.data.repository

import com.sirc.data.local.dao.DriverConfigDao
import com.sirc.data.local.mapper.toDriverConfig
import com.sirc.data.local.mapper.toEntity
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultDriverConfigRepository @Inject constructor(
    private val dao: DriverConfigDao,
) : DriverConfigRepository {
    override suspend fun getDriverConfig(): DriverConfig? = dao.getConfig()?.toDriverConfig()

    override fun observeDriverConfig(): Flow<DriverConfig?> = dao.observeConfig().map { it?.toDriverConfig() }

    override fun isConfigured(): Flow<Boolean> = dao.observeConfig().map { it != null }

    override suspend fun save(driverConfig: DriverConfig) {
        dao.upsert(driverConfig.toEntity())
    }
}
