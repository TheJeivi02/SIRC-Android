package com.sirc.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.model.HistoryStats
import com.sirc.domain.usecase.HistoryStatsCalculator
import com.sirc.domain.usecase.ObserveOfferHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Calcula las estadísticas del Dashboard desde el historial persistente. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    observeOfferHistory: ObserveOfferHistoryUseCase,
) : ViewModel() {
    val stats: StateFlow<HistoryStats> =
        observeOfferHistory.observe(limit = STATS_LIMIT)
            .map(HistoryStatsCalculator::calculate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryStats(),
            )

    private companion object {
        const val STATS_LIMIT = 500
    }
}
