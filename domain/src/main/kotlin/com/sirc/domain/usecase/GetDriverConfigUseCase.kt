package com.sirc.domain.usecase

import com.sirc.domain.model.DriverConfig
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDriverConfigUseCase @Inject constructor(
    private val repository: DriverConfigRepository,
) {
    fun observeDriverConfig(): Flow<DriverConfig?> = repository.observeDriverConfig()

    fun observeIsConfigured(): Flow<Boolean> = repository.isConfigured()
}
