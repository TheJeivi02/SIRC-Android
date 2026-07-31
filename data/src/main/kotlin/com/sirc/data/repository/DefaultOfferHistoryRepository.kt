package com.sirc.data.repository

import com.sirc.data.local.dao.OfferHistoryDao
import com.sirc.data.local.mapper.toDomain
import com.sirc.data.local.mapper.toEntity
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.repository.OfferHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultOfferHistoryRepository @Inject constructor(
    private val dao: OfferHistoryDao,
) : OfferHistoryRepository {
    override suspend fun add(entry: OfferHistoryEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun clear() {
        dao.clear()
    }

    override fun observeEntries(limit: Int): Flow<List<OfferHistoryEntry>> =
        dao.observeEntries(limit).map { list -> list.map { it.toDomain() } }
}
