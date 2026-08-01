package com.sirc.data.repository

import com.sirc.data.local.dao.OfferHistoryDao
import com.sirc.data.local.mapper.toDomain
import com.sirc.data.local.mapper.toEntity
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.repository.OfferHistoryRepository
import com.sirc.domain.repository.OverlayConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementación Room del historial de ofertas.
 *
 * Al insertar, recorta automáticamente a [OverlayConfigRepository]`historyLimit`
 * para evitar que la base crezca indefinidamente.
 */
class DefaultOfferHistoryRepository @Inject constructor(
    private val dao: OfferHistoryDao,
    private val overlayConfigRepository: OverlayConfigRepository,
) : OfferHistoryRepository {
    override suspend fun add(entry: OfferHistoryEntry) {
        dao.insert(entry.toEntity())
        trimToLimit(overlayConfigRepository.getOverlayConfig().historyLimit)
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun trimToLimit(limit: Int) {
        dao.trimToLimit(limit.coerceAtLeast(MIN_LIMIT))
    }

    override fun observeEntries(limit: Int): Flow<List<OfferHistoryEntry>> =
        dao.observeEntries(limit).map { list -> list.map { it.toDomain() } }

    private companion object {
        const val MIN_LIMIT = 50
    }
}
