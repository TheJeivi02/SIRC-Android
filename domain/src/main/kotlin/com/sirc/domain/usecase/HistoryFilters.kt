package com.sirc.domain.usecase

import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.RidePlatform

/** Criterios de filtrado de la pantalla Historial. */
data class HistoryFilters(
    val platform: RidePlatform? = null,
    val decision: Decision? = null,
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val query: String = "",
) {
    val isActive: Boolean
        get() =
            platform != null ||
                decision != null ||
                dateFromMillis != null ||
                dateToMillis != null ||
                query.isNotBlank()
}

/** Aplica los [HistoryFilters] sobre las entradas del historial. */
object HistoryFilter {
    fun filter(
        entries: List<OfferHistoryEntry>,
        filters: HistoryFilters,
    ): List<OfferHistoryEntry> {
        var result = entries
        if (filters.platform != null) result = result.filter { it.platform == filters.platform }
        if (filters.decision != null) result = result.filter { it.decision == filters.decision }
        if (filters.dateFromMillis != null) result = result.filter { it.timestampMillis >= filters.dateFromMillis }
        if (filters.dateToMillis != null) result = result.filter { it.timestampMillis <= filters.dateToMillis }
        val needle = filters.query.trim().lowercase()
        if (needle.isNotEmpty()) {
            result =
                result.filter { entry ->
                    entry.summary.lowercase().contains(needle) ||
                        entry.platform.displayName.lowercase().contains(needle) ||
                        entry.offerType?.lowercase()?.contains(needle) == true ||
                        entry.reasons?.lowercase()?.contains(needle) == true ||
                        entry.ruleSummary?.lowercase()?.contains(needle) == true
                }
        }
        return result
    }
}
