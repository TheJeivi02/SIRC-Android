package com.sirc.domain.usecase

import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.repository.OverlayConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOverlayConfigUseCase @Inject constructor(
    private val repository: OverlayConfigRepository,
) {
    suspend fun getConfig(): OverlayConfig = repository.getOverlayConfig()

    fun observeConfig(): Flow<OverlayConfig> = repository.observeOverlayConfig()
}
