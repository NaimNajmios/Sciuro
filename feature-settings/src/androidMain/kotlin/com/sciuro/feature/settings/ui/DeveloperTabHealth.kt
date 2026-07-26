package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalIncome
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.core.parsing.metrics.ParserHealthRow
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun DeveloperTabHealth(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val healthData by viewModel.healthData.collectAsState()
    val priorHealthData by viewModel.priorHealthData.collectAsState()

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.dev_health_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dev_health_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (healthData.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.dev_health_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.dev_health_package), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.dev_health_processed), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.dev_health_trend), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }

            items(healthData) { row ->
                RowHealthCard(row, priorHealthData.find { it.packageName == row.packageName })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.dev_health_pipeline_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dev_health_pipeline_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val metrics by viewModel.pipelineMetrics.collectAsState()
            
            val llmTotal = metrics?.llmCalls ?: 0L
            val deadLetters = metrics?.deadLetters ?: 0L

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MetricRow(stringResource(R.string.dev_health_llm_calls), "$llmTotal")
                    MetricRow(stringResource(R.string.dev_health_dead_letters), "$deadLetters")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RowHealthCard(row: ParserHealthRow, priorRow: ParserHealthRow?) {
    val matchRate = row.matchRate
    val priorMatchRate = priorRow?.matchRate
    val trendColor = when {
        priorMatchRate == null -> MaterialTheme.colorScheme.onSurfaceVariant
        matchRate >= priorMatchRate -> SignalIncome
        (priorMatchRate - matchRate) > 0.2 -> SignalDanger
        else -> SignalDanger
    }
    val trendIcon = when {
        priorMatchRate == null -> ""
        matchRate >= priorMatchRate -> "\u2191"
        else -> "\u2193"
    }
    val isDegraded = priorMatchRate != null && (priorMatchRate - matchRate) > 0.2

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDegraded)
                SignalDanger.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = row.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${row.processed} / ${row.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "$trendIcon ${"%.0f".format(matchRate * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = trendColor
                )
            }
            if (isDegraded) {
                Text(
                    text = stringResource(R.string.dev_health_degraded, "%.0f".format(priorMatchRate!! * 100)),
                    style = MaterialTheme.typography.bodySmall,
                    color = SignalDanger,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
