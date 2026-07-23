package com.sciuro.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.feature.settings.viewmodel.LinkedAccountsUiState
import com.sciuro.feature.settings.viewmodel.LinkedAccountsViewModel

@Composable
fun LinkedAccountsScreen(
    viewModel: LinkedAccountsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is LinkedAccountsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is LinkedAccountsUiState.Empty -> {
                Text(
                    "No accounts found. Create an account first.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is LinkedAccountsUiState.Ready -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select two accounts to link:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(s.accounts) { account ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (account.id in s.selectedIds)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth().clickable { viewModel.toggleSelection(account.id) }) {
                                    Text(
                                        account.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "${account.type} · ${account.account_number ?: "No account number"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.linkSelectedPair() },
                            enabled = s.canLink,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (s.selectedIds.size == 2) "Link Selected Pair" else "Select two accounts")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            is LinkedAccountsUiState.Linked -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAccounts() }) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
