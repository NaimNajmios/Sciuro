package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.feature.wallet.viewmodel.WalletViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun WalletScreen(viewModel: WalletViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    val totalLiquidity = accounts.sumOf { it.balance }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Wallets", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Liquid Cash", style = MaterialTheme.typography.titleMedium)
                Text("RM $totalLiquidity", style = MaterialTheme.typography.displaySmall)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Accounts & E-Wallets", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { account ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(account.name, style = MaterialTheme.typography.titleMedium)
                            if (account.isEWallet) {
                                Text("E-Wallet", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Bank Account", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("RM ${account.balance}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
