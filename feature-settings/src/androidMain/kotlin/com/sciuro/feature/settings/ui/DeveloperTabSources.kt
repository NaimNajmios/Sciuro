package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.feature.settings.R
import com.najmi.sciuro.core.ui.components.SciuroTextField
import org.koin.compose.getKoin

@Composable
fun DeveloperTabSources(modifier: Modifier = Modifier) {
    val allowlist: MutableIngestionAllowlist = getKoin().get()
    var customPackage by remember { mutableStateOf("") }
    var pendingRemovePackage by remember { mutableStateOf<String?>(null) }
    val effectivePackages by allowlist.effectivePackages.collectAsState()

    val removeDialog = pendingRemovePackage
    if (removeDialog != null) {
        AlertDialog(
            onDismissRequest = { pendingRemovePackage = null },
            title = { Text(stringResource(R.string.dev_sources_remove_title)) },
            text = { Text(stringResource(R.string.dev_sources_remove_confirm, removeDialog)) },
            confirmButton = {
                TextButton(onClick = {
                    allowlist.removePackage(removeDialog)
                    pendingRemovePackage = null
                }) { Text(stringResource(R.string.dev_sources_remove_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemovePackage = null }) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.dev_sources_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val bankPackages = effectivePackages
            .filter { allowlist.isDefaultBankPackage(it) }
            .sorted()
        val aggPackages = effectivePackages
            .filter { allowlist.isDefaultAggregatorPackage(it) }
            .sorted()
        val customPackages = effectivePackages
            .filter { allowlist.isUserAddedPackage(it) }
            .sorted()

        if (bankPackages.isNotEmpty()) {
            item {
                Text(stringResource(R.string.dev_sources_banks), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(bankPackages) { pkg ->
                SourceRow(
                    pkg = pkg,
                    isRemovable = true,
                    onRemove = { pendingRemovePackage = pkg }
                )
            }
        }

        if (aggPackages.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.dev_sources_aggregators), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(aggPackages) { pkg ->
                SourceRow(
                    pkg = pkg,
                    isRemovable = true,
                    onRemove = { pendingRemovePackage = pkg }
                )
            }
        }

        if (customPackages.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.dev_sources_custom), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(customPackages) { pkg ->
                SourceRow(
                    pkg = pkg,
                    isRemovable = true,
                    onRemove = { pendingRemovePackage = pkg }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                SciuroTextField(
                    value = customPackage,
                    onValueChange = { customPackage = it },
                    label = stringResource(R.string.dev_sources_add_hint),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val normalized = customPackage.trim()
                        if (normalized.isNotBlank()) {
                            allowlist.addPackage(normalized)
                            customPackage = ""
                        }
                    },
                    enabled = customPackage.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dev_sources_add_action))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SourceRow(
    pkg: String,
    isRemovable: Boolean,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                pkg,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            if (isRemovable) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.dev_sources_remove_action),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
