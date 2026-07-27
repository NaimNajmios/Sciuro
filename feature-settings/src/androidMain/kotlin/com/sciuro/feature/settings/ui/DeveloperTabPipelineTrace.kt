package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.sciuro.feature.settings.viewmodel.TraceEventSummary
import com.sciuro.feature.settings.viewmodel.TraceRow
import kotlinx.coroutines.launch
import com.najmi.sciuro.core.ui.components.SciuroTextField

@Composable
fun DeveloperTabPipelineTrace(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.pipelineEvents.collectAsState()
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var selectedEventTraces by remember { mutableStateOf<List<TraceRow>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    val traceFilterStatus by viewModel.traceFilterStatus.collectAsState()
    val traceFilterPackage by viewModel.traceFilterPackage.collectAsState()
    val showOnlyAllowlisted by viewModel.showOnlyAllowlisted.collectAsState()

    fun loadTraces(eventId: String?) {
        if (eventId != null) {
            scope.launch {
                selectedEventTraces = viewModel.getTraceDetails(eventId)
                selectedEventId = eventId
            }
        }
    }

    if (selectedEventId != null) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.dev_trace_event_label, selectedEventId!!.take(8)),
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { selectedEventId = null }) { Text(stringResource(R.string.dev_trace_back)) }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
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
                                Text("${trace.stage} \u2192 ${trace.outcome}", style = MaterialTheme.typography.labelSmall)
                                Text(timeStr, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                            if (trace.durationMs != null) {
                                Text("${trace.durationMs}ms", style = MaterialTheme.typography.labelSmall)
                            }
                            if (trace.confidence != null) {
                                Text("conf: ${"%.2f".format(trace.confidence)}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!trace.detail.isNullOrBlank()) {
                                Text(trace.detail.take(200), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (selectedEventTraces.isEmpty()) {
                    item { Text(stringResource(R.string.dev_trace_no_traces), modifier = Modifier.padding(16.dp)) }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.dev_trace_title), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var pkgText by remember(traceFilterPackage) { mutableStateOf(traceFilterPackage) }
            SciuroTextField(
                value = pkgText,
                onValueChange = { pkgText = it },
                label = "Package",
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { viewModel.setTraceFilter(traceFilterStatus, pkgText) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Apply")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "SUCCESS", "FAILURE", "DROP").forEach { status ->
                FilterChip(
                    selected = traceFilterStatus == status,
                    onClick = { viewModel.setTraceFilter(status, traceFilterPackage) },
                    label = { Text(status) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = showOnlyAllowlisted,
                onClick = { viewModel.setShowOnlyAllowlisted(!showOnlyAllowlisted) },
                label = { Text("Allowlisted") }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (events.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.dev_trace_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

        items(events) { event ->
            SciuroCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                onClick = { loadTraces(event.rawEventId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            event.packageName ?: stringResource(R.string.dev_trace_unknown_id),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            event.rawEventId?.take(8) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(stringResource(R.string.dev_trace_stages, event.stageCount.toInt()), style = MaterialTheme.typography.labelSmall)
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
}}
