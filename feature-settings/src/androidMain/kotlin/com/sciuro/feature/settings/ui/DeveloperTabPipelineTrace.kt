package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TraceEventSummary(
    val rawEventId: String?,
    val firstAt: Long?,
    val lastAt: Long?,
    val stageCount: Long
)

data class TraceRow(
    val stage: String,
    val outcome: String,
    val durationMs: Long?,
    val confidence: Double?,
    val detail: String?,
    val createdAt: Long
)

@Composable
fun DeveloperTabPipelineTrace(
    modifier: Modifier = Modifier
) {
    val database: SciuroDatabase = koinInject()
    var events by remember { mutableStateOf<List<TraceEventSummary>>(emptyList()) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var selectedEventTraces by remember { mutableStateOf<List<TraceRow>>(emptyList()) }

    LaunchedEffect(Unit) {
        events = database.pipelineTraceQueries.selectDistinctTraceEvents(100).executeAsList().map {
            TraceEventSummary(it.raw_event_id, it.first_at, it.last_at, it.stage_count)
        }
    }

    fun loadTraces(eventId: String?) {
        if (eventId != null) {
            selectedEventTraces = database.pipelineTraceQueries.selectTraceByEvent(eventId).executeAsList().map {
                TraceRow(it.stage, it.outcome, it.duration_ms, it.confidence, it.detail_json, it.created_at)
            }
            selectedEventId = eventId
        }
    }

    if (selectedEventId != null) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Event ${selectedEventId!!.take(8)}...", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { selectedEventId = null }) { Text("Back") }
            }
            LazyColumn {
                items(selectedEventTraces) { trace ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (trace.outcome) {
                                "SUCCESS" -> MaterialTheme.colorScheme.surfaceVariant
                                "FAILURE", "DROP" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val timeStr = remember(trace.createdAt) {
                                SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(trace.createdAt))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${trace.stage} → ${trace.outcome}", style = MaterialTheme.typography.labelSmall)
                                Text(timeStr, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                            if (trace.durationMs != null) {
                                Text("${trace.durationMs}ms", style = MaterialTheme.typography.labelSmall)
                            }
                            if (trace.confidence != null) {
                                Text("conf: ${"%.2f".format(trace.confidence)}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!trace.detail.isNullOrBlank()) {
                                Text(trace.detail!!.take(200), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (selectedEventTraces.isEmpty()) {
                    item { Text("No traces for this event.", modifier = Modifier.padding(16.dp)) }
                }
            }
        }
        return
    }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Recent pipeline events", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (events.isEmpty()) {
            item {
                Text("No trace data yet. Process a notification to see traces.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp))
            }
        }

        items(events) { event ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                onClick = { loadTraces(event.rawEventId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            event.rawEventId?.take(12) ?: "?",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("${event.stageCount} stages", style = MaterialTheme.typography.labelSmall)
                    }
                    val ageMs = System.currentTimeMillis() - (event.lastAt ?: System.currentTimeMillis())
                    val ageText = when {
                        ageMs < 60_000 -> "${ageMs / 1000}s ago"
                        ageMs < 3_600_000 -> "${ageMs / 60_000}m ago"
                        else -> "${ageMs / 3_600_000}h ago"
                    }
                    Text(ageText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
