package com.sciuro.feature.debt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.najmi.sciuro.core.ui.theme.SignalWarning
import com.najmi.sciuro.core.ui.util.SciuroHaptics
import com.najmi.sciuro.core.ui.util.bounceClick
import com.sciuro.feature.debt.R
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroFigurePair
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtType
import com.sciuro.feature.debt.viewmodel.BnplRiskInfo
import com.sciuro.feature.debt.viewmodel.DebtViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtOverviewScreen(
    viewModel: DebtViewModel = koinViewModel()
) {
    val allDebts by viewModel.debts.collectAsState()
    val debtsIOwe by viewModel.debtsIOwe.collectAsState()
    val debtsOwedToMe by viewModel.debtsOwedToMe.collectAsState()
    val bnplRisk by viewModel.bnplRisk.collectAsState()

    var selectedTab by remember { mutableStateOf("I Owe") }
    val tabs = listOf("I Owe", "Owed to Me")
    val displayedDebts = if (selectedTab == "I Owe") debtsIOwe else debtsOwedToMe

    var showFormSheet by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<com.sciuro.feature.debt.model.DebtUiModel?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showRecordPayment by remember { mutableStateOf<String?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }
    
    // Confirmation dialog states
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showCreateConfirmation by remember { mutableStateOf(false) }
    var showApplyPaymentConfirmation by remember { mutableStateOf(false) }
    var showMarkFinishedConfirmation by remember { mutableStateOf(false) }
    var autoMarkFinishedDebtId by remember { mutableStateOf<String?>(null) }

    var contextMenuDebtId by remember { mutableStateOf<String?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
                    title = stringResource(R.string.debt_title),
                    heroFigure = if (allDebts.isEmpty()) {
                        { Text(stringResource(R.string.debt_empty_state), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) }
                    } else {
                        { HeroFigurePair(first = totalIOwe, second = totalOwedToMe) }
                    },
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Owed: RM ${"%.2f".format(totalOwedToMe)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                        if (bnplRisk.count >= 2) {
                            BnplWarningCard(bnplRisk = bnplRisk)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (displayedDebts.isEmpty()) {
                            EmptyStateView(
                                message = if (selectedTab == "I Owe")
                                    stringResource(R.string.debt_empty_i_owe)
                                else
                                    stringResource(R.string.debt_empty_owed),
                                primaryCtaText = stringResource(R.string.debt_add),
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
                                        .bounceClick(
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
                                            },
                                            onLongClick = {
                                                SciuroHaptics.success(haptic)
                                                contextMenuDebtId = debt.id
                                            }
                                        )
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

                                        val progressColor = when {
                                            debt.progress > 0.75f -> MaterialTheme.colorScheme.primary
                                            debt.progress > 0.25f -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                        LinearProgressIndicator(
                                            progress = { if (debt.progress > 1f) 1f else debt.progress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            color = progressColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                                                    Text(stringResource(R.string.debt_record_payment))
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
                                                Text(stringResource(R.string.debt_edit))
                                            }
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = contextMenuDebtId == debt.id,
                                    onDismissRequest = { contextMenuDebtId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.debt_context_edit)) },
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
                                            contextMenuDebtId = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.debt_context_mark_finished)) },
                                        onClick = {
                                            editingDebt = debt
                                            showMarkFinishedConfirmation = true
                                            contextMenuDebtId = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.debt_context_delete)) },
                                        onClick = {
                                            editingDebt = debt
                                            showDeleteConfirmation = true
                                            contextMenuDebtId = null
                                        }
                                    )
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.debt_add))
            }
        }
    }

    if (showFormSheet) {
        val isEditing = editingDebt != null
        SciuroBottomSheet(onDismissRequest = { showFormSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    if (isEditing) stringResource(R.string.debt_form_title_edit) else stringResource(R.string.debt_form_title_add),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(stringResource(R.string.debt_form_direction), style = MaterialTheme.typography.labelLarge)
                val directionLabels = listOf("I Owe", "Owed to Me")
                PillToggle(
                    options = directionLabels,
                    selectedOption = when (formDirection) {
                        DebtDirection.I_OWE -> "I Owe"
                        DebtDirection.OWED_TO_ME -> "Owed to Me"
                    },
                    onOptionSelected = { label ->
                        formDirection = when (label) {
                            "I Owe" -> DebtDirection.I_OWE
                            else -> DebtDirection.OWED_TO_ME
                        }
                    },
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                SciuroTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = stringResource(R.string.debt_form_debt_name)
                )

                if (formDirection == DebtDirection.OWED_TO_ME || formType == DebtType.MONEY_OWED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = formCounterparty,
                        onValueChange = { formCounterparty = it },
                        label = stringResource(R.string.debt_form_counterparty)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SciuroAmountField(
                    value = formAmountText,
                    onValueChange = { formAmountText = it },
                    label = stringResource(R.string.debt_form_amount_rm)
                )

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = formNotes,
                        onValueChange = { formNotes = it },
                        label = stringResource(R.string.debt_form_notes)
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
                            Text(stringResource(R.string.debt_delete))
                        }

                        SciuroPrimaryButton(
                            text = stringResource(R.string.debt_mark_finished),
                            onClick = {
                                showMarkFinishedConfirmation = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                SciuroPrimaryButton(
                    text = if (isEditing) stringResource(R.string.debt_save) else stringResource(R.string.debt_create),
                    onClick = {
                        val amt = formAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && formName.isNotBlank()) {
                            if (isEditing) {
                                showSaveConfirmation = true
                            } else {
                                showCreateConfirmation = true
                            }
                        }
                    },
                    enabled = formName.isNotBlank() && (formAmountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    if (showMarkFinishedConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_mark_finished_title),
            message = stringResource(R.string.debt_mark_finished_message),
            confirmText = stringResource(R.string.debt_mark_finished),
            onConfirm = {
                editingDebt?.let { viewModel.markAsPaidOff(it.id) }
                showMarkFinishedConfirmation = false
                showFormSheet = false
            },
            onDismiss = { showMarkFinishedConfirmation = false }
        )
    }

    if (showSaveConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_save_changes_title),
            message = stringResource(R.string.debt_save_changes_message),
            confirmText = stringResource(R.string.debt_confirm_save),
            onConfirm = {
                val amt = formAmountText.toDoubleOrNull() ?: 0.0
                editingDebt?.let {
                    viewModel.updateDebt(
                        debt = it,
                        name = formName,
                        principalAmount = amt,
                        remainingBalance = amt, // Note: This might overwrite balance incorrectly if payment was made, but keeping original logic
                        counterpartyName = formCounterparty.ifBlank { null },
                        notes = formNotes.ifBlank { null }
                    )
                }
                showSaveConfirmation = false
                showFormSheet = false
            },
            onDismiss = { showSaveConfirmation = false }
        )
    }

    if (showCreateConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_create_title),
            message = stringResource(R.string.debt_create_message),
            confirmText = stringResource(R.string.debt_confirm_create),
            onConfirm = {
                val amt = formAmountText.toDoubleOrNull() ?: 0.0
                viewModel.createDebt(
                    name = formName,
                    type = formType,
                    direction = formDirection,
                    principalAmount = amt,
                    counterpartyName = formCounterparty.ifBlank { null },
                    notes = null
                )
                showCreateConfirmation = false
                showFormSheet = false
            },
            onDismiss = { showCreateConfirmation = false }
        )
    }

    if (showDeleteConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_delete_title),
            message = stringResource(R.string.debt_delete_message),
            confirmText = stringResource(R.string.debt_confirm_delete),
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
            Text(stringResource(R.string.debt_record_payment), style = MaterialTheme.typography.headlineSmall)

            SciuroAmountField(
                value = paymentAmountText,
                onValueChange = { paymentAmountText = it },
                label = stringResource(R.string.debt_amount_received_rm)
            )

            SciuroPrimaryButton(
                text = stringResource(R.string.debt_apply_payment),
                onClick = {
                    val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        showApplyPaymentConfirmation = true
                    }
                },
                enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showApplyPaymentConfirmation) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_apply_payment_title),
            message = stringResource(R.string.debt_apply_payment_message),
            confirmText = stringResource(R.string.debt_confirm_apply),
            onConfirm = {
                val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                showRecordPayment?.let { debtId ->
                    viewModel.recordPayment(debtId, amt)
                    
                    val debt = allDebts.find { it.id == debtId }
                    if (debt != null && (debt.remainingBalance - amt) <= 0) {
                        autoMarkFinishedDebtId = debtId
                    }
                }
                showApplyPaymentConfirmation = false
                showRecordPayment = null
            },
            onDismiss = { showApplyPaymentConfirmation = false }
        )
    }

    if (autoMarkFinishedDebtId != null) {
        SciuroConfirmationDialog(
            title = stringResource(R.string.debt_fully_paid_title),
            message = stringResource(R.string.debt_fully_paid_message),
            confirmText = stringResource(R.string.debt_mark_finished),
            onConfirm = {
                viewModel.markAsPaidOff(autoMarkFinishedDebtId!!)
                autoMarkFinishedDebtId = null
            },
            onDismiss = { autoMarkFinishedDebtId = null }
        )
    }
}

@Composable
private fun BnplWarningCard(bnplRisk: BnplRiskInfo) {
    SciuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SignalWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = SignalWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.debt_bnpl_risk_title, bnplRisk.count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.debt_bnpl_risk_desc, bnplRisk.total.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
