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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import com.sciuro.core.parsing.config.SettingsProvider
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
import com.najmi.sciuro.core.ui.components.AuditEventDisplay
import com.najmi.sciuro.core.ui.components.TransactionCard
import com.najmi.sciuro.core.ui.components.TransactionDetailSheet
import com.najmi.sciuro.core.ui.components.formatAuditLogDetail
import com.najmi.sciuro.core.ui.theme.IBMPlexMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settingsProvider: SettingsProvider = koinInject(),viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedRange by remember { mutableStateOf("All Time") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Income", "Expense")

    val categoryMap = remember(state.expenseCategories, state.incomeCategories) {
        (state.expenseCategories + state.incomeCategories).associateBy { it.id }
    }

    val filteredTransactions = remember(state.allTransactions, selectedRange, selectedTypeFilter) {
        state.allTransactions.filter { tx ->
            val matchesTime = when (selectedRange) {
                "This Month" -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    tx.timestamp >= cal.timeInMillis
                }
                else -> true
            }
            val matchesType = when (selectedTypeFilter) {
                "Income" -> tx.direction == "INFLOW"
                "Expense" -> tx.direction == "OUTFLOW"
                else -> true
            }
            matchesTime && matchesType
        }
    }
    
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var newAmount by remember { mutableStateOf("") }
    var newDirection by remember { mutableStateOf("OUTFLOW") }
    var newMerchant by remember { mutableStateOf("") }
    var newAccountId by remember { mutableStateOf<String?>(null) }
    var newCategoryId by remember { mutableStateOf<String?>(null) }
    
    var pendingApprovalTxId by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdForApproval by remember { mutableStateOf<String?>(null) }
    
    // Detail Sheet State
    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<com.sciuro.core.ledger.db.Transaction_record?>(null) }
    var detailData by remember { mutableStateOf<com.sciuro.feature.dashboard.viewmodel.TransactionDetailData?>(null) }

    LaunchedEffect(showDetailSheet, selectedTxForDetail) {
        if (showDetailSheet && selectedTxForDetail != null) {
            detailData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                viewModel.loadTransactionDetail(selectedTxForDetail!!)
            }
        } else {
            detailData = null
        }
    }

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
                val displayChartData = remember(state.balanceHistory, selectedRange) {
                    if (state.balanceHistory.isEmpty()) {
                        emptyList()
                    } else {
                        when (selectedRange) {
                            "This Month" -> state.balanceHistory.takeLast(30)
                            else -> state.balanceHistory
                        }
                    }
                }

                HeroPanel(
                    title = "Total Net Position",
                    heroFigure = "RM ${"%.2f".format(state.netPosition)}",
                    toggleOptions = listOf("This Month", "All Time"),
                    selectedToggle = selectedRange,
                    onToggleSelected = { selectedRange = it },
                    chartData = displayChartData,
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${state.accounts.size} accounts tracked",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            if (state.recentAdjustmentCount > 0) {
                                Text(
                                    text = "${state.recentAdjustmentCount} adjustments this week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
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

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Active Budgets", style = MaterialTheme.typography.titleSmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${state.activeBudgetsCount}", style = MaterialTheme.typography.headlineSmall)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Runway",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "RM ${"%.0f".format(state.runway)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (state.runway < 0) com.najmi.sciuro.core.ui.theme.SignalDanger else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!state.hasIncomePattern) {
                                            Text(
                                                "based on bills only",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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

                            LazyRow(
                                modifier = Modifier.padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filterOptions) { filter ->
                                    FilterChip(
                                        selected = selectedTypeFilter == filter,
                                        onClick = { selectedTypeFilter = filter },
                                        label = { Text(filter) }
                                    )
                                }
                            }

                            if (filteredTransactions.isEmpty()) {
                                val noMatching = state.allTransactions.isNotEmpty()
                                com.najmi.sciuro.core.ui.components.EmptyStateView(
                                    message = if (noMatching) "No transactions match the current filter." else "No transactions yet."
                                )
                            } else {
                                filteredTransactions.forEach { tx ->
                                    val cat = categoryMap[tx.category_id]
                                    val catColor = cat?.color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant
                                    val catIcon = mapCategoryIcon(tx.category_id)
                                    val isTransfer = tx.category_id == "cat_transfer"
                                    val statusText = if (tx.is_reviewed == 1L) "Reviewed" else "Swipe right to approve, left to reject"

                                    val cardContent = @Composable {
                                        TransactionCard(
                                            merchantName = tx.merchant ?: "Unknown Merchant",
                                            amount = "RM ${"%.2f".format(tx.amount)}",
                                            direction = tx.direction,
                                            statusText = statusText,
                                            categoryIcon = catIcon,
                                            categoryColor = catColor,
                                            isTransfer = isTransfer,
                                            confidence = tx.confidence,
                                            extractionMethod = tx.extraction_method,
                                            onClick = {
                                                selectedTxForDetail = tx
                                                showDetailSheet = true
                                            }
                                        )
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
                                                    SwipeToDismissBoxValue.StartToEnd -> com.najmi.sciuro.core.ui.theme.SignalIncome
                                                    SwipeToDismissBoxValue.EndToStart -> com.najmi.sciuro.core.ui.theme.SignalDanger
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
                                            cardContent()
                                        }
                                    } else {
                                        cardContent()
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
            presetLabels = settingsProvider.getQuickLabels(),
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

    if (showDetailSheet && selectedTxForDetail != null) {
        val tx = selectedTxForDetail!!
        val cat = categoryMap[tx.category_id]
        val isTransfer = detailData?.transferLink != null || tx.category_id == "cat_transfer"
        val rawEvent = detailData?.rawEvent
        val auditLogs = detailData?.auditLogs ?: emptyList()
        val formattedTimestamp = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(tx.timestamp))
        val categoryNames = categoryMap.mapValues { it.value.name }
        val auditEvents = auditLogs.map { log ->
            val actionLabel = when (log.action.name) {
                "CREATE" -> "Created"
                "UPDATE" -> "Edited"
                "RECLASSIFY" -> "Recategorized"
                "DELETE" -> "Deleted"
                else -> log.action.name
            }
            val sourceLabel = when (log.source.name) {
                "SYSTEM_AUTO" -> "auto"
                "USER_MANUAL" -> "you"
                "LLM_INFERRED" -> "AI"
                else -> log.source.name
            }
            val detail = formatAuditLogDetail(
                action = log.action.name,
                source = log.source.name,
                beforeState = log.beforeState,
                afterState = log.afterState,
                categoryNames = categoryNames
            )
            AuditEventDisplay(
                label = "$actionLabel ($sourceLabel)",
                detail = detail,
                isCurrent = false
            )
        }

        TransactionDetailSheet(
            showSheet = showDetailSheet,
            onDismiss = { showDetailSheet = false },
            merchantName = tx.merchant ?: "Unknown Merchant",
            amount = "RM ${"%.2f".format(tx.amount)}",
            direction = tx.direction,
            timestamp = formattedTimestamp,
            extractionMethod = tx.extraction_method,
            confidence = tx.confidence,
            rawEventTitle = rawEvent?.title,
            rawEventText = rawEvent?.text,
            hasTransferLink = isTransfer,
            auditEvents = auditEvents,
            onEditClick = {
                showDetailSheet = false
                editingTxId = tx.id
                editTxAmount = tx.amount.toString()
                editTxMerchant = tx.merchant ?: ""
                editTxAccountId = tx.account_id
                editTxDirection = tx.direction
                editTxCategoryId = tx.category_id
                showEditTransactionDialog = true
            },
            onDeleteClick = {
                showDetailSheet = false
                editingTxId = tx.id
                showDeleteConfirmation = true
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

private fun parseColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}

private fun mapCategoryIcon(categoryId: String?): ImageVector? {
    return when (categoryId) {
        "cat_dining", "cat_exp_1" -> Icons.Filled.Restaurant
        "cat_groceries", "cat_exp_6" -> Icons.Filled.LocalGroceryStore
        "cat_transport", "cat_exp_2" -> Icons.Filled.DirectionsCar
        "cat_utilities", "cat_exp_3" -> Icons.Filled.Home
        "cat_exp_4" -> Icons.Filled.ShoppingCart
        "cat_exp_5" -> Icons.Filled.Description
        "cat_exp_7" -> Icons.Filled.LocalHospital
        "cat_exp_8" -> Icons.Filled.School
        "cat_exp_9", "cat_inc_6" -> Icons.Filled.MoreHoriz
        "cat_inc_1" -> Icons.Filled.AccountBalance
        "cat_inc_2" -> Icons.Filled.Computer
        "cat_inc_3" -> Icons.Filled.CardGiftcard
        "cat_inc_4" -> Icons.Filled.TrendingUp
        "cat_inc_5" -> Icons.Filled.Refresh
        "cat_transfer" -> Icons.Filled.SwapHoriz
        else -> null
    }
}




