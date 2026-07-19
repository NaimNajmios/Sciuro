package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.wallet.viewmodel.AccountDetailViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Automatically go back if account is deleted and state updates to null
    LaunchedEffect(state.account) {
        // We only go back if it was loaded once and then became null
        // However, initial state might also be null before load. 
        // A safer way is checking if it's null after a short delay or keeping a flag.
        // For simplicity, we just rely on the delete button doing the pop.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                viewModel.deleteAccount()
                                onNavigateBack()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (state.account == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        val account = state.account!!
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                HeroPanel(
                    title = account.name,
                    heroFigure = "RM ${"%.2f".format(account.balance)}",
                    toggleOptions = emptyList(),
                    selectedToggle = "",
                    onToggleSelected = { }
                )
            }
            
            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            "Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        if (state.transactions.isEmpty()) {
                            com.najmi.sciuro.core.ui.components.EmptyStateView(
                                message = "No transactions found for this account."
                            )
                        } else {
                            state.transactions.forEach { tx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (tx.direction == "INFLOW") Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else Color(0xFFE53935)
                                            )
                                            Column {
                                                Text(
                                                    tx.merchant ?: "Unknown Merchant",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (tx.is_reviewed == 1L) Color.Gray else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Text(
                                            "RM ${"%.2f".format(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
