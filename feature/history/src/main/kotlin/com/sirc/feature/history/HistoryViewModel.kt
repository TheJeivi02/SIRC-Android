package com.sirc.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.usecase.ClearOfferHistoryUseCase
import com.sirc.domain.usecase.ObserveOfferHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeOfferHistory: ObserveOfferHistoryUseCase,
    private val clearOfferHistory: ClearOfferHistoryUseCase,
    val engine: ProfitEngine,
) : ViewModel() {
    val entries: StateFlow<List<OfferHistoryEntry>> =
        observeOfferHistory.observe(limit = 100)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun clearHistory() {
        viewModelScope.launch { clearOfferHistory() }
    }
}
