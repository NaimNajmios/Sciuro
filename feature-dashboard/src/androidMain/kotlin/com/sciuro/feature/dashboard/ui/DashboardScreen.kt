package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.dashboard.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedRange by remember { mutableStateOf("All Time") }
    
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var newAmount by remember { mutableStateOf("") }
    var newDirection by remember { mutableStateOf("OUTFLOW") }
    var newMerchant by remember { mutableStateOf("") }
    var newAccountId by remember { mutableStateOf<String?>(null) }
    
    var pendingApprovalTxId by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdForApproval by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                HeroPanel(
                    title = "Total Net Worth",
                    heroFigure = "RM ${"%.2f".format(state.netWorth)}",
                    toggleOptions = listOf("This Month", "All Time"),
                    selectedToggle = selectedRange,
                    onToggleSelected = { selectedRange = it },
                    chartData = listOf(100f, 150f, 130f, 180f, 200f) // Mock chart data for now
                )
            }
            
            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content inside the sheet
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 80.dp) // Space for FAB
                    ) {
                        if (state.unreviewedTransactionsCount == 0 && state.activeBudgetsCount == 0 && state.allTransactions.isEmpty()) {
                            com.najmi.sciuro.core.ui.components.EmptyStateView(
                                message = "Nothing gathered yet — once your bank notifications start coming in or you add a manual entry, this is where they'll show up."
                            )
                        } else {
                            if (state.unreviewedTransactionsCount > 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Column {
                                            Text(
                                                "Review Inbox",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                "${state.unreviewedTransactionsCount} items pending your review",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Active Budgets", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "${state.activeBudgetsCount} active this month",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                            
                            Text(
                                "Transaction History",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            if (state.allTransactions.isEmpty()) {
                                Text("No transactions found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            } else {
                                state.allTransactions.forEach { tx ->
                                    @Composable
                                    fun TransactionCard() {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
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
                                                            if (tx.is_reviewed == 1L) "Reviewed" else "Swipe right to approve, left to reject",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (tx.is_reviewed == 1L) Color.Gray else MaterialTheme.colorScheme.primary
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

                                    if (tx.is_reviewed == 0L) {
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = {
                                                when(it) {
                                                    SwipeToDismissBoxValue.StartToEnd -> {
                                                        pendingApprovalTxId = tx.id
                                                        selectedAccountIdForApproval = tx.account_id
                                                        false
                                                    }
                                                    SwipeToDismissBoxValue.EndToStart -> {
                                                        viewModel.rejectTransaction(tx.id)
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            }
                                        )
                                        SwipeToDismissBox(
                                            state = dismissState,
                                            backgroundContent = {
                                                val color = when (dismissState.targetValue) {
                                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                                                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                                                    else -> Color.Transparent
                                                }
                                                val icon = when (dismissState.targetValue) {
                                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Check
                                                    SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                                                    else -> null
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(bottom = 8.dp)
                                                        .clip(CardDefaults.shape)
                                                        .background(color),
                                                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                                ) {
                                                    if (icon != null) {
                                                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                                                    }
                                                }
                                            }
                                        ) {
                                            TransactionCard()
                                        }
                                    } else {
                                        TransactionCard()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = {
                newAmount = ""
                newMerchant = ""
                newDirection = "OUTFLOW"
                newAccountId = null
                showAddTransactionDialog = true 
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
        }
    }
    
    if (pendingApprovalTxId != null) {
        AlertDialog(
            onDismissRequest = { pendingApprovalTxId = null },
            title = { Text("Approve Transaction") },
            text = {
                var accountExpanded by remember { mutableStateOf(false) }
                Column {
                    Text("Select an account for this transaction:")
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it }
                    ) {
                        val selAcc = state.accounts.find { it.id == selectedAccountIdForApproval }
                        OutlinedTextField(
                            value = selAcc?.name ?: "Select Account",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            state.accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedAccountIdForApproval = acc.id
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveTransaction(pendingApprovalTxId!!, selectedAccountIdForApproval)
                        pendingApprovalTxId = null
                    },
                    enabled = selectedAccountIdForApproval != null
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApprovalTxId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showAddTransactionDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddTransactionDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Manual Entry",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = newDirection == "OUTFLOW",
                            onClick = { newDirection = "OUTFLOW" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Expense")
                        }
                        SegmentedButton(
                            selected = newDirection == "INFLOW",
                            onClick = { newDirection = "INFLOW" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Income")
                        }
                    }
                }

                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it },
                    label = { Text("Amount (RM)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = newMerchant,
                    onValueChange = { newMerchant = it },
                    label = { Text("Description / Merchant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    val selectedAccount = state.accounts.find { it.id == newAccountId }
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                newAccountId = null
                                accountExpanded = false
                            }
                        )
                        state.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    newAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        val amount = newAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && newMerchant.isNotBlank()) {
                            viewModel.bookManualTransaction(
                                amount = amount,
                                direction = newDirection,
                                merchant = newMerchant,
                                accountId = newAccountId,
                                categoryId = null // We'll leave category mapping out of manual entry for now or add it later
                            )
                            showAddTransactionDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newAmount.isNotBlank() && newMerchant.isNotBlank()
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }
}

