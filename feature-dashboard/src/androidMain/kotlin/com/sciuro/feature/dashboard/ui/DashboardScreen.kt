package com.sciuro.feature.dashboard.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import com.sciuro.core.ledger.config.SettingsProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import com.sciuro.feature.dashboard.R
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.FastTransactionSheet
import com.najmi.sciuro.core.ui.components.FastTxOption
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.feature.dashboard.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

import com.najmi.sciuro.core.ui.components.AuditEventDisplay
import com.najmi.sciuro.core.ui.components.TransactionDetailSheet
import com.najmi.sciuro.core.ui.components.formatAuditLogDetail

import com.sciuro.feature.dashboard.ui.components.NetPositionBreakdownPanel
import com.sciuro.feature.dashboard.ui.components.ReviewInboxBanner
import com.sciuro.feature.dashboard.ui.components.AutoBookedBanner
import com.sciuro.feature.dashboard.ui.components.DashboardSummaryRow
import com.sciuro.feature.dashboard.ui.components.AdjustmentBanner
import com.sciuro.feature.dashboard.ui.components.TransactionHistoryHeader
import com.sciuro.feature.dashboard.ui.components.TransactionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settingsProvider: SettingsProvider = koinInject(),viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val autoBookedCount by viewModel.autoBookedTransactionsCount.collectAsState()
    val autoBookedTxs by viewModel.autoBookedTransactions.collectAsState()
    val reviewSuggestions by viewModel.reviewSuggestions.collectAsState()
    var selectedRange by remember { mutableStateOf("All Time") }
    val typeFilter by viewModel.typeFilter.collectAsState()
    val filterOptions = listOf("All", "Income", "Expense")
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val paginatedTransactions by viewModel.paginatedTransactions.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val categoryMap = remember(state.expenseCategories, state.incomeCategories) {
        (state.expenseCategories + state.incomeCategories).associateBy { it.id }
    }

    LaunchedEffect(selectedRange, startDate, endDate) {
        when (selectedRange) {
            "This Month" -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                viewModel.setDateRange(cal.timeInMillis, null)
            }
            "Custom" -> {
                viewModel.setDateRange(startDate, endDate)
            }
            else -> {
                viewModel.setDateRange(null, null)
            }
        }
    }
    
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var newAmount by remember { mutableStateOf("") }
    var newDirection by remember { mutableStateOf("OUTFLOW") }
    var newMerchant by remember { mutableStateOf("") }
    var newAccountId by remember { mutableStateOf<String?>(null) }
    
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

    val msgRejected = stringResource(R.string.dashboard_transaction_rejected)
    val msgApproved = stringResource(R.string.dashboard_transaction_approved)
    val msgSaved = stringResource(R.string.dashboard_transaction_saved)
    val msgUpdated = stringResource(R.string.dashboard_transaction_updated)
    val msgDeleted = stringResource(R.string.dashboard_transaction_deleted)

    val pullToRefreshState = rememberPullToRefreshState()
    val scrollState = rememberLazyListState()

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isPrimaryDark = primaryColor.luminance() < 0.5f

    val isAtHeroPanel by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset < 200
        }
    }

    SideEffect {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            window.statusBarColor = if (isAtHeroPanel) primaryColor.toArgb() else surfaceColor.toArgb()
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isAtHeroPanel || !isPrimaryDark
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier
        .nestedScroll(pullToRefreshState.nestedScrollConnection)
        .fillMaxSize()
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
        ) {
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
                    title = stringResource(R.string.dashboard_total_net_position),
                    heroFigure = { HeroFigure(state.netPosition) },
                    toggleOptions = listOf("This Month", "All Time", "Custom"),
                    selectedToggle = selectedRange,
                    onToggleSelected = { 
                        selectedRange = it 
                        if (it == "Custom") {
                            showDatePickerDialog = true
                        }
                    },
                    chartData = displayChartData,
                    content = {
                        NetPositionBreakdownPanel(
                            breakdown = state.netPositionBreakdown,
                            accountsCount = state.accounts.size,
                            recentAdjustmentCount = state.recentAdjustmentCount
                        )
                        if (state.lastMilestoneReached > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        stringResource(R.string.dashboard_milestone_reached, state.lastMilestoneReached.toInt()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 80.dp)
                    ) {
                        if (state.unreviewedTransactionsCount == 0 && state.activeBudgetsCount == 0 && state.allTransactions.isEmpty()) {
                            com.najmi.sciuro.core.ui.components.EmptyStateView(
                                message = stringResource(R.string.dashboard_empty_state_initial)
                            )
                        } else {
                            if (state.unreviewedTransactionsCount > 0) {
                                ReviewInboxBanner(
                                    unreviewedCount = state.unreviewedTransactionsCount,
                                    suggestions = reviewSuggestions,
                                    onConfirm = { txId, catId, accId ->
                                        viewModel.confirmReviewSuggestion(txId, catId, accId)
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msgApproved) }
                                    },
                                    onReject = { txId ->
                                        viewModel.rejectTransaction(txId)
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msgRejected) }
                                    }
                                )
                            }
                            
                            AutoBookedBanner(
                                autoBookedCount = autoBookedCount,
                                autoBookedTxs = autoBookedTxs,
                                onUndo = { viewModel.undoAutoConfirm(it) }
                            )

                            DashboardSummaryRow(
                                activeBudgetsCount = state.activeBudgetsCount,
                                runway = state.runway,
                                hasIncomePattern = state.hasIncomePattern,
                                expectedIncomeAmount = state.expectedIncomeAmount,
                                expectedIncomeDate = state.expectedIncomeDate
                            )

                            state.weeklyDigest?.let { digest ->
                                SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("This week", style = MaterialTheme.typography.titleSmall)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "RM ${"%.0f".format(digest.totalSpent)} across ${digest.transactionCount} transaction${if (digest.transactionCount != 1) "s" else ""}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (digest.topCategoryName != null) {
                                                Text(
                                                    "Top: ${digest.topCategoryName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (digest.unreviewedCount > 0) {
                                            Text(
                                                "${digest.unreviewedCount} to review",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            AdjustmentBanner(adjustmentCount = state.recentAdjustmentCount)
                            
                            TransactionHistoryHeader(
                                typeFilter = typeFilter,
                                filterOptions = filterOptions,
                                onFilterSelected = { viewModel.setTypeFilter(if (it == "All") null else it) }
                            )

                            if (paginatedTransactions.isEmpty()) {
                                val noMatching = state.allTransactions.isNotEmpty()
                                com.najmi.sciuro.core.ui.components.EmptyStateView(
                                    message = if (noMatching) stringResource(R.string.dashboard_no_matching_transactions) else stringResource(R.string.dashboard_no_transactions)
                                )
                            } else {
                                TransactionList(
                                    transactions = paginatedTransactions,
                                    categoryMap = categoryMap,
                                    onTransactionClick = { tx ->
                                        selectedTxForDetail = tx
                                        showDetailSheet = true
                                    },
                                    onSwipeApprove = { tx ->
                                        pendingApprovalTxId = tx.id
                                        selectedAccountIdForApproval = tx.account_id
                                    },
                                    onSwipeReject = { tx ->
                                        viewModel.rejectTransaction(tx.id)
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msgRejected) }
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }

        if (pullToRefreshState.isRefreshing) {
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            )
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
                .padding(
                    end = 16.dp, 
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dashboard_add_transaction_cd))
        }
    }
    
    if (showDatePickerDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
        com.sciuro.feature.dashboard.ui.components.DashboardDatePicker(
            dateRangePickerState = dateRangePickerState,
            onDismiss = {
                showDatePickerDialog = false
                if (startDate == null && endDate == null) {
                    selectedRange = "All Time"
                }
            },
            onConfirm = { start, end ->
                viewModel.setDateRange(start, end)
                showDatePickerDialog = false
            }
        )
    }
    
    if (pendingApprovalTxId != null) {
        com.sciuro.feature.dashboard.ui.components.ApproveTransactionDialog(
            accounts = state.accounts,
            selectedAccountId = selectedAccountIdForApproval,
            onAccountSelected = { selectedAccountIdForApproval = it },
            onApprove = {
                viewModel.approveTransaction(pendingApprovalTxId!!, selectedAccountIdForApproval)
                pendingApprovalTxId = null
                coroutineScope.launch { snackbarHostState.showSnackbar(msgApproved) }
            },
            onDismiss = { pendingApprovalTxId = null }
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
                    snackbarHostState.showSnackbar(msgSaved)
                }
            }
        )
    }

    if (showDetailSheet && selectedTxForDetail != null) {
        val tx = selectedTxForDetail!!
        val isTransfer = detailData?.transferLink != null || tx.category_id == "cat_transfer"
        val rawEvent = detailData?.rawEvent
        val auditLogs = detailData?.auditLogs ?: emptyList()
        val formattedTimestamp = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(tx.timestamp))
        val categoryNames = categoryMap.mapValues { it.value.name }
        val auditEvents = auditLogs.map { log ->
            val actionLabel = when (log.action.name) {
                "CREATE" -> stringResource(R.string.dashboard_action_created)
                "UPDATE" -> stringResource(R.string.dashboard_action_edited)
                "RECLASSIFY" -> stringResource(R.string.dashboard_action_recategorized)
                "DELETE" -> stringResource(R.string.dashboard_action_deleted)
                else -> log.action.name
            }
            val sourceLabel = when (log.source.name) {
                "SYSTEM_AUTO" -> stringResource(R.string.dashboard_source_auto)
                "USER_MANUAL" -> stringResource(R.string.dashboard_source_you)
                "LLM_INFERRED" -> stringResource(R.string.dashboard_source_ai)
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
            merchantName = tx.merchant ?: stringResource(R.string.dashboard_unknown_merchant),
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
        com.sciuro.feature.dashboard.ui.components.EditTransactionSheet(
            amount = editTxAmount,
            onAmountChange = { editTxAmount = it },
            merchant = editTxMerchant,
            onMerchantChange = { editTxMerchant = it },
            direction = editTxDirection,
            onDirectionChange = { editTxDirection = it },
            categoryId = editTxCategoryId,
            onCategoryIdChange = { editTxCategoryId = it },
            accountId = editTxAccountId,
            onAccountIdChange = { editTxAccountId = it },
            accounts = state.accounts,
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            onSave = {
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
                coroutineScope.launch { snackbarHostState.showSnackbar(msgUpdated) }
            },
            onDelete = { showDeleteConfirmation = true },
            onDismiss = { showEditTransactionDialog = false }
        )
    }

    if (showDeleteConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.dashboard_delete_transaction_title),
            message = stringResource(R.string.dashboard_delete_transaction_message),
            confirmText = stringResource(R.string.dashboard_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteTransaction(editingTxId!!)
                showDeleteConfirmation = false
                showEditTransactionDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msgDeleted)
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}


