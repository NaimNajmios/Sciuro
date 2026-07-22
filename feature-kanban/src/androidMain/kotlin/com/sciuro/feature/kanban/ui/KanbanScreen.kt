package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.ExperimentalFoundationApi
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.sciuro.core.ledger.model.Account
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import com.sciuro.feature.kanban.viewmodel.KanbanViewModel
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
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

    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var taskToReject by remember { mutableStateOf<KanbanTask?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                HeroPanel(
                    title = "Active Tasks",
            heroFigure = "${todoCount + inProgressCount}",
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
                    Text(
                        text = "Upcoming: $todoCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Due: $inProgressCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inProgressCount > 0) com.najmi.sciuro.core.ui.theme.SignalWarning else Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Settled: $doneCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
            }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    PillToggle(
                        options = listOf("To Do", "In Progress", "Done"),
                        selectedOption = selectedStatus,
                        onOptionSelected = { selectedStatus = it },
                        modifier = Modifier.fillMaxWidth(),
                        fillWidth = true
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredTasks.isEmpty()) {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(
                            message = "No tasks in this list."
                        )
                    } else {
                        filteredTasks.forEach { task ->
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
                                            onClick = {
                                                taskToReject = task
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Reject")
                                        }
                                        SciuroPrimaryButton(
                                            text = "Approve",
                                            onClick = {
                                                viewModel.updateTaskStatus(task.id, TaskStatus.DONE, selectedAccount?.id, selectedDirection)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Task Approved")
                                                }
                                            },
                                            enabled = selectedAccount != null,
                                            modifier = Modifier.weight(1f)
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

    taskToReject?.let { task ->
        SciuroConfirmationDialog(
            title = "Reject Task",
            message = "Are you sure you want to reject '${task.title}'? It will be removed from the active workflow.",
            confirmText = "Reject",
            isDestructive = true,
            onConfirm = {
                viewModel.updateTaskStatus(task.id, TaskStatus.REJECTED, null)
                taskToReject = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Task Rejected")
                }
            },
            onDismiss = { taskToReject = null }
        )
    }
}


