package com.sirc.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirc.core.ui.components.DecisionBadge
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
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

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Aún no hay ofertas evaluadas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryItem(entry = entry, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    entry: OfferHistoryEntry,
    viewModel: HistoryViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.padding(2.dp))
            Text(
                text = "Ganancia: ${viewModel.engine.formatCurrency(entry.estimatedProfit, null)}",
                style = MaterialTheme.typography.labelLarge,
                color =
                    when (entry.decision) {
                        Decision.PROFITABLE -> SircColors.Profit
                        Decision.MARGINAL -> SircColors.Marginal
                        Decision.NOT_PROFITABLE -> SircColors.NotProfit
                    },
            )
            Spacer(modifier = Modifier.padding(2.dp))
            Text(
                text = formatTimestamp(entry.timestampMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val df = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
    return df.format(java.util.Date(millis))
}
