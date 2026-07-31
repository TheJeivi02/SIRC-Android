package com.sirc.domain.usecase

import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.repository.OverlayConfigRepository
import javax.inject.Inject

class SaveOverlayConfigUseCase @Inject constructor(
    private val repository: OverlayConfigRepository,
) {
    suspend operator fun invoke(config: OverlayConfig) = repository.save(config)
}
