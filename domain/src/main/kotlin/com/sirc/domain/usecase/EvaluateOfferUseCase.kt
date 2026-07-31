package com.sirc.domain.usecase

import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.TripOffer
import com.sirc.domain.repository.DriverConfigRepository
import javax.inject.Inject

/** Evalúa una oferta aplicando el motor de rentabilidad con la configuración actual. */
class EvaluateOfferUseCase @Inject constructor(
    private val engine: ProfitEngine,
    private val configRepository: DriverConfigRepository,
) {
    suspend operator fun invoke(offer: TripOffer): ProfitEvaluation {
        val costs: DriverCosts = configRepository.getDriverCosts()
        val thresholds: DecisionThresholds = configRepository.getDecisionThresholds()
        return engine.evaluate(offer, costs, thresholds)
    }
}
