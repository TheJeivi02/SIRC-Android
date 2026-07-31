package com.sirc.data.repository

import com.sirc.data.local.dao.OverlayConfigDao
import com.sirc.data.local.mapper.toDomain
import com.sirc.data.local.mapper.toEntity
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.repository.OverlayConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultOverlayConfigRepository @Inject constructor(
    private val dao: OverlayConfigDao,
) : OverlayConfigRepository {
    override suspend fun getOverlayConfig(): OverlayConfig = dao.getConfig()?.toDomain() ?: OverlayConfig()

    override suspend fun save(overlayConfig: OverlayConfig) {
        dao.upsert(overlayConfig.toEntity())
    }

    override fun observeOverlayConfig(): Flow<OverlayConfig> =
        dao.observeConfig().map { it?.toDomain() ?: OverlayConfig() }
}
