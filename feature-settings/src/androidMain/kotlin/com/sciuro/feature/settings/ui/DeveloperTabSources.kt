package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.core.ingestion.config.IngestionConfig
import com.najmi.sciuro.core.ui.components.SciuroTextField

@Composable
fun DeveloperTabSources(modifier: Modifier = Modifier) {
    var customPackage by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Notification Sources", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Direct Bank Apps", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
        }

        val bankPackages = IngestionConfig.directBankPackages.sorted().toList()
        items(bankPackages) { pkg ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                tonalElevation = 1.dp
            ) {
                Text(pkg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Aggregator Apps", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
        }

        val aggPackages = IngestionConfig.aggregatorPackages.sorted().toList()
        items(aggPackages) { pkg ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                tonalElevation = 1.dp
            ) {
                Text(pkg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SciuroTextField(
                value = customPackage,
                onValueChange = { customPackage = it },
                label = "Add Custom Package",
                enabled = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Package editing will be available in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
