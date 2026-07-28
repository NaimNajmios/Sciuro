package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton

@Composable
fun DeveloperTabSources(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val allowlist = viewModel.ingestionAllowlist
    var customPackage by remember { mutableStateOf("") }
    var pendingRemovePackage by remember { mutableStateOf<String?>(null) }
    var pendingEditPackage by remember { mutableStateOf<String?>(null) }
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

    val editPackage = pendingEditPackage
    if (editPackage != null) {
        var editValue by remember(editPackage) { mutableStateOf(editPackage) }

        com.najmi.sciuro.core.ui.components.SciuroFormSheet(
            title = stringResource(R.string.dev_sources_edit_title),
            onDismissRequest = { pendingEditPackage = null }
        ) {
            SciuroTextField(
                value = editValue,
                onValueChange = { editValue = it },
                label = stringResource(R.string.dev_sources_edit_label),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            SciuroPrimaryButton(
                text = stringResource(R.string.dev_sources_edit_action),
                onClick = {
                    val trimmed = editValue.trim()
                    if (trimmed.isNotBlank() && trimmed != editPackage) {
                        allowlist.renamePackage(editPackage, trimmed)
                    }
                    pendingEditPackage = null
                },
                enabled = editValue.isNotBlank()
            )
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
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
                SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.dev_sources_banks),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        bankPackages.forEach { pkg ->
                            SourceRow(
                                pkg = pkg,
                                onEdit = { pendingEditPackage = pkg },
                                onRemove = { pendingRemovePackage = pkg }
                            )
                        }
                    }
                }
            }
        }

        if (aggPackages.isNotEmpty()) {
            item {
                SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.dev_sources_aggregators),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        aggPackages.forEach { pkg ->
                            SourceRow(
                                pkg = pkg,
                                onEdit = { pendingEditPackage = pkg },
                                onRemove = { pendingRemovePackage = pkg }
                            )
                        }
                    }
                }
            }
        }

        if (customPackages.isNotEmpty()) {
            item {
                SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.dev_sources_custom),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        customPackages.forEach { pkg ->
                            SourceRow(
                                pkg = pkg,
                                onEdit = { pendingEditPackage = pkg },
                                onRemove = { pendingRemovePackage = pkg }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
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
        }
    }
}

@Composable
private fun SourceRow(
    pkg: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            pkg,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.dev_sources_edit_action),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.dev_sources_remove_action),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
