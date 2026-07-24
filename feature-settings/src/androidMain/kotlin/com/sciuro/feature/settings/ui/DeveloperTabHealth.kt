package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalIncome
import com.sciuro.core.parsing.metrics.ParserHealthRepository
import com.sciuro.core.parsing.metrics.ParserHealthRow
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

@Composable
fun DeveloperTabHealth(modifier: Modifier = Modifier) {
    val healthRepo: ParserHealthRepository = getKoin().get()
    var healthData by remember { mutableStateOf<List<ParserHealthRow>>(emptyList()) }
    var priorHealthData by remember { mutableStateOf<List<ParserHealthRow>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            val now = System.currentTimeMillis()
            val sinceMs = now - sevenDaysMs
            val priorStart = sinceMs - sevenDaysMs
            healthData = healthRepo.getMatchRatesSince(sinceMs)
            priorHealthData = healthRepo.getMatchRatesInWindow(priorStart, sinceMs)
        }
    }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Parser Health", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Per-package match rates over the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (healthData.isEmpty()) {
            item {
                Text(
                    "No ingestion events in the last 7 days.",
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
                    Text("Package", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Processed / Total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Trend", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }

            items(healthData) { row ->
                RowHealthCard(row, priorHealthData.find { it.packageName == row.packageName })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pipeline Metrics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Trace-based rates over the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val database: SciuroDatabase = getKoin().get()
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            val sinceMs = System.currentTimeMillis() - sevenDaysMs

            val outcomeCounts = remember {
                database.pipelineTraceQueries.countTraceByOutcomeSince(sinceMs).executeAsList()
            }

            val llmTotal = outcomeCounts.filter { it.stage == "PARSE_LLM" }.sumOf { it.cnt }
            val deadLetters = outcomeCounts.filter { it.stage == "STAGING" && it.outcome == "FAILURE" }.sumOf { it.cnt }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MetricRow("LLM fallback calls (7d)", "$llmTotal")
                    MetricRow("Dead letters (7d)", "$deadLetters")
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
        matchRate >= priorMatchRate -> "↑"
        else -> "↓"
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
                    text = "Match rate dropped from ${"%.0f".format(priorMatchRate!! * 100)}% — format drift may be occurring.",
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
