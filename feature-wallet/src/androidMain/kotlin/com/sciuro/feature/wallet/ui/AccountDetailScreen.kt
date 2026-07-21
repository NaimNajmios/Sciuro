package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.components.AdjustmentBottomSheet
import com.sciuro.feature.wallet.viewmodel.AccountDetailViewModel
import com.sciuro.feature.wallet.viewmodel.TimelineItem
import kotlinx.coroutines.launch
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import org.koin.androidx.compose.koinViewModel

private val filterOptions = listOf("All", "Transactions", "Adjustments", "Income", "Expense")

@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf(
        null,
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#F44336", // Red
        "#9C27B0", // Purple
        "#FF9800", // Orange
        "#607D8B", // Blue Grey
        "#1A1A1A", // Dark
        "#795548"  // Brown
    )

    if (state.account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val account = state.account!!

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeroPanel(
                title = account.name,
                heroFigure = "RM ${"%.2f".format(account.balance)}",
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )

            Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 36.dp, end = 16.dp)) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Adjust Balance") },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                        onClick = {
                            expanded = false
                            showAdjustmentDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Color") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            expanded = false
                            selectedColor = state.account?.color
                            showColorDialog = true
                        }
                    )
                    if (state.account?.is_system == 0L) {
                        DropdownMenuItem(
                            text = { Text("Archive Account") },
                            onClick = {
                                expanded = false
                                showArchiveDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
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

                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter) }
                        )
                    }
                }

                if (state.timeline.isEmpty()) {
                    com.najmi.sciuro.core.ui.components.EmptyStateView(
                        message = if (state.selectedFilter == "Adjustments") "No adjustments recorded for this account."
                                   else if (state.selectedFilter == "All" && state.transactions.isEmpty() && state.adjustments.isEmpty()) "No transactions or adjustments found for this account."
                                   else "No items match the current filter."
                    )
                } else {
                    for (item in state.timeline) {
                        when (item) {
                            is TimelineItem.TransactionItem -> {
                                val tx = item.tx
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
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed",
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
                            is TimelineItem.AdjustmentItem -> {
                                val adj = item.adjustment
                                AdjustmentCard(
                                    reason = adj.reason,
                                    amount = adj.amount,
                                    formattedTime = "",
                                    onClick = {
                                        viewModel.deleteCorrection(adj.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Adjustment removed")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustmentDialog && state.account != null) {
        AdjustmentBottomSheet(
            currentBalance = state.account!!.balance,
            onDismiss = { showAdjustmentDialog = false },
            onConfirm = { amount, reason ->
                viewModel.recordCorrection(amount, reason)
                showAdjustmentDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Balance adjustment recorded")
                }
            }
        )
    }

    if (showArchiveDialog) {
        SciuroConfirmationDialog(
            title = "Archive Account",
            message = "Are you sure you want to archive this account? It will be hidden from the main wallet views but historical transactions will be kept.",
            confirmText = "Archive",
            isDestructive = false,
            onConfirm = {
                viewModel.archiveAccount()
                showArchiveDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Account archived") }
                onNavigateBack()
            },
            onDismiss = { showArchiveDialog = false }
        )
    }

    if (showDeleteDialog) {
        SciuroConfirmationDialog(
            title = "Delete Account",
            message = "Are you sure you want to permanently delete this account? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Account deleted") }
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Choose Account Color") },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(presetColors) { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colorHex == null) MaterialTheme.colorScheme.surfaceVariant
                                    else try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (colorHex == null) MaterialTheme.colorScheme.onSurface else Color.White)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateAccountColor(selectedColor)
                    showColorDialog = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Account color updated") }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
