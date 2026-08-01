package com.sirc.domain.repository

import com.sirc.domain.model.OfferHistoryEntry
import kotlinx.coroutines.flow.Flow

interface OfferHistoryRepository {
    suspend fun add(entry: OfferHistoryEntry)

    suspend fun clear()

    /** Elimina los registros más antiguos hasta dejar como máximo [limit]. */
    suspend fun trimToLimit(limit: Int)

    fun observeEntries(limit: Int = 100): Flow<List<OfferHistoryEntry>>
}
