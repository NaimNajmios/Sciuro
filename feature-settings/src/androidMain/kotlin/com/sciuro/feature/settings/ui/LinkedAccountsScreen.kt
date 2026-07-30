package com.sciuro.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SheetList


import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel

@Composable
fun LinkedAccountsScreen(
    viewModel: LinkedAccountsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var pairToUnlink by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.linked_accounts_title),
            heroFigure = { Text(stringResource(R.string.linked_accounts_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.linked_accounts_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    state.isLoading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                    state.accounts.isEmpty() -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    stringResource(R.string.linked_accounts_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        if (state.linkedPairs.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.linked_accounts_existing_links),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(state.linkedPairs) { (idA, idB) ->
                                val nameA = state.accounts.find { it.id == idA }?.name ?: idA
                                val nameB = state.accounts.find { it.id == idB }?.name ?: idB
                                SciuroCard(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("$nameA \u2194 $nameB", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        TextButton(onClick = { pairToUnlink = Pair(idA, idB) }) {
                                            Text(stringResource(R.string.linked_accounts_unlink), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.linked_accounts_select),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(state.accounts) { account ->
                            SciuroCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth().clickable { viewModel.toggleSelection(account.id) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = account.id in state.selectedIds,
                                        onCheckedChange = { viewModel.toggleSelection(account.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(account.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${account.type} \u00B7 ${account.account_number ?: "No account number"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.linkSelectedPair() },
                                enabled = state.canLink,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.selectedIds.size == 2) stringResource(R.string.linked_accounts_link_button) else stringResource(R.string.linked_accounts_select_hint))
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    state.message?.let { message ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text(stringResource(R.string.settings_ok))
                }
            }
        ) {
            Text(message)
        }
    }

    pairToUnlink?.let { (idA, idB) ->
        val nameA = state.accounts.find { it.id == idA }?.name ?: idA
        val nameB = state.accounts.find { it.id == idB }?.name ?: idB
        SciuroConfirmationDialog(
            title = stringResource(R.string.linked_accounts_unlink),
            message = "Unlink $nameA and $nameB?",
            confirmText = stringResource(R.string.linked_accounts_unlink),
            isDestructive = true,
            onConfirm = {
                viewModel.unlinkPair(idA, idB)
                pairToUnlink = null
            },
            onDismiss = { pairToUnlink = null }
        )
    }
}
