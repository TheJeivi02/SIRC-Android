package com.sirc.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.usecase.ClearOfferHistoryUseCase
import com.sirc.domain.usecase.HistoryFilter
import com.sirc.domain.usecase.HistoryFilters
import com.sirc.domain.usecase.ObserveOfferHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Presets de rango de fechas del Historial. */
enum class DatePreset(val label: String) {
    TODAY("Hoy"),
    LAST_7_DAYS("7 días"),
    LAST_30_DAYS("30 días"),
    ALL("Todo"),
}

/** Estado de la pantalla Historial: filtros, resultado filtrado y detalle. */
data class HistoryUiState(
    val totalCount: Int = 0,
    val entries: List<OfferHistoryEntry> = emptyList(),
    val filters: HistoryFilters = HistoryFilters(),
    val selected: OfferHistoryEntry? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeOfferHistory: ObserveOfferHistoryUseCase,
    private val clearOfferHistory: ClearOfferHistoryUseCase,
    val engine: ProfitEngine,
) : ViewModel() {
    private val entries =
        observeOfferHistory.observe(limit = HISTORY_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val filters = MutableStateFlow(HistoryFilters())
    private val selected = MutableStateFlow<OfferHistoryEntry?>(null)

    val uiState: StateFlow<HistoryUiState> =
        combine(entries, filters, selected) { all, currentFilters, currentSelected ->
            HistoryUiState(
                totalCount = all.size,
                entries = HistoryFilter.filter(all, currentFilters),
                filters = currentFilters,
                selected = currentSelected,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun setPlatform(platform: RidePlatform?) {
        filters.update { it.copy(platform = platform) }
    }

    fun setDecision(decision: Decision?) {
        filters.update { it.copy(decision = decision) }
    }

    fun setQuery(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun setDateRange(
        fromMillis: Long?,
        toMillis: Long?,
    ) {
        filters.update { it.copy(dateFromMillis = fromMillis, dateToMillis = toMillis) }
    }

    fun applyPreset(preset: DatePreset) {
        val now = Calendar.getInstance()
        val fromMillis =
            when (preset) {
                DatePreset.TODAY -> startOfDay(now).timeInMillis
                DatePreset.LAST_7_DAYS -> startOfDay(now).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                DatePreset.LAST_30_DAYS -> startOfDay(now).apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                DatePreset.ALL -> null
            }
        filters.update { it.copy(dateFromMillis = fromMillis, dateToMillis = null) }
    }

    fun clearFilters() {
        filters.value = HistoryFilters()
    }

    fun select(entry: OfferHistoryEntry) {
        selected.value = entry
    }

    fun dismissDetail() {
        selected.value = null
    }

    fun clearHistory() {
        viewModelScope.launch { clearOfferHistory() }
    }

    private fun startOfDay(calendar: Calendar): Calendar {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private companion object {
        const val HISTORY_LIMIT = 500
    }
}
