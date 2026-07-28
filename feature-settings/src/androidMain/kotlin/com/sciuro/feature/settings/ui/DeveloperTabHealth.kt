package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

@Composable
fun DeveloperTabHealth(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val healthData by viewModel.healthData.collectAsState()
    val priorHealthData by viewModel.priorHealthData.collectAsState()
    val metrics by viewModel.pipelineMetrics.collectAsState()

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_health_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_health_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (healthData.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val totalProcessed = healthData.sumOf { it.processed.toLong() }
                        val totalTotal = healthData.sumOf { it.total.toLong() }
                        val avgMatchRate = if (healthData.isNotEmpty())
                            healthData.map { it.matchRate }.average().toFloat()
                        else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${healthData.size}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Packages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$totalProcessed / $totalTotal",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Processed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${"%.0f".format(avgMatchRate * 100)}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (avgMatchRate >= 0.7f) SignalIncome else SignalDanger
                                )
                                Text(
                                    "Avg. Match Rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.dev_health_package), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.dev_health_processed), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.dev_health_trend), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        healthData.forEach { row ->
                            RowHealthCard(row, priorHealthData.find { it.packageName == row.packageName })
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.dev_health_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_health_pipeline_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_health_pipeline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val llmTotal = metrics?.llmCalls ?: 0L
                    val deadLetters = metrics?.deadLetters ?: 0L

                    MetricRow(stringResource(R.string.dev_health_llm_calls), llmTotal.toString())
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    MetricRow(stringResource(R.string.dev_health_dead_letters), deadLetters.toString())
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
        priorMatchRate == null -> "-"
        matchRate >= priorMatchRate -> "\u2191"
        else -> "\u2193"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.packageName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${row.processed} / ${row.total}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "$trendIcon ${"%.0f".format(matchRate * 100)}%",
            style = MaterialTheme.typography.bodySmall,
            color = trendColor
        )
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
