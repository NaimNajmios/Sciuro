package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.feature.kanban.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.theme.reducedMotion
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.sciuro.core.ledger.model.Account
import com.sciuro.feature.kanban.model.BillStatus
import com.sciuro.feature.kanban.model.BillTask
import com.sciuro.feature.kanban.model.DebtTask
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import com.sciuro.feature.kanban.viewmodel.KanbanViewModel
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtType
import com.sciuro.core.obligations.model.ObligationFrequency
import com.sciuro.core.ledger.model.Category
import com.sciuro.feature.kanban.ui.components.KanbanDialogs
import com.sciuro.feature.kanban.ui.components.BillDetailSheet
import com.sciuro.feature.kanban.ui.components.DebtDetailSheet
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val debtTasks by viewModel.debtTasks.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val showCompletedDebts by viewModel.showCompletedDebts.collectAsState()

    var selectedTab by remember { mutableStateOf("Review") }
    val tabs = listOf("Review", "Bills", "Debts")

    var selectedStatus by remember { mutableStateOf("To Do") }
    val currentStatusFilter = when (selectedStatus) {
        "To Do" -> TaskStatus.TODO
        "In Progress" -> TaskStatus.IN_PROGRESS
        "Done" -> TaskStatus.DONE
        else -> TaskStatus.TODO
    }
    val filteredTasks = tasks.filter { it.status == currentStatusFilter }
    val todoCount = remember(tasks) { tasks.count { it.status == TaskStatus.TODO } }
    val inProgressCount = remember(tasks) { tasks.count { it.status == TaskStatus.IN_PROGRESS } }
    val doneCount = remember(tasks) { tasks.count { it.status == TaskStatus.DONE } }

    var taskToReject by remember { mutableStateOf<KanbanTask?>(null) }

    var paymentBill by remember { mutableStateOf<BillTask?>(null) }
    var paymentDebt by remember { mutableStateOf<DebtTask?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }


    var showAddSheet by remember { mutableStateOf(false) }

    // Confirmation dialog states
    var taskToApprove by remember { mutableStateOf<Triple<KanbanTask, String?, String>?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtTask?>(null) }
    var debtToEdit by remember { mutableStateOf<DebtTask?>(null) }
    var billToDelete by remember { mutableStateOf<BillTask?>(null) }
    var billToEdit by remember { mutableStateOf<BillTask?>(null) }
    var billToView by remember { mutableStateOf<BillTask?>(null) }
    var debtToView by remember { mutableStateOf<DebtTask?>(null) }
    var createBillAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var createDebtAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val recentlySettledIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.animationTriggers.collect { cardId ->
            recentlySettledIds.add(cardId)
            coroutineScope.launch {
                delay(1500)
                recentlySettledIds.remove(cardId)
            }
        }
    }

    var autoMarkFinishedDebtId by remember { mutableStateOf<String?>(null) }
    
    val pullToRefreshState = rememberPullToRefreshState()

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
        ) {
            item {
                val billOverdue = remember(bills) { bills.count { it.status == BillStatus.OVERDUE } }
                val billDueSoon = remember(bills) { bills.count { it.status == BillStatus.DUE_SOON } }
                val activeDebtCount = remember(debtTasks) { debtTasks.size }

                HeroPanel(
                    title = when (selectedTab) {
                        "Bills" -> stringResource(R.string.kanban_hero_title_bills)
                        "Debts" -> stringResource(R.string.kanban_hero_title_debts)
                        else -> stringResource(R.string.kanban_hero_title_tasks)
                    },
                    heroFigure = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    "Bills" -> "${billOverdue + billDueSoon}"
                                    "Debts" -> "$activeDebtCount"
                                    else -> "${todoCount + inProgressCount}"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = when (selectedTab) {
                                    "Bills" -> stringResource(R.string.kanban_hero_figure_due)
                                    "Debts" -> stringResource(R.string.kanban_hero_figure_active)
                                    else -> stringResource(R.string.kanban_hero_figure_total)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    toggleOptions = tabs,
                    selectedToggle = selectedTab,
                    onToggleSelected = { selectedTab = it },
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            when (selectedTab) {
                                "Bills" -> {
                                    HeroMetric(label = stringResource(R.string.kanban_metric_overdue), value = billOverdue.toString(), color = if (billOverdue > 0) com.najmi.sciuro.core.ui.theme.SignalDanger else Color.White.copy(alpha = 0.7f))
                                    HeroMetric(label = stringResource(R.string.kanban_metric_due_soon), value = billDueSoon.toString(), color = if (billDueSoon > 0) com.najmi.sciuro.core.ui.theme.SignalWarning else Color.White.copy(alpha = 0.7f))
                                }
                                "Debts" -> {
                                    val totalOwe = remember(debtTasks) { debtTasks.filter { it.direction == com.sciuro.core.debt.model.DebtDirection.I_OWE }.sumOf { it.remainingBalance } }
                                    val totalOwed = remember(debtTasks) { debtTasks.filter { it.direction == com.sciuro.core.debt.model.DebtDirection.OWED_TO_ME }.sumOf { it.remainingBalance } }
                                    HeroMetric(label = "I Owe", value = "RM ${"%.0f".format(totalOwe)}", color = Color.White.copy(alpha = 0.9f))
                                    HeroMetric(label = "Owed To Me", value = "RM ${"%.0f".format(totalOwed)}", color = Color.White.copy(alpha = 0.9f))
                                }
                                else -> {
                                    HeroMetric(label = "To Do", value = todoCount.toString(), color = Color.White.copy(alpha = 0.9f))
                                    HeroMetric(label = "In Progress", value = inProgressCount.toString(), color = if (inProgressCount > 0) com.najmi.sciuro.core.ui.theme.SignalWarning else Color.White.copy(alpha = 0.7f))
                                    HeroMetric(label = "Done", value = doneCount.toString(), color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                )
            }

            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))



                    when (selectedTab) {
                        "Bills" -> BillsColumn(
                            bills = bills,
                            recentlySettledIds = recentlySettledIds,
                            onMarkPaid = { paymentBill = it },
                            onClickBill = { billToView = it }
                        )
                        "Debts" ->   DebtsColumn(
                            debtTasks = debtTasks,
                            showCompletedDebts = showCompletedDebts,
                            onToggleCompleted =     { viewModel.toggleShowCompletedDebts() },
                            recentlySettledIds = recentlySettledIds,
                            onRecordPayment = { paymentDebt = it },
                            onClickDebt = { debtToView = it }
                        )
                        else -> ReviewColumn(
                            tasks = filteredTasks,
                            accounts = accounts,
                            selectedStatus = selectedStatus,
                            onStatusChange = { selectedStatus = it },
                            onReject = { taskToReject = it },
                            onApprove = { task, accountId, direction ->
                                taskToApprove = Triple(task, accountId, direction)
                            }
                        )
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

        if (selectedTab != "Review") {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp, 
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp
                    ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.kanban_add_tab_cd, selectedTab))
            }
        }
    }

    val snackbarTaskRejected = stringResource(R.string.kanban_snackbar_task_rejected)
    val snackbarDebtDeleted = stringResource(R.string.kanban_snackbar_debt_deleted)
    val snackbarDebtUpdated = stringResource(R.string.kanban_snackbar_debt_updated)
    val snackbarTaskApproved = stringResource(R.string.kanban_snackbar_task_approved)
    val snackbarBillMarkedPaid = stringResource(R.string.kanban_snackbar_bill_marked_paid)
    val snackbarPaymentRecorded = stringResource(R.string.kanban_snackbar_payment_recorded)
    val snackbarDebtMarkedFinished = stringResource(R.string.kanban_snackbar_debt_marked_finished)
    val snackbarBillCreated = stringResource(R.string.kanban_snackbar_bill_created)
    val snackbarDebtCreated = stringResource(R.string.kanban_snackbar_debt_created)
    val snackbarBillDeleted = "Bill deleted"
    val snackbarBillUpdated = "Bill updated"

    KanbanDialogs(
        taskToReject = taskToReject,
        onRejectConfirmed = { task ->
            viewModel.updateTaskStatus(task.id, TaskStatus.REJECTED, null)
            taskToReject = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarTaskRejected) }
        },
        onRejectDismiss = { taskToReject = null },
        debtToDelete = debtToDelete,
        onDeleteDebtConfirmed = { debt ->
            viewModel.deleteDebt(debt.id)
            debtToDelete = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarDebtDeleted) }
        },
        onDeleteDebtDismiss = { debtToDelete = null },
        debtToEdit = debtToEdit,
        onEditDebtConfirmed = { debt, name, principalAmount, notes ->
            viewModel.updateDebt(debt.id, name, principalAmount, notes)
            debtToEdit = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarDebtUpdated) }
        },
        onEditDebtDismiss = { debtToEdit = null },
        taskToApprove = taskToApprove,
        onApproveConfirmed = { task, accountId, direction ->
            viewModel.updateTaskStatus(task.id, TaskStatus.DONE, accountId, direction)
            taskToApprove = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarTaskApproved) }
        },
        onApproveDismiss = { taskToApprove = null },
        paymentBill = paymentBill,
        onBillPaid = { bill ->
            viewModel.markBillAsPaid(bill.obligation)
            paymentBill = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarBillMarkedPaid) }
        },
        onBillPaymentDismiss = { paymentBill = null },
        paymentDebt = paymentDebt,
        paymentAmountText = paymentAmountText,
        onPaymentAmountChange = { paymentAmountText = it },
        onDebtPaymentApply = { debt, amt ->
            viewModel.recordDebtPayment(debt.id, amt)
            paymentDebt = null
            paymentAmountText = ""
            if (debt.remainingBalance - amt <= 0) {
                autoMarkFinishedDebtId = debt.id
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarPaymentRecorded) }
            }
        },
        onDebtPaymentDismiss = { paymentDebt = null },
        autoMarkFinishedDebtId = autoMarkFinishedDebtId,
        onMarkFinishedConfirmed = { debtId ->
            viewModel.markDebtAsFinished(debtId)
            autoMarkFinishedDebtId = null
            coroutineScope.launch { snackbarHostState.showSnackbar(snackbarDebtMarkedFinished) }
        },
        onMarkFinishedDismiss = { autoMarkFinishedDebtId = null },
        createBillAction = createBillAction,
        onCreateBillConfirmed = {
            createBillAction?.invoke()
            createBillAction = null
        },
        onCreateBillDismiss = { createBillAction = null },
        createDebtAction = createDebtAction,
        onCreateDebtConfirmed = {
            createDebtAction?.invoke()
            createDebtAction = null
        },
        onCreateDebtDismiss = { createDebtAction = null }
    )

    if (billToDelete != null) {
        SciuroConfirmationDialog(
            title = "Delete Bill",
            message = "Are you sure you want to delete this bill?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteObligation(billToDelete!!.id)
                billToDelete = null
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarBillDeleted) }
            },
            onDismiss = { billToDelete = null }
        )
    }

    if (billToEdit != null) {
        EditBillSheet(
            bill = billToEdit!!,
            accounts = accounts,
            expenseCategories = expenseCategories,
            onDismiss = { billToEdit = null },
            onEdit = { name, amount, frequency, nextDueDate, categoryId, accountId ->
                viewModel.updateObligation(billToEdit!!.id, name, amount, frequency, nextDueDate, categoryId, accountId)
                billToEdit = null
                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarBillUpdated) }
            }
        )
    }

    if (billToView != null) {
        BillDetailSheet(
            bill = billToView!!,
            onDismiss = { billToView = null },
            onEditClick = {
                billToEdit = billToView
                billToView = null
            },
            onDeleteClick = {
                billToDelete = billToView
                billToView = null
            }
        )
    }

    if (debtToView != null) {
        DebtDetailSheet(
            debt = debtToView!!,
            onDismiss = { debtToView = null },
            onEditClick = {
                debtToEdit = debtToView
                debtToView = null
            },
            onDeleteClick = {
                debtToDelete = debtToView
                debtToView = null
            }
        )
    }

    if (showAddSheet) {
        when (selectedTab) {
            "Bills" -> AddBillSheet(
                accounts = accounts,
                expenseCategories = expenseCategories,
                onDismiss = { showAddSheet = false },
                onCreate = { name, amount, frequency, nextDueDate, categoryId, accountId ->
                    createBillAction = {
                        viewModel.createObligation(name, amount, frequency, nextDueDate, categoryId, accountId)
                        showAddSheet = false
                        coroutineScope.launch { snackbarHostState.showSnackbar(snackbarBillCreated) }
                    }
                }
            )
            "Debts" -> AddDebtSheet(
                onDismiss = { showAddSheet = false },
                onCreate = { name, type, direction, principalAmount, counterpartyName, notes ->
                    createDebtAction = {
                        viewModel.createDebt(name, type, direction, principalAmount, counterpartyName, notes)
                        showAddSheet = false
                        coroutineScope.launch { snackbarHostState.showSnackbar(snackbarDebtCreated) }
                    }
                }
            )
        }
    }

    createBillAction?.let { action ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_create_bill_title),
            message = stringResource(R.string.kanban_create_bill_message),
            confirmText = stringResource(R.string.kanban_confirm_create),
            onConfirm = {
                action()
                createBillAction = null
            },
            onDismiss = { createBillAction = null }
        )
    }

    createDebtAction?.let { action ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_create_debt_title),
            message = stringResource(R.string.kanban_create_debt_message),
            confirmText = stringResource(R.string.kanban_confirm_create),
            onConfirm = {
                action()
                createDebtAction = null
            },
            onDismiss = { createDebtAction = null }
        )
    }
}

@Composable
private fun ReviewColumn(
    tasks: List<KanbanTask>,
    accounts: List<Account>,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    onReject: (KanbanTask) -> Unit,
    onApprove: (KanbanTask, String?, String) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillToggle(
            options = listOf("To Do", "In Progress", "Done"),
            selectedOption = selectedStatus,
            onOptionSelected = onStatusChange,
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (tasks.isEmpty()) {
            EmptyStateView(message = stringResource(R.string.kanban_empty_tasks))
        } else {
            tasks.forEach { task ->
                KanbanTaskCard(
                    task = task,
                    accounts = accounts,
                    onApprove = { accountId, direction -> onApprove(task, accountId, direction) },
                    onReject = { onReject(task) }
                )
            }
        }
    }
}

@Composable
private fun BillsColumn(
    bills: List<BillTask>,
    recentlySettledIds: List<String>,
    onMarkPaid: (BillTask) -> Unit,
    onClickBill: (BillTask) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val overdueBills = bills.filter { it.status == BillStatus.OVERDUE }
        val dueSoonBills = bills.filter { it.status == BillStatus.DUE_SOON }
        val upcomingBills = bills.filter { it.status == BillStatus.UPCOMING }
        val settledBills = bills.filter { it.status == BillStatus.SETTLED }

        if (bills.isEmpty()) {
            EmptyStateView(message = stringResource(R.string.kanban_empty_bills))
        } else {
            if (overdueBills.isNotEmpty()) {
                Text(stringResource(R.string.kanban_section_overdue), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                overdueBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, onClick = { onClickBill(bill) }, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
            if (dueSoonBills.isNotEmpty()) {
                Text(stringResource(R.string.kanban_section_due_soon), style = MaterialTheme.typography.titleMedium, color = com.najmi.sciuro.core.ui.theme.SignalWarning, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                dueSoonBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, onClick = { onClickBill(bill) }, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
            if (upcomingBills.isNotEmpty()) {
                Text(stringResource(R.string.kanban_section_upcoming), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                upcomingBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, onClick = { onClickBill(bill) }, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
            if (settledBills.isNotEmpty()) {
                Text("Settled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                settledBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, onClick = { onClickBill(bill) }, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: BillTask,
    onMarkPaid: (BillTask) -> Unit,
    onClick: () -> Unit,
    isRecentlySettled: Boolean = false
) {
    val noMotion = reducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isRecentlySettled && !noMotion) 1.02f else 1f,
        animationSpec = spring(),
        label = "billSettle"
    )
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(bill.obligation.nextDueDate))

    SciuroCard(
        modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) {
                Column {
                    Text(bill.name, style = MaterialTheme.typography.titleMedium)
                    Text("RM ${"%.2f".format(bill.amount)} • Due $formattedDate", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bill.status == BillStatus.OVERDUE) {
                        Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.kanban_section_overdue), tint = MaterialTheme.colorScheme.error)
                    } else if (bill.status == BillStatus.SETTLED) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("PAID", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { onMarkPaid(bill) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.kanban_mark_as_paid))
            }
        }
    }
}

@Composable
private fun DebtsColumn(
    debtTasks: List<DebtTask>,
    showCompletedDebts: Boolean,
    onToggleCompleted: () -> Unit,
    recentlySettledIds: List<String>,
    onRecordPayment: (DebtTask) -> Unit,
    onClickDebt: (DebtTask) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Debts", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show Completed", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = showCompletedDebts,
                    onCheckedChange = { onToggleCompleted() },
                    modifier = Modifier.scale(0.8f)
                )
            }
        }

        val activeDebts = debtTasks.filter { it.debt.status == com.sciuro.core.debt.model.DebtStatus.ACTIVE }
        val completedDebts = debtTasks.filter { it.debt.status == com.sciuro.core.debt.model.DebtStatus.PAID_OFF }

        if (activeDebts.isEmpty() && completedDebts.isEmpty()) {
            EmptyStateView(message = stringResource(R.string.kanban_empty_debts))
        } else {
            val noMotion = reducedMotion()
            activeDebts.forEach { debt ->
                val scale by animateFloatAsState(
                    targetValue = if (debt.id in recentlySettledIds && !noMotion) 1.02f else 1f,
                    animationSpec = spring(),
                    label = "debtSettle"
                )
                SciuroCard(
                    modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale),
                    onClick = { onClickDebt(debt) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(debt.name, style = MaterialTheme.typography.titleMedium)
                                if (debt.counterpartyName != null) {
                                    Text(debt.counterpartyName, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("RM ${"%.2f".format(debt.remainingBalance)}", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val progressColor = if (debt.progress > 0.75f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        LinearProgressIndicator(
                            progress = { if (debt.progress > 1f) 1f else debt.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (debt.type == com.sciuro.core.debt.model.DebtType.MONEY_OWED) {
                            OutlinedButton(
                                onClick = { onRecordPayment(debt) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.kanban_record_payment))
                            }
                        }
                    }
                }
            }

            if (showCompletedDebts && completedDebts.isNotEmpty()) {
                Text("Completed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                completedDebts.forEach { debt ->
                    SciuroCard(modifier = Modifier.fillMaxWidth().alpha(0.5f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        debt.name, 
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    if (debt.counterpartyName != null) {
                                        Text(debt.counterpartyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("PAID OFF", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanTaskCard(
    task: KanbanTask,
    accounts: List<Account>,
    onApprove: (String?, String) -> Unit,
    onReject: () -> Unit
) {
    var selectedAccount by remember(task.id) {
        mutableStateOf(accounts.find { it.id == task.accountId })
    }
    var selectedDirection by remember(task.id) {
        mutableStateOf(task.direction ?: "OUTFLOW")
    }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    SciuroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (task.accountId == null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = stringResource(R.string.kanban_unassigned_account),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            stringResource(R.string.kanban_needs_account_assignment),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (task.title.startsWith("Review Transaction")) {
                PillToggle(
                    options = listOf("Expense", "Income"),
                    selectedOption = if (selectedDirection == "OUTFLOW") "Expense" else "Income",
                    onOptionSelected = { label ->
                        selectedDirection = if (label == "Expense") "OUTFLOW" else "INFLOW"
                    },
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
            }

            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                SciuroTextField(
                    value = selectedAccount?.name ?: stringResource(R.string.kanban_select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.kanban_wallet_account),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccount = account
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.kanban_reject))
                }
                SciuroPrimaryButton(
                    text = stringResource(R.string.kanban_approve),
                    onClick = { onApprove(selectedAccount?.id, selectedDirection) },
                    enabled = selectedAccount != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillSheet(
    accounts: List<Account>,
    expenseCategories: List<Category>,
    onDismiss: () -> Unit,
    onCreate: (name: String, amount: Double, frequency: ObligationFrequency, nextDueDate: Long, categoryId: String?, accountId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(ObligationFrequency.MONTHLY) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.kanban_add_bill_subscription), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            SciuroTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.kanban_label_name))

            Spacer(modifier = Modifier.height(8.dp))
            SciuroAmountField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.kanban_label_amount_rm)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_frequency), style = MaterialTheme.typography.labelLarge)
            val freqLabels = ObligationFrequency.entries.map { it.name.lowercase().replaceFirstChar { it.uppercaseChar() } }
            PillToggle(
                options = freqLabels,
                selectedOption = frequency.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                onOptionSelected = { label ->
                    frequency = ObligationFrequency.entries.first {
                        it.name.lowercase().replaceFirstChar { it.uppercaseChar() } == label
                    }
                },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_next_due_date), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dueDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.kanban_select_date),
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.kanban_pick_date))
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dueDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) { Text(stringResource(R.string.kanban_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.kanban_cancel)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_account), style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                SciuroTextField(
                    value = selectedAccount?.name ?: stringResource(R.string.kanban_select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.kanban_wallet_account),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccount = account
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_category), style = MaterialTheme.typography.labelLarge)
            if (expenseCategories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenseCategories) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = if (categoryId == cat.id) null else cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isFormValid = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && dueDate != null
            SciuroPrimaryButton(
                text = stringResource(R.string.kanban_create_bill_button),
                onClick = {
                    onCreate(
                        name,
                        amountText.toDoubleOrNull() ?: 0.0,
                        frequency,
                        dueDate!!,
                        categoryId,
                        selectedAccount?.id
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: DebtType, direction: DebtDirection, principalAmount: Double, counterpartyName: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(DebtDirection.I_OWE) }
    var amountText by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.kanban_add_debt), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Text(stringResource(R.string.kanban_label_direction), style = MaterialTheme.typography.labelLarge)
            val dirLabels = listOf("I Owe", "Owed to Me")
            PillToggle(
                options = dirLabels,
                selectedOption = when (direction) {
                    DebtDirection.I_OWE -> "I Owe"
                    DebtDirection.OWED_TO_ME -> "Owed to Me"
                },
                onOptionSelected = { label ->
                    direction = when (label) {
                        "I Owe" -> DebtDirection.I_OWE
                        else -> DebtDirection.OWED_TO_ME
                    }
                },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            SciuroTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.kanban_label_debt_name))

            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = stringResource(R.string.kanban_label_counterparty)
            )

            Spacer(modifier = Modifier.height(8.dp))
            SciuroAmountField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.kanban_label_amount_rm)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SciuroPrimaryButton(
                text = stringResource(R.string.kanban_create_debt_button),
                onClick = {
                    onCreate(
                        name,
                        DebtType.MONEY_OWED,
                        direction,
                        amountText.toDoubleOrNull() ?: 0.0,
                        counterparty.ifBlank { null },
                        null
                    )
                },
                enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditDebtSheet(
    debt: DebtTask,
    onDismiss: () -> Unit,
    onEdit: (name: String, principalAmount: Double, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(debt.name) }
    var amountText by remember { mutableStateOf(debt.debt.principalAmount.toString()) }
    var notes by remember { mutableStateOf(debt.debt.notes ?: "") }

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.kanban_edit_debt_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            SciuroTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.kanban_label_debt_name))

            Spacer(modifier = Modifier.height(8.dp))
            SciuroAmountField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.kanban_label_principal_amount)
            )

            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(value = notes, onValueChange = { notes = it }, label = stringResource(R.string.kanban_label_notes_optional))

            Spacer(modifier = Modifier.height(16.dp))

            SciuroPrimaryButton(
                text = stringResource(R.string.kanban_save_changes),
                onClick = {
                    onEdit(
                        name,
                        amountText.toDoubleOrNull() ?: 0.0,
                        notes.ifBlank { null }
                    )
                },
                enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBillSheet(
    bill: BillTask,
    accounts: List<Account>,
    expenseCategories: List<Category>,
    onDismiss: () -> Unit,
    onEdit: (name: String, amount: Double, frequency: ObligationFrequency, nextDueDate: Long, categoryId: String?, accountId: String?) -> Unit
) {
    var name by remember { mutableStateOf(bill.name) }
    var amountText by remember { mutableStateOf(bill.amount.toString()) }
    var frequency by remember { mutableStateOf(bill.obligation.frequency) }
    var dueDate by remember { mutableStateOf<Long?>(bill.obligation.nextDueDate) }
    var categoryId by remember { mutableStateOf<String?>(bill.obligation.categoryId) }
    var selectedAccount by remember { mutableStateOf<Account?>(accounts.find { it.id == bill.obligation.accountId }) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Edit Bill", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            SciuroTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.kanban_label_name))

            Spacer(modifier = Modifier.height(8.dp))
            SciuroAmountField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.kanban_label_amount_rm)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_frequency), style = MaterialTheme.typography.labelLarge)
            val freqLabels = ObligationFrequency.entries.map { it.name.lowercase().replaceFirstChar { it.uppercaseChar() } }
            PillToggle(
                options = freqLabels,
                selectedOption = frequency.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                onOptionSelected = { label ->
                    frequency = ObligationFrequency.entries.first {
                        it.name.lowercase().replaceFirstChar { it.uppercaseChar() } == label
                    }
                },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_next_due_date), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dueDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.kanban_select_date),
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.kanban_pick_date))
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dueDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) { Text(stringResource(R.string.kanban_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.kanban_cancel)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_account), style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                SciuroTextField(
                    value = selectedAccount?.name ?: stringResource(R.string.kanban_select_account),
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.kanban_wallet_account),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccount = account
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.kanban_label_category), style = MaterialTheme.typography.labelLarge)
            if (expenseCategories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenseCategories) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = if (categoryId == cat.id) null else cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isFormValid = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && dueDate != null
            SciuroPrimaryButton(
                text = "Save Changes",
                onClick = {
                    onEdit(
                        name,
                        amountText.toDoubleOrNull() ?: 0.0,
                        frequency,
                        dueDate!!,
                        categoryId,
                        selectedAccount?.id
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}
