package com.sirc.domain.usecase

import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDriverConfigUseCase @Inject constructor(
    private val repository: DriverConfigRepository,
) {
    suspend fun getDriverCosts(): DriverCosts = repository.getDriverCosts()

    suspend fun getDecisionThresholds(): DecisionThresholds = repository.getDecisionThresholds()

    fun observeDriverCosts(): Flow<DriverCosts> = repository.observeDriverCosts()

    fun observeDecisionThresholds(): Flow<DecisionThresholds> = repository.observeDecisionThresholds()
}
