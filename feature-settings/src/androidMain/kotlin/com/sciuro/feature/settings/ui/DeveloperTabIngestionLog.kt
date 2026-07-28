package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperTabIngestionLog(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val deadLetterEvents by viewModel.deadLetterEvents.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val deadLetterCount by viewModel.deadLetterCount.collectAsState()

    var selectedDeadLetter by remember { mutableStateOf<com.sciuro.core.ledger.db.Raw_event_staging?>(null) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            pendingCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.dev_ingestion_pending),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            deadLetterCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (deadLetterCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.dev_ingestion_dead_letter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dead-Letter Events",
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = { viewModel.refreshCounts() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = androidx.compose.ui.Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (deadLetterEvents.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.dev_ingestion_no_dead_letter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        items(deadLetterEvents) { event ->
            SciuroCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = { selectedDeadLetter = event }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val timeStr = remember(event.captured_at) {
                        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(event.captured_at))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$timeStr — ${event.source_package_or_address}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (event.last_error != null) {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                    Text(event.text, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.resendDeadLetter(event.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.dev_ingestion_resend),
                            modifier = androidx.compose.ui.Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dev_ingestion_resend))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedDeadLetter?.let { event ->
        var editTitle by remember(event) { mutableStateOf(event.title) }
        var editText by remember(event) { mutableStateOf(event.text) }

        com.najmi.sciuro.core.ui.components.SciuroFormSheet(
            title = "Edit Dead Letter",
            onDismissRequest = { selectedDeadLetter = null }
        ) {
            com.najmi.sciuro.core.ui.components.SciuroTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = "Title",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            com.najmi.sciuro.core.ui.components.SciuroTextField(
                value = editText,
                onValueChange = { editText = it },
                label = "Payload (JSON/Text)",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).heightIn(min = 120.dp),
                minLines = 4,
                maxLines = 10
            )

            Button(
                onClick = {
                    viewModel.updateAndResendDeadLetter(event.id, editTitle, editText)
                    selectedDeadLetter = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update & Resend")
            }
        }
    }
}
