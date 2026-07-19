package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.saveable.rememberSaveable
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun WalletScreen(viewModel: WalletViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    
    var selectedAssetType by rememberSaveable { mutableStateOf("Liquid Cash") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Dialog Form State
    var newAccountName by rememberSaveable { mutableStateOf("") }
    var newAccountType by rememberSaveable { mutableStateOf("Bank Account") }
    var newAccountPackage by rememberSaveable { mutableStateOf("") }
    var newAccountBalance by rememberSaveable { mutableStateOf("") }
    
    // In a real app, calculate actual totals for invested as well
    val totalLiquidity = accounts.sumOf { it.balance }
    val displayTotal = if (selectedAssetType == "Liquid Cash") totalLiquidity else 0.0 // Mock investment
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeroPanel(
                title = "Total $selectedAssetType",
                heroFigure = "RM ${"%.2f".format(displayTotal)}",
                toggleOptions = listOf("Liquid Cash", "Investments"),
                selectedToggle = selectedAssetType,
                onToggleSelected = { selectedAssetType = it }
            )
        }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedAssetType == "Liquid Cash") {
                        if (accounts.isEmpty()) {
                            com.najmi.sciuro.core.ui.components.EmptyStateView(
                                message = "No cash tracked yet. Withdraw from an ATM and it'll show up here automatically."
                            )
                        } else {
                            accounts.forEach { account ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        editingAccountId = account.id
                                        newAccountName = account.name
                                        newAccountType = if (account.isEWallet) "E-Wallet" else "Bank Account"
                                        newAccountPackage = account.associatedPackage ?: ""
                                        newAccountBalance = account.balance.toString()
                                        showAddDialog = true
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(account.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                if (account.isEWallet) "E-Wallet" else "Bank Account", 
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "RM ${"%.2f".format(account.balance)}", 
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = com.najmi.sciuro.core.ui.theme.IBMPlexMono
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Investments empty state
                        com.najmi.sciuro.core.ui.components.EmptyStateView(
                            message = "No investments tracked yet."
                        )
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { 
                editingAccountId = null
                newAccountName = ""
                newAccountType = "Bank Account"
                newAccountPackage = ""
                newAccountBalance = ""
                showAddDialog = true 
            },
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Account")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingAccountId == null) "Add Account" else "Edit Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("Account Name (e.g. Maybank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAccountPackage,
                        onValueChange = { newAccountPackage = it },
                        label = { Text("Associated App Package (Optional)") },
                        placeholder = { Text("com.maybank2u.life") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAccountBalance,
                        onValueChange = { newAccountBalance = it },
                        label = { Text("Initial Balance (RM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Simple toggle for Bank vs E-Wallet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = newAccountType == "Bank Account",
                            onClick = { newAccountType = "Bank Account" },
                            label = { Text("Bank") }
                        )
                        FilterChip(
                            selected = newAccountType == "E-Wallet",
                            onClick = { newAccountType = "E-Wallet" },
                            label = { Text("E-Wallet") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val balance = newAccountBalance.toDoubleOrNull() ?: 0.0
                        if (editingAccountId == null) {
                            viewModel.addAccount(
                                name = newAccountName,
                                type = newAccountType,
                                associatedPackage = newAccountPackage,
                                initialBalance = balance
                            )
                        } else {
                            viewModel.updateAccount(
                                id = editingAccountId!!,
                                name = newAccountName,
                                type = newAccountType,
                                associatedPackage = newAccountPackage,
                                balance = balance
                            )
                        }
                        showAddDialog = false
                    },
                    enabled = newAccountName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (editingAccountId != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAccount(editingAccountId!!)
                                showAddDialog = false
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

