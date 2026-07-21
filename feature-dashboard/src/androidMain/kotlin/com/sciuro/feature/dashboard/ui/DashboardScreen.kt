package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.FastTransactionSheet
import com.najmi.sciuro.core.ui.components.FastTxOption
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.sciuro.feature.dashboard.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.theme.IBMPlexMono

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
    var newCategoryId by remember { mutableStateOf<String?>(null) }
    
    var pendingApprovalTxId by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdForApproval by remember { mutableStateOf<String?>(null) }
    
    // Edit Transaction State
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var editingTxId by remember { mutableStateOf<String?>(null) }
    var editTxAmount by remember { mutableStateOf("") }
    var editTxMerchant by remember { mutableStateOf("") }
    var editTxAccountId by remember { mutableStateOf<String?>(null) }
    var editTxDirection by remember { mutableStateOf("OUTFLOW") }
    var editTxCategoryId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
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

                            if (state.recentAdjustmentCount > 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Balance Adjustments",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                "${state.recentAdjustmentCount} this week",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                        Text(
                                            if (state.recentAdjustmentCount == 1) "View in Wallet" else "View in Wallet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                "Transaction History",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            if (state.allTransactions.isEmpty()) {
                                com.najmi.sciuro.core.ui.components.EmptyStateView(
                                    message = "No transactions yet."
                                )
                            } else {
                                state.allTransactions.forEach { tx ->
                                    @Composable
                                    fun TransactionCard() {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                                                editingTxId = tx.id
                                                editTxAmount = tx.amount.toString()
                                                editTxMerchant = tx.merchant ?: ""
                                                editTxAccountId = tx.account_id
                                                editTxDirection = tx.direction
                                                editTxCategoryId = tx.category_id
                                                showEditTransactionDialog = true
                                            },
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
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            if (tx.is_reviewed == 1L) "Reviewed" else "Swipe right to approve, left to reject",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (tx.is_reviewed == 1L) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error
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
                                                        coroutineScope.launch { snackbarHostState.showSnackbar("Transaction rejected") }
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
                        coroutineScope.launch { snackbarHostState.showSnackbar("Transaction approved") }
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
        val accountOptions = state.accounts.map { FastTxOption(it.id, it.name) }
        val expCatOptions = state.expenseCategories.map { FastTxOption(it.id, it.name) }
        val incCatOptions = state.incomeCategories.map { FastTxOption(it.id, it.name) }
        
        FastTransactionSheet(
            accounts = accountOptions,
            expenseCategories = expCatOptions,
            incomeCategories = incCatOptions,
            onDismissRequest = { showAddTransactionDialog = false },
            onSubmit = { amount, direction, merchant, categoryId, accountId, destinationAccountId ->
                viewModel.bookManualTransaction(
                    amount = amount,
                    direction = direction,
                    merchant = merchant,
                    accountId = accountId,
                    categoryId = categoryId ?: (if (direction == "OUTFLOW") "cat_exp_9" else "cat_inc_6"),
                    destinationAccountId = destinationAccountId
                )
                showAddTransactionDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Transaction saved successfully")
                }
            }
        )
    }

    if (showEditTransactionDialog) {
        SciuroBottomSheet(
            onDismissRequest = { showEditTransactionDialog = false }
        ) {
            Text("Edit Transaction", style = MaterialTheme.typography.headlineSmall)
            
            SciuroTextField(
                value = editTxAmount,
                onValueChange = { editTxAmount = it },
                label = "Amount (RM)",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
            
            SciuroTextField(
                value = editTxMerchant,
                onValueChange = { editTxMerchant = it },
                label = "Merchant / Note"
            )
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = editTxDirection == "OUTFLOW",
                    onClick = { editTxDirection = "OUTFLOW"; editTxCategoryId = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Expense")
                }
                SegmentedButton(
                    selected = editTxDirection == "INFLOW",
                    onClick = { editTxDirection = "INFLOW"; editTxCategoryId = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Income")
                }
            }
                
                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val selAcc = state.accounts.find { it.id == editTxAccountId }
                    SciuroTextField(
                        value = selAcc?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = "Wallet Account",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        state.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    editTxAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val relevantCategories = if (editTxDirection == "OUTFLOW") state.expenseCategories else state.incomeCategories
                    items(relevantCategories) { cat ->
                        FilterChip(
                            selected = editTxCategoryId == cat.id,
                            onClick = { editTxCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmation = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    
                    SciuroPrimaryButton(
                        text = "Save",
                        onClick = {
                            val amt = editTxAmount.toDoubleOrNull() ?: 0.0
                            viewModel.editTransaction(
                                transactionId = editingTxId!!,
                                amount = amt,
                                direction = editTxDirection,
                                merchant = editTxMerchant,
                                categoryId = editTxCategoryId,
                                accountId = editTxAccountId
                            )
                            showEditTransactionDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Transaction updated")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editTxAmount.isNotBlank() && editTxAccountId != null
                    )
                }
        }
    }

    if (showDeleteConfirmation) {
        SciuroConfirmationDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this transaction? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteTransaction(editingTxId!!)
                showDeleteConfirmation = false
                showEditTransactionDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Transaction deleted")
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

