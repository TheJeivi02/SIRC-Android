package com.sirc.domain.usecase

import com.sirc.domain.engine.ProfitEvaluationEngine
import com.sirc.domain.engine.RecommendationEngine
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.OfferEvaluationResult
import com.sirc.domain.model.TripOffer
import com.sirc.domain.repository.DriverConfigRepository
import javax.inject.Inject

/**
 * Evalúa una oferta con la configuración completa del conductor y genera la
 * recomendación: evaluación + desglose de costos + recomendación accionable.
 */
class EvaluateDetailedOfferUseCase @Inject constructor(
    private val profitEvaluationEngine: ProfitEvaluationEngine,
    private val recommendationEngine: RecommendationEngine,
    private val configRepository: DriverConfigRepository,
) {
    suspend operator fun invoke(offer: TripOffer): OfferEvaluationResult {
        val config: DriverConfig = configRepository.getDriverConfig() ?: DriverConfig.default()
        val detailed = profitEvaluationEngine.evaluate(offer, config)
        return OfferEvaluationResult(
            evaluation = detailed.evaluation,
            breakdown = detailed.breakdown,
            recommendation = recommendationEngine.recommend(detailed.evaluation),
        )
    }
}
