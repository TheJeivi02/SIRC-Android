package com.sirc.domain.repository

import com.sirc.domain.model.OverlayConfig
import kotlinx.coroutines.flow.Flow

interface OverlayConfigRepository {
    suspend fun getOverlayConfig(): OverlayConfig

    suspend fun save(overlayConfig: OverlayConfig)

    fun observeOverlayConfig(): Flow<OverlayConfig>
}
