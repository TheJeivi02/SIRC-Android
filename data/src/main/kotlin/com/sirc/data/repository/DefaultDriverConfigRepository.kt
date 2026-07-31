package com.sirc.data.repository

import com.sirc.data.local.dao.DriverConfigDao
import com.sirc.data.local.mapper.toDecisionThresholds
import com.sirc.data.local.mapper.toDriverCosts
import com.sirc.data.local.mapper.toEntity
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultDriverConfigRepository @Inject constructor(
    private val dao: DriverConfigDao,
) : DriverConfigRepository {
    override suspend fun getDriverCosts(): DriverCosts = dao.getConfig()?.toDriverCosts() ?: DEFAULT_COSTS

    override suspend fun getDecisionThresholds(): DecisionThresholds =
        dao.getConfig()?.toDecisionThresholds() ?: DEFAULT_THRESHOLDS

    override suspend fun save(driverCosts: DriverCosts) {
        val thresholds = getDecisionThresholds()
        dao.upsert(driverCosts.toEntity(thresholds))
    }

    override suspend fun save(decisionThresholds: DecisionThresholds) {
        val costs = getDriverCosts()
        dao.upsert(costs.toEntity(decisionThresholds))
    }

    override fun observeDriverCosts(): Flow<DriverCosts> =
        dao.observeConfig().map { it?.toDriverCosts() ?: DEFAULT_COSTS }

    override fun observeDecisionThresholds(): Flow<DecisionThresholds> =
        dao.observeConfig().map { it?.toDecisionThresholds() ?: DEFAULT_THRESHOLDS }

    companion object {
        private val DEFAULT_COSTS =
            DriverCosts(
                costPerKm = 2.0,
                costPerMinute = 0.3,
                costPerTrip = 1.0,
                currency = "MXN",
            )
        private val DEFAULT_THRESHOLDS =
            DecisionThresholds(
                minProfit = 20.0,
                minProfitPerHour = 120.0,
            )
    }
}
