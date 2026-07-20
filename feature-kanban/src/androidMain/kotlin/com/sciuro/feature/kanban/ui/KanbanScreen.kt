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
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.ledger.model.Account
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import com.sciuro.feature.kanban.viewmodel.KanbanViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // In a real app, calculate actual totals
    val activeDebt = 5000.00
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeroPanel(
                title = "Active Debt & Bills",
                heroFigure = "RM ${"%.2f".format(activeDebt)}",
                toggleOptions = listOf("To Do", "In Progress", "Done"),
                selectedToggle = selectedStatus,
                onToggleSelected = { selectedStatus = it }
            )
        }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                                    
                                    // Account Selection
                                    ExposedDropdownMenuBox(
                                        expanded = accountDropdownExpanded,
                                        onExpandedChange = { accountDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedAccount?.name ?: "Select Account",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Wallet Account") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                                                viewModel.updateTaskStatus(task.id, TaskStatus.REJECTED, null)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Reject")
                                        }
                                        Button(
                                            onClick = { 
                                                viewModel.updateTaskStatus(task.id, TaskStatus.DONE, selectedAccount?.id)
                                            },
                                            enabled = selectedAccount != null,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve")
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
}

