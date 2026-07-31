package com.sirc.domain.usecase

import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.repository.DriverConfigRepository
import javax.inject.Inject

class SaveDriverConfigUseCase @Inject constructor(
    private val repository: DriverConfigRepository,
) {
    suspend fun saveDriverCosts(costs: DriverCosts) = repository.save(costs)

    suspend fun saveDecisionThresholds(thresholds: DecisionThresholds) = repository.save(thresholds)
}
