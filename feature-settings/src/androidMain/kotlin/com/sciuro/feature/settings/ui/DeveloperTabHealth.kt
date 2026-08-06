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
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.core.parsing.metrics.ParserHealthRow
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel

@Composable
fun DeveloperTabHealth(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSciuroSemanticTokens.current
    val healthData by viewModel.healthData.collectAsState()
    val priorHealthData by viewModel.priorHealthData.collectAsState()
    val metrics by viewModel.pipelineMetrics.collectAsState()
    val eventBusMetrics by viewModel.eventBusMetrics.collectAsState()
    val auditIntegrityGaps by viewModel.auditIntegrityGaps.collectAsState()
    val recoveryMetrics by viewModel.recoveryMetrics.collectAsState()

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
                                    color = if (avgMatchRate >= 0.7f) tokens.signalIncome else tokens.signalDanger
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
                    val dailyCount = metrics?.dailyLlmCallCount ?: 0L
                    val dailyLimit = metrics?.dailyLlmCallLimit ?: 50
                    val cacheHitRate = metrics?.cacheHitRate ?: 0f

                    MetricRow(stringResource(R.string.dev_health_llm_calls), llmTotal.toString())
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    MetricRowColored(
                        stringResource(R.string.dev_health_llm_daily, dailyCount, dailyLimit),
                        "$dailyCount / $dailyLimit",
                        if (dailyCount >= dailyLimit) tokens.signalDanger else tokens.signalIncome
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    MetricRow(stringResource(R.string.dev_health_cache_hit_rate), "${"%.0f".format(cacheHitRate * 100)}%")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    MetricRow(stringResource(R.string.dev_health_dead_letters), deadLetters.toString())
                }
            }
        }

        item {
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_events_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_events_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (eventBusMetrics != null) {
                        val ebm = eventBusMetrics!!

                        val pendingColor = when {
                            ebm.pendingCount > 50 -> tokens.signalDanger
                            ebm.pendingCount > 10 -> tokens.signalWarning
                            else -> tokens.signalIncome
                        }
                        val deadColor = if (ebm.deadLetterCount > 0) tokens.signalDanger else tokens.signalIncome
                        val retryColor = if (ebm.retryCount > 0) tokens.signalWarning else tokens.signalIncome
                        val dropColor = if (ebm.liveDropCount > 0) tokens.signalDanger else tokens.signalIncome

                        MetricRowColored(stringResource(R.string.dev_events_pending), ebm.pendingCount.toString(), pendingColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRowColored(stringResource(R.string.dev_events_dead_letters), ebm.deadLetterCount.toString(), deadColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRowColored(stringResource(R.string.dev_events_retries), ebm.retryCount.toString(), retryColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRowColored(stringResource(R.string.dev_events_live_drops), ebm.liveDropCount.toString(), dropColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(stringResource(R.string.dev_events_oldest_age), formatDuration(ebm.oldestPendingAgeMs))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(stringResource(R.string.dev_events_lag), formatDuration(ebm.subscriberLagMs))
                    } else {
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
                    Text(stringResource(R.string.dev_health_audit_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_health_audit_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val gapColor = if (auditIntegrityGaps > 0L) tokens.signalDanger else tokens.signalIncome
                    MetricRowColored(stringResource(R.string.dev_health_audit_gaps), auditIntegrityGaps.toString(), gapColor)
                }
            }
        }

        item {
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_recovery_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_recovery_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val rm = recoveryMetrics
                    if (rm != null) {
                        MetricRow(stringResource(R.string.dev_recovery_quarantine_count), rm.quarantineCount.toString())
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(
                            stringResource(R.string.dev_recovery_last_quarantine),
                            if (rm.lastQuarantineTimestamp > 0L) formatDate(rm.lastQuarantineTimestamp)
                            else stringResource(R.string.dev_recovery_never)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(stringResource(R.string.dev_recovery_quarantined_files), rm.quarantinedFileCount.toString())
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(
                            stringResource(R.string.dev_recovery_last_integrity_check),
                            if (rm.lastIntegrityCheckMs > 0L) formatDate(rm.lastIntegrityCheckMs)
                            else stringResource(R.string.dev_recovery_never)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        MetricRow(
                            stringResource(R.string.dev_recovery_integrity_result),
                            rm.lastIntegrityResult ?: stringResource(R.string.dev_recovery_never)
                        )
                    } else {
                        Text(
                            stringResource(R.string.dev_recovery_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RowHealthCard(row: ParserHealthRow, priorRow: ParserHealthRow?) {
    val tokens = LocalSciuroSemanticTokens.current
    val matchRate = row.matchRate
    val priorMatchRate = priorRow?.matchRate
    val trendColor = when {
        priorMatchRate == null -> MaterialTheme.colorScheme.onSurfaceVariant
        matchRate >= priorMatchRate -> tokens.signalIncome
        (priorMatchRate - matchRate) > 0.2 -> tokens.signalDanger
        else -> tokens.signalDanger
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

@Composable
private fun MetricRowColored(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0s"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatDate(ms: Long): String =
    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(ms))
