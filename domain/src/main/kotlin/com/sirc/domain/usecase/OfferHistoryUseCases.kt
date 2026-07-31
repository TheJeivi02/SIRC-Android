package com.sirc.domain.usecase

import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.repository.OfferHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOfferHistoryUseCase @Inject constructor(
    private val repository: OfferHistoryRepository,
) {
    fun observe(limit: Int = 100): Flow<List<OfferHistoryEntry>> = repository.observeEntries(limit)
}

class ClearOfferHistoryUseCase @Inject constructor(
    private val repository: OfferHistoryRepository,
) {
    suspend operator fun invoke() = repository.clear()
}

class AddOfferHistoryUseCase @Inject constructor(
    private val repository: OfferHistoryRepository,
) {
    suspend operator fun invoke(entry: OfferHistoryEntry) = repository.add(entry)
}
