package com.sirc.domain.repository

import com.sirc.domain.model.OfferEvaluationRecord
import kotlinx.coroutines.flow.Flow

/**
 * Historial temporal de ofertas evaluadas, retenido en memoria durante la vida
 * del proceso (sin Room). Conserva las últimas [limit] entradas.
 */
interface OfferEvaluationRepository {
    suspend fun add(record: OfferEvaluationRecord)

    suspend fun clear()

    fun observe(limit: Int = 100): Flow<List<OfferEvaluationRecord>>
}
