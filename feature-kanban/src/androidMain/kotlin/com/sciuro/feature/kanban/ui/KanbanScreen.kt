package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.PillToggle
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
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val debtTasks by viewModel.debtTasks.collectAsState()

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

    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

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
                    heroFigure = when (selectedTab) {
                        "Bills" -> "${billOverdue + billDueSoon} Due"
                        "Debts" -> "$activeDebtCount Active"
                        else -> "${todoCount + inProgressCount}"
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
                            onMarkPaid = { paymentBill = it }
                        )
                        "Debts" -> DebtsColumn(
                            debtTasks = debtTasks,
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
                overdueBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid) }
            }
            if (dueSoonBills.isNotEmpty()) {
                Text("Due Soon", style = MaterialTheme.typography.labelLarge, color = com.najmi.sciuro.core.ui.theme.SignalWarning)
                dueSoonBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid) }
            }
            if (upcomingBills.isNotEmpty()) {
                Text("Upcoming", style = MaterialTheme.typography.labelLarge)
                upcomingBills.forEach { bill -> BillCard(bill = bill, onMarkPaid = onMarkPaid) }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: BillTask,
    onMarkPaid: (BillTask) -> Unit
) {
    SciuroCard(modifier = Modifier.fillMaxWidth()) {
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
    onRecordPayment: (DebtTask) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val activeDebts = debtTasks.filter { it.debt.status == com.sciuro.core.debt.model.DebtStatus.ACTIVE }

        if (activeDebts.isEmpty()) {
            EmptyStateView(message = "No active debts.")
        } else {
            activeDebts.forEach { debt ->
                SciuroCard(modifier = Modifier.fillMaxWidth()) {
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
                            trackColor = Color.LightGray
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
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    SegmentedButton(
                        selected = selectedDirection == "OUTFLOW",
                        onClick = { selectedDirection = "OUTFLOW" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense")
                    }
                    SegmentedButton(
                        selected = selectedDirection == "INFLOW",
                        onClick = { selectedDirection = "INFLOW" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income")
                    }
                }
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
