package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.theme.reducedMotion
import com.najmi.sciuro.core.ui.components.SciuroTextField
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
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val debtTasks by viewModel.debtTasks.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                val billOverdue = remember(bills) { bills.count { it.status == BillStatus.OVERDUE } }
                val billDueSoon = remember(bills) { bills.count { it.status == BillStatus.DUE_SOON } }
                val activeDebtCount = remember(debtTasks) { debtTasks.size }

                HeroPanel(
                    title = when (selectedTab) {
                        "Bills" -> "Bills & Subscriptions"
                        "Debts" -> "Debts"
                        else -> "Active Tasks"
                    },
                    heroFigure = {
                        Text(
                            text = when (selectedTab) {
                                "Bills" -> "${billOverdue + billDueSoon} Due"
                                "Debts" -> "$activeDebtCount Active"
                                else -> "${todoCount + inProgressCount}"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    },
                    toggleOptions = emptyList(),
                    selectedToggle = "",
                    onToggleSelected = {},
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            when (selectedTab) {
                                "Bills" -> {
                                    Text("Overdue: $billOverdue", style = MaterialTheme.typography.bodySmall,
                                        color = if (billOverdue > 0) com.najmi.sciuro.core.ui.theme.SignalDanger else Color.White.copy(alpha = 0.7f))
                                    Text("Due Soon: $billDueSoon", style = MaterialTheme.typography.bodySmall,
                                        color = if (billDueSoon > 0) com.najmi.sciuro.core.ui.theme.SignalWarning else Color.White.copy(alpha = 0.7f))
                                }
                                "Debts" -> {
                                    val totalOwe = remember(debtTasks) { debtTasks.filter { it.direction == com.sciuro.core.debt.model.DebtDirection.I_OWE }.sumOf { it.remainingBalance } }
                                    val totalOwed = remember(debtTasks) { debtTasks.filter { it.direction == com.sciuro.core.debt.model.DebtDirection.OWED_TO_ME }.sumOf { it.remainingBalance } }
                                    Text("I Owe: RM ${"%.0f".format(totalOwe)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("Owed: RM ${"%.0f".format(totalOwed)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                }
                                else -> {
                                    Text("Upcoming: $todoCount", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("Due: $inProgressCount", style = MaterialTheme.typography.bodySmall,
                                        color = if (inProgressCount > 0) com.najmi.sciuro.core.ui.theme.SignalWarning else Color.White.copy(alpha = 0.7f))
                                    Text("Settled: $doneCount", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                )
            }

            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    PillToggle(
                        options = tabs,
                        selectedOption = selectedTab,
                        onOptionSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth(),
                        fillWidth = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedTab) {
                        "Bills" -> BillsColumn(
                            bills = bills,
                            recentlySettledIds = recentlySettledIds,
                            onMarkPaid = { paymentBill = it }
                        )
                        "Debts" -> DebtsColumn(
                            debtTasks = debtTasks,
                            recentlySettledIds = recentlySettledIds,
                            onRecordPayment = { paymentDebt = it }
                        )
                        else -> ReviewColumn(
                            tasks = filteredTasks,
                            accounts = accounts,
                            selectedStatus = selectedStatus,
                            onStatusChange = { selectedStatus = it },
                            onReject = { taskToReject = it },
                            onApprove = { task, accountId, direction ->
                                viewModel.updateTaskStatus(task.id, TaskStatus.DONE, accountId, direction)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Task Approved") }
                            }
                        )
                    }
                }
            }
        }

        if (selectedTab != "Review") {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add ${selectedTab}")
            }
        }
    }

    taskToReject?.let { task ->
        SciuroConfirmationDialog(
            title = "Reject Task",
            message = "Are you sure you want to reject '${task.title}'?",
            confirmText = "Reject",
            isDestructive = true,
            onConfirm = {
                viewModel.updateTaskStatus(task.id, TaskStatus.REJECTED, null)
                taskToReject = null
                coroutineScope.launch { snackbarHostState.showSnackbar("Task Rejected") }
            },
            onDismiss = { taskToReject = null }
        )
    }

    paymentBill?.let { bill ->
        SciuroBottomSheet(onDismissRequest = { paymentBill = null }) {
            Text("Mark Bill as Paid", style = MaterialTheme.typography.headlineSmall)
            Text("${bill.name} — RM ${"%.2f".format(bill.amount)}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            SciuroPrimaryButton(
                text = "Confirm Payment",
                onClick = {
                    viewModel.markBillAsPaid(bill.obligation)
                    paymentBill = null
                    coroutineScope.launch { snackbarHostState.showSnackbar("Bill marked as paid") }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    paymentDebt?.let { debt ->
        SciuroBottomSheet(onDismissRequest = { paymentDebt = null }) {
            Text("Record Payment", style = MaterialTheme.typography.headlineSmall)
            Text("${debt.name} — RM ${"%.2f".format(debt.remainingBalance)} remaining", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            SciuroTextField(
                value = paymentAmountText,
                onValueChange = { paymentAmountText = it },
                label = "Payment Amount (RM)",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SciuroPrimaryButton(
                text = "Apply Payment",
                onClick = {
                    val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        viewModel.recordDebtPayment(debt.id, amt)
                        paymentDebt = null
                        paymentAmountText = ""
                        coroutineScope.launch { snackbarHostState.showSnackbar("Payment recorded") }
                    }
                },
                enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAddSheet) {
        when (selectedTab) {
            "Bills" -> AddBillSheet(
                accounts = accounts,
                expenseCategories = expenseCategories,
                onDismiss = { showAddSheet = false },
                onCreate = { name, amount, frequency, nextDueDate, categoryId, accountId ->
                    viewModel.createObligation(name, amount, frequency, nextDueDate, categoryId, accountId)
                    showAddSheet = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Bill created") }
                }
            )
            "Debts" -> AddDebtSheet(
                onDismiss = { showAddSheet = false },
                onCreate = { name, type, direction, principalAmount, counterpartyName, notes ->
                    viewModel.createDebt(name, type, direction, principalAmount, counterpartyName, notes)
                    showAddSheet = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Debt created") }
                }
            )
        }
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
            EmptyStateView(message = "No tasks in this list.")
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
    onMarkPaid: (BillTask) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val overdueBills = bills.filter { it.status == BillStatus.OVERDUE }
        val dueSoonBills = bills.filter { it.status == BillStatus.DUE_SOON }
        val upcomingBills = bills.filter { it.status == BillStatus.UPCOMING }

        if (bills.isEmpty()) {
            EmptyStateView(message = "No bills or subscriptions yet. They will appear once detected from recurring transactions.")
        } else {
            if (overdueBills.isNotEmpty()) {
                Text("Overdue", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                overdueBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
            if (dueSoonBills.isNotEmpty()) {
                Text("Due Soon", style = MaterialTheme.typography.labelLarge, color = com.najmi.sciuro.core.ui.theme.SignalWarning)
                dueSoonBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
            if (upcomingBills.isNotEmpty()) {
                Text("Upcoming", style = MaterialTheme.typography.labelLarge)
                upcomingBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid, isRecentlySettled = bill.obligation.id in recentlySettledIds) }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: BillTask,
    onMarkPaid: (BillTask) -> Unit,
    isRecentlySettled: Boolean = false
) {
    val noMotion = reducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isRecentlySettled && !noMotion) 1.02f else 1f,
        animationSpec = spring(),
        label = "billSettle"
    )
    SciuroCard(modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text(bill.name, style = MaterialTheme.typography.titleMedium)
                    Text("RM ${"%.2f".format(bill.amount)}", style = MaterialTheme.typography.bodyMedium)
                }
                if (bill.status == BillStatus.OVERDUE) {
                    Icon(Icons.Filled.Warning, contentDescription = "Overdue", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { onMarkPaid(bill) }, modifier = Modifier.fillMaxWidth()) {
                Text("Mark as Paid")
            }
        }
    }
}

@Composable
private fun DebtsColumn(
    debtTasks: List<DebtTask>,
    recentlySettledIds: List<String>,
    onRecordPayment: (DebtTask) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val activeDebts = debtTasks.filter { it.debt.status == com.sciuro.core.debt.model.DebtStatus.ACTIVE }

        if (activeDebts.isEmpty()) {
            EmptyStateView(message = "No active debts.")
        } else {
            val noMotion = reducedMotion()
            activeDebts.forEach { debt ->
                val scale by animateFloatAsState(
                    targetValue = if (debt.id in recentlySettledIds && !noMotion) 1.02f else 1f,
                    animationSpec = spring(),
                    label = "debtSettle"
                )
                SciuroCard(modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale)) {
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
                            Text("RM ${"%.2f".format(debt.remainingBalance)}", style = MaterialTheme.typography.titleMedium)
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
                                Text("Record Payment")
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.accountId == null) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.accountId == null) MaterialTheme.colorScheme.onErrorContainer else Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.accountId == null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.accountId == null) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Unassigned Account",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (task.title.startsWith("Review Transaction")) {
                PillToggle(
                    options = listOf("Expense", "Income"),
                    selectedOption = if (selectedDirection == "OUTFLOW") "Expense" else "Income",
                    onOptionSelected = { label ->
                        selectedDirection = if (label == "Expense") "OUTFLOW" else "INFLOW"
                    },
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                SciuroTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = "Wallet Account",
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
                SciuroPrimaryButton(
                    text = "Approve",
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Add Bill / Subscription", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            SciuroTextField(value = name, onValueChange = { name = it }, label = "Name")

            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (RM)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Frequency", style = MaterialTheme.typography.labelLarge)
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
            Text("Next Due Date", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dueDate?.let { dateFormatter.format(Date(it)) } ?: "Select Date",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text("Pick Date")
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
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Account", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                SciuroTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = "Wallet Account",
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
            Text("Category", style = MaterialTheme.typography.labelLarge)
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
                text = "Create Bill",
                onClick = {
                    onCreate(
                        name,
                        amountText.toDouble(),
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Add Debt", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Direction", style = MaterialTheme.typography.labelLarge)
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
            SciuroTextField(value = name, onValueChange = { name = it }, label = "Debt Name")

            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = "Counterparty (who owes / is owed)"
            )

            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (RM)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SciuroPrimaryButton(
                text = "Create Debt",
                onClick = {
                    onCreate(
                        name,
                        DebtType.MONEY_OWED,
                        direction,
                        amountText.toDouble(),
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
