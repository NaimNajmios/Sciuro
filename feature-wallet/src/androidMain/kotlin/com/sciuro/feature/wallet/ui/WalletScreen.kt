package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun WalletScreen(viewModel: WalletViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    
    var selectedAssetType by remember { mutableStateOf("Liquid Cash") }
    
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
                                    modifier = Modifier.fillMaxWidth(),
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
}

