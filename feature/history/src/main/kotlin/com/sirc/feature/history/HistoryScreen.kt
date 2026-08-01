package com.sirc.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.components.LabeledValue
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.usecase.HistoryFilters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.totalCount > 0) {
                Text(
                    text = "Borrar",
                    color = SircColors.NotProfit,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.clearHistory() }
                            .padding(8.dp),
                )
            }
        }

        FilterBar(viewModel = viewModel, filters = state.filters)

        Spacer(modifier = Modifier.height(8.dp))

        if (state.entries.isEmpty()) {
            EmptyHistory(querying = state.filters.isActive)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    HistoryItem(
                        entry = entry,
                        engine = viewModel.engine,
                        onClick = { viewModel.select(entry) },
                    )
                }
            }
        }
    }

    state.selected?.let { entry ->
        HistoryDetailDialog(
            entry = entry,
            engine = viewModel.engine,
            onDismiss = viewModel::dismissDetail,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    viewModel: HistoryViewModel,
    filters: HistoryFilters,
) {
    var query by rememberSaveable { mutableStateOf(filters.query) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.setQuery(it)
            },
            label = { Text("Buscar") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.setQuery("")
                    }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlatformDropdown(
                selected = filters.platform,
                onSelect = viewModel::setPlatform,
                modifier = Modifier.weight(1f),
            )
            DecisionDropdown(
                selected = filters.decision,
                onSelect = viewModel::setDecision,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            DatePreset.entries.forEach { preset ->
                val active =
                    when (preset) {
                        DatePreset.ALL -> !filters.isActive || filters.dateFromMillis == null
                        DatePreset.TODAY,
                        DatePreset.LAST_7_DAYS,
                        DatePreset.LAST_30_DAYS,
                        -> filters.dateFromMillis == presetRangeStart(preset)
                    }
                FilterChip(
                    selected = active,
                    onClick = { viewModel.applyPreset(preset) },
                    label = { Text(preset.label) },
                )
            }
            if (filters.isActive) {
                FilterChip(
                    selected = false,
                    onClick = viewModel::clearFilters,
                    label = { Text("Quitar filtros") },
                )
            }
        }
    }
}

private fun presetRangeStart(preset: DatePreset): Long? =
    when (preset) {
        DatePreset.ALL -> null
        DatePreset.TODAY,
        DatePreset.LAST_7_DAYS,
        DatePreset.LAST_30_DAYS,
        -> {
            val now = java.util.Calendar.getInstance()
            when (preset) {
                DatePreset.TODAY -> now
                DatePreset.LAST_7_DAYS -> now.apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }
                DatePreset.LAST_30_DAYS -> now.apply { add(java.util.Calendar.DAY_OF_YEAR, -30) }
                DatePreset.ALL -> now
            }.timeInMillis
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformDropdown(
    selected: RidePlatform?,
    onSelect: (RidePlatform?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.displayName ?: "Todas",
            onValueChange = {},
            readOnly = true,
            label = { Text("Plataforma") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Todas") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            RidePlatform.entries.forEach { platform ->
                DropdownMenuItem(
                    text = { Text(platform.displayName) },
                    onClick = {
                        onSelect(platform)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecisionDropdown(
    selected: Decision?,
    onSelect: (Decision?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let { decisionLabel(it) } ?: "Todos",
            onValueChange = {},
            readOnly = true,
            label = { Text("Resultado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            Decision.entries.forEach { decision ->
                DropdownMenuItem(
                    text = { Text(decisionLabel(decision)) },
                    onClick = {
                        onSelect(decision)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    entry: OfferHistoryEntry,
    engine: com.sirc.domain.engine.ProfitEngine,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = entry.platform.displayName,
                    style = MaterialTheme.typography.labelLarge,
                )
                DecisionBadge(decision = entry.decision, compact = true)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ganancia: ${engine.formatCurrency(entry.estimatedProfit, null)}",
                style = MaterialTheme.typography.labelLarge,
                color =
                    when (entry.decision) {
                        Decision.PROFITABLE -> SircColors.Profit
                        Decision.MARGINAL -> SircColors.Marginal
                        Decision.NOT_PROFITABLE -> SircColors.NotProfit
                    },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(entry.timestampMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryDetailDialog(
    entry: OfferHistoryEntry,
    engine: com.sirc.domain.engine.ProfitEngine,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${entry.platform.displayName} · ${formatTimestamp(entry.timestampMillis)}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DecisionBadge(decision = entry.decision, compact = true)
                    entry.recommendation?.let {
                        com.sirc.core.ui.components.RecommendationBadge(recommendation = it, compact = true)
                    }
                }
                LabeledValue(
                    label = "Precio",
                    value = entry.estimatedTotal?.let { engine.formatCurrency(it, null) } ?: "—",
                )
                LabeledValue(label = "Distancia", value = entry.distanceKm?.let { "${formatNumber(it)} km" } ?: "—")
                LabeledValue(label = "Duración", value = entry.durationMin?.let { "${formatNumber(it)} min" } ?: "—")
                LabeledValue(label = "Ganancia", value = engine.formatCurrency(entry.estimatedProfit, null))
                LabeledValue(label = "Tipo de oferta", value = entry.offerType ?: "—")
                LabeledValue(label = "Confianza", value = confidenceLabel(entry))
                LabeledValue(label = "Recomendación", value = entry.recommendation?.name ?: "—")
                entry.ruleSummary?.takeIf { it.isNotBlank() }?.let {
                    LabeledValue(label = "Reglas", value = it)
                }
                entry.reasons?.takeIf { it.isNotBlank() }?.let {
                    LabeledValue(label = "Motivo", value = it)
                }
                entry.processingMillis?.let {
                    LabeledValue(label = "Procesamiento", value = "${formatNumber(it)} ms")
                }
                entry.rulesMillis?.let {
                    LabeledValue(label = "Tiempo en reglas", value = "${formatNumber(it)} ms")
                }
                entry.evaluationMillis?.let {
                    LabeledValue(label = "Tiempo de evaluación", value = "${formatNumber(it)} ms")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

@Composable
private fun EmptyHistory(querying: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                if (querying) {
                    "Sin resultados para los filtros seleccionados."
                } else {
                    "Aún no hay ofertas evaluadas."
                },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun decisionLabel(decision: Decision): String =
    when (decision) {
        Decision.PROFITABLE -> "Rentable"
        Decision.MARGINAL -> "Al límite"
        Decision.NOT_PROFITABLE -> "No rentable"
    }

private fun confidenceLabel(entry: OfferHistoryEntry): String =
    when {
        entry.confidencePercent != null && entry.confidenceLevel != null ->
            "${entry.confidencePercent}% (${entry.confidenceLevel})"

        entry.confidencePercent != null -> "${entry.confidencePercent}%"
        else -> "—"
    }

private fun formatNumber(value: Double): String = java.text.DecimalFormat("#,##0.0#").format(value).replace(',', '.')

private fun formatTimestamp(millis: Long): String {
    val df = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return df.format(Date(millis))
}
