package com.sirc.domain.usecase

import com.sirc.domain.model.DriverConfig
import com.sirc.domain.repository.DriverConfigRepository
import javax.inject.Inject

class SaveDriverConfigUseCase @Inject constructor(
    private val repository: DriverConfigRepository,
) {
    suspend fun save(config: DriverConfig) = repository.save(config)
}
