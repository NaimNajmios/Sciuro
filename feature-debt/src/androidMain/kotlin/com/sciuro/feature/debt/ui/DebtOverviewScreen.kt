package com.sciuro.feature.debt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtType
import com.sciuro.feature.debt.viewmodel.DebtViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtOverviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtViewModel = koinViewModel()
) {
    val allDebts by viewModel.debts.collectAsState()
    val debtsIOwe by viewModel.debtsIOwe.collectAsState()
    val debtsOwedToMe by viewModel.debtsOwedToMe.collectAsState()

    var selectedTab by remember { mutableStateOf("I Owe") }
    val tabs = listOf("I Owe", "Owed to Me")
    val displayedDebts = if (selectedTab == "I Owe") debtsIOwe else debtsOwedToMe

    var showFormSheet by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<com.sciuro.feature.debt.model.DebtUiModel?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showRecordPayment by remember { mutableStateOf<String?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }

    // Form state
    var formName by remember { mutableStateOf("") }
    var formType by remember { mutableStateOf(DebtType.MONEY_OWED) }
    var formDirection by remember { mutableStateOf(DebtDirection.I_OWE) }
    var formAmountText by remember { mutableStateOf("") }
    var formCounterparty by remember { mutableStateOf("") }
    var formNotes by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                val totalIOwe = remember(debtsIOwe) { debtsIOwe.sumOf { it.remainingBalance } }
                val totalOwedToMe = remember(debtsOwedToMe) { debtsOwedToMe.sumOf { it.remainingBalance } }

                HeroPanel(
                    title = "Debts",
                    heroFigure = if (allDebts.isEmpty()) "0 Debts"
                        else "RM ${"%.0f".format(totalIOwe)} / RM ${"%.0f".format(totalOwedToMe)}",
                    toggleOptions = emptyList(),
                    selectedToggle = "",
                    onToggleSelected = {},
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "I Owe: RM ${"%.2f".format(totalIOwe)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Owed: RM ${"%.2f".format(totalOwedToMe)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (displayedDebts.isEmpty()) {
                            EmptyStateView(
                                message = if (selectedTab == "I Owe")
                                    "No debts you owe — add a loan, credit card, or informal debt."
                                else
                                    "No one owes you money yet — add informal debts people owe you.",
                                primaryCtaText = "Add Debt",
                                onPrimaryCtaClick = {
                                    formName = ""
                                    formType = DebtType.MONEY_OWED
                                    formDirection = if (selectedTab == "I Owe") DebtDirection.I_OWE else DebtDirection.OWED_TO_ME
                                    formAmountText = ""
                                    formCounterparty = ""
                                    formNotes = ""
                                    editingDebt = null
                                    showFormSheet = true
                                }
                            )
                        } else {
                            displayedDebts.forEach { debt ->
                                SciuroCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(debt.name, style = MaterialTheme.typography.titleMedium)
                                                if (debt.counterpartyName != null) {
                                                    Text(
                                                        debt.counterpartyName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                "RM ${"%.2f".format(debt.remainingBalance)}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val progressColor = if (debt.progress > 0.75f) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        LinearProgressIndicator(
                                            progress = { if (debt.progress > 1f) 1f else debt.progress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            color = progressColor,
                                            trackColor = Color.LightGray
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "${(debt.progress * 100).toInt()}% paid",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (debt.type == DebtType.MONEY_OWED && debt.direction == DebtDirection.OWED_TO_ME) {
                                                OutlinedButton(
                                                    onClick = {
                                                        showRecordPayment = debt.id
                                                        paymentAmountText = ""
                                                    },
                                                    modifier = Modifier.padding(end = 8.dp)
                                                ) {
                                                    Text("Record Payment")
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    editingDebt = debt
                                                    formName = debt.name
                                                    formType = debt.type
                                                    formDirection = debt.direction
                                                    formAmountText = if (debt.remainingBalance > 0)
                                                        debt.remainingBalance.toInt().toString() else ""
                                                    formCounterparty = debt.counterpartyName ?: ""
                                                    formNotes = debt.notes ?: ""
                                                    showFormSheet = true
                                                }
                                            ) {
                                                Text("Edit")
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

        if (allDebts.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    formName = ""
                    formType = DebtType.MONEY_OWED
                    formDirection = if (selectedTab == "I Owe") DebtDirection.I_OWE else DebtDirection.OWED_TO_ME
                    formAmountText = ""
                    formCounterparty = ""
                    formNotes = ""
                    editingDebt = null
                    showFormSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Debt")
            }
        }
    }

    if (showFormSheet) {
        val isEditing = editingDebt != null
        SciuroBottomSheet(onDismissRequest = { showFormSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (isEditing) "Edit Debt" else "Add Debt",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Direction", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DebtDirection.entries.forEachIndexed { index, dir ->
                        SegmentedButton(
                            selected = formDirection == dir,
                            onClick = { formDirection = dir },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DebtDirection.entries.size
                            )
                        ) {
                            Text(when (dir) {
                                DebtDirection.I_OWE -> "I Owe"
                                DebtDirection.OWED_TO_ME -> "Owed to Me"
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SciuroTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = "Debt Name"
                )

                if (formDirection == DebtDirection.OWED_TO_ME || formType == DebtType.MONEY_OWED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = formCounterparty,
                        onValueChange = { formCounterparty = it },
                        label = "Counterparty (who owes / is owed)"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SciuroTextField(
                    value = formAmountText,
                    onValueChange = { formAmountText = it },
                    label = "Amount (RM)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = formNotes,
                        onValueChange = { formNotes = it },
                        label = "Notes"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }

                        SciuroPrimaryButton(
                            text = "Mark Paid Off",
                            onClick = {
                                editingDebt?.let { viewModel.markAsPaidOff(it.id) }
                                showFormSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                SciuroPrimaryButton(
                    text = if (isEditing) "Save" else "Create Debt",
                    onClick = {
                        val amt = formAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && formName.isNotBlank()) {
                            if (isEditing) {
                                editingDebt?.let {
                                    viewModel.updateDebt(
                                        debt = it,
                                        name = formName,
                                        principalAmount = amt,
                                        remainingBalance = amt,
                                        counterpartyName = formCounterparty.ifBlank { null },
                                        notes = formNotes.ifBlank { null }
                                    )
                                }
                            } else {
                                viewModel.createDebt(
                                    name = formName,
                                    type = formType,
                                    direction = formDirection,
                                    principalAmount = amt,
                                    counterpartyName = formCounterparty.ifBlank { null },
                                    notes = null
                                )
                            }
                            showFormSheet = false
                        }
                    },
                    enabled = formName.isNotBlank() && (formAmountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        SciuroConfirmationDialog(
            title = "Delete Debt",
            message = "Are you sure you want to delete this debt? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                editingDebt?.let { viewModel.deleteDebt(it.id) }
                showDeleteConfirmation = false
                showFormSheet = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    showRecordPayment?.let { debtId ->
        SciuroBottomSheet(onDismissRequest = { showRecordPayment = null }) {
            Text("Record Payment", style = MaterialTheme.typography.headlineSmall)

            SciuroTextField(
                value = paymentAmountText,
                onValueChange = { paymentAmountText = it },
                label = "Amount Received (RM)",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            SciuroPrimaryButton(
                text = "Apply Payment",
                onClick = {
                    val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        viewModel.recordPayment(debtId, amt)
                        showRecordPayment = null
                    }
                },
                enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
