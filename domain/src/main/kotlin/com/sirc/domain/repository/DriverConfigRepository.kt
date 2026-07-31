package com.sirc.domain.repository

import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import kotlinx.coroutines.flow.Flow

interface DriverConfigRepository {
    suspend fun getDriverCosts(): DriverCosts

    suspend fun getDecisionThresholds(): DecisionThresholds

    suspend fun save(driverCosts: DriverCosts)

    suspend fun save(decisionThresholds: DecisionThresholds)

    fun observeDriverCosts(): Flow<DriverCosts>

    fun observeDecisionThresholds(): Flow<DecisionThresholds>
}
