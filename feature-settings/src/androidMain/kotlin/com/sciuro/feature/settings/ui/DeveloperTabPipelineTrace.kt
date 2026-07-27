package com.sciuro.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalIncome
import com.najmi.sciuro.core.ui.theme.SignalTransfer
import com.najmi.sciuro.core.ui.theme.SignalWarning
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import com.sciuro.feature.settings.viewmodel.TraceEventSummary
import com.sciuro.feature.settings.viewmodel.TraceRow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val statusOptions = listOf("ALL", "SUCCESS", "FAILURE", "DROP")

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
        PipelineTraceDetail(
            eventId = selectedEventId!!,
            traces = selectedEventTraces,
            onBack = { selectedEventId = null },
            modifier = modifier
        )
        return
    }

    PipelineTraceEventList(
        events = events,
        filterStatus = traceFilterStatus,
        filterPackage = traceFilterPackage,
        showOnlyAllowlisted = showOnlyAllowlisted,
        onStatusFilterChange = { viewModel.setTraceFilter(it, traceFilterPackage) },
        onPackageFilterApply = { viewModel.setTraceFilter(traceFilterStatus, it) },
        onAllowlistedToggle = { viewModel.setShowOnlyAllowlisted(!showOnlyAllowlisted) },
        onEventClick = { loadTraces(it.rawEventId) },
        modifier = modifier
    )
}

@Composable
private fun PipelineTraceEventList(
    events: List<TraceEventSummary>,
    filterStatus: String,
    filterPackage: String,
    showOnlyAllowlisted: Boolean,
    onStatusFilterChange: (String) -> Unit,
    onPackageFilterApply: (String) -> Unit,
    onAllowlistedToggle: () -> Unit,
    onEventClick: (TraceEventSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    var pkgText by remember(filterPackage) { mutableStateOf(filterPackage) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.dev_trace_title), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                SciuroTextField(
                    value = pkgText,
                    onValueChange = { pkgText = it },
                    label = "Package",
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { onPackageFilterApply(pkgText) }) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            PillToggle(
                options = statusOptions,
                selectedOption = filterStatus,
                onOptionSelected = onStatusFilterChange,
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = showOnlyAllowlisted,
                    onClick = onAllowlistedToggle,
                    label = { Text(stringResource(R.string.dev_trace_allowlisted_only)) }
                )
            }
        }

        if (events.isEmpty()) {
            item {
                EmptyStateView(
                    message = stringResource(R.string.dev_trace_no_data),
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }

        items(events) { event ->
            PipelineEventCard(event = event, onClick = { onEventClick(event) })
        }

        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

@Composable
private fun PipelineEventCard(
    event: TraceEventSummary,
    onClick: () -> Unit
) {
    SciuroCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.packageName ?: stringResource(R.string.dev_trace_unknown_id),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.rawEventId?.take(8) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.dev_trace_stages, event.stageCount.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val ageMs = System.currentTimeMillis() - (event.lastAt ?: System.currentTimeMillis())
            val ageText = when {
                ageMs < 60_000 -> "${ageMs / 1000}s ago"
                ageMs < 3_600_000 -> "${ageMs / 60_000}m ago"
                else -> "${ageMs / 3_600_000}h ago"
            }
            Text(
                ageText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PipelineTraceDetail(
    eventId: String,
    traces: List<TraceRow>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDuration = traces.sumOf { it.durationMs ?: 0 }
    val successCount = traces.count { it.outcome == "SUCCESS" }
    val failCount = traces.count { it.outcome == "FAILURE" || it.outcome == "DROP" }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.dev_trace_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    stringResource(R.string.dev_trace_event_label, eventId.take(8)),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        item {
            TraceSummaryCard(
                stageCount = traces.size,
                totalDurationMs = totalDuration,
                successCount = successCount,
                failCount = failCount
            )
        }

        itemsIndexed(traces) { index, trace ->
            PipelineStageRow(
                stage = trace.stage,
                outcome = trace.outcome,
                durationMs = trace.durationMs,
                confidence = trace.confidence,
                detail = trace.detail,
                createdAt = trace.createdAt,
                isFirst = index == 0,
                isLast = index == traces.lastIndex
            )
        }

        if (traces.isEmpty()) {
            item {
                EmptyStateView(
                    message = stringResource(R.string.dev_trace_no_traces),
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

@Composable
private fun TraceSummaryCard(
    stageCount: Int,
    totalDurationMs: Long,
    successCount: Int,
    failCount: Int
) {
    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.dev_trace_summary_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dev_trace_summary_stages, stageCount),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        stringResource(R.string.dev_trace_summary_duration, totalDurationMs),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dev_trace_summary_passed, successCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalIncome
                    )
                    if (failCount > 0) {
                        Text(
                            stringResource(R.string.dev_trace_summary_failed, failCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = SignalDanger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStageRow(
    stage: String,
    outcome: String,
    durationMs: Long?,
    confidence: Double?,
    detail: String?,
    createdAt: Long,
    isFirst: Boolean,
    isLast: Boolean
) {
    val outcomeColor = outcomeColor(outcome)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    val timeStr = remember(createdAt) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(createdAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(outcomeColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        SciuroCard(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 4.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stage, style = MaterialTheme.typography.labelMedium)
                        Text(
                            " \u2192 ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            outcome,
                            style = MaterialTheme.typography.labelMedium,
                            color = outcomeColor
                        )
                    }
                    Text(
                        timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (durationMs != null) {
                        Text(
                            "${durationMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (confidence != null) {
                        Text(
                            "conf: ${"%.2f".format(confidence)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!detail.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        detail.take(200),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun outcomeColor(outcome: String) = when (outcome) {
    "SUCCESS" -> SignalIncome
    "FAILURE", "DROP" -> SignalDanger
    "FALLBACK" -> SignalWarning
    "SKIP" -> SignalTransfer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
