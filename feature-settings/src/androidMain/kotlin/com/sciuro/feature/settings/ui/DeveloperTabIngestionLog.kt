package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperTabIngestionLog(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val deadLetterEvents by viewModel.deadLetterEvents.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val deadLetterCount by viewModel.deadLetterCount.collectAsState()

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pending", style = MaterialTheme.typography.labelMedium)
                        Text("$pendingCount", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Dead Letter", style = MaterialTheme.typography.labelMedium)
                        Text("$deadLetterCount", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }

        if (deadLetterEvents.isEmpty()) {
            item {
                Text(
                    "No dead-letter events.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        items(deadLetterEvents) { event ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val timeStr = remember(event.captured_at) {
                        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(event.captured_at))
                    }
                    Text("$timeStr — ${event.source_package_or_address}", style = MaterialTheme.typography.labelSmall)
                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                    Text(event.text, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                    event.last_error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
