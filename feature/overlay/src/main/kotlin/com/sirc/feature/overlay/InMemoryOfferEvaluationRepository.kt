package com.sirc.feature.overlay

import com.sirc.domain.model.OfferEvaluationRecord
import com.sirc.domain.repository.OfferEvaluationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Historial temporal de ofertas evaluadas en memoria (sin Room).
 *
 * Conserva las últimas [MAX_RECORDS] ofertas durante la vida del proceso y
 * asigna un [OfferEvaluationRecord.id] incremental a cada entrada.
 */
@Singleton
class InMemoryOfferEvaluationRepository @Inject constructor() : OfferEvaluationRepository {
    private val buffer = ArrayDeque<OfferEvaluationRecord>()
    private val records = MutableStateFlow<List<OfferEvaluationRecord>>(emptyList())
    private var nextId = 1L

    override suspend fun add(record: OfferEvaluationRecord) {
        synchronized(this) {
            buffer.addLast(record.copy(id = nextId++))
            while (buffer.size > MAX_RECORDS) buffer.removeFirst()
            records.value = buffer.toList()
        }
    }

    override suspend fun clear() {
        synchronized(this) {
            buffer.clear()
            records.value = emptyList()
        }
    }

    override fun observe(limit: Int): Flow<List<OfferEvaluationRecord>> = records.map { list -> list.takeLast(limit) }

    companion object {
        private const val MAX_RECORDS = 100
    }
}
