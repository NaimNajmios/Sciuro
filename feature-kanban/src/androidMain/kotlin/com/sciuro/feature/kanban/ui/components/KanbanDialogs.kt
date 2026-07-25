package com.sciuro.feature.kanban.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.feature.kanban.model.BillTask
import com.sciuro.feature.kanban.model.DebtTask
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import com.sciuro.feature.kanban.ui.EditDebtSheet
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

@Composable
fun KanbanDialogs(
    taskToReject: KanbanTask?,
    onRejectConfirmed: (KanbanTask) -> Unit,
    onRejectDismiss: () -> Unit,
    debtToDelete: DebtTask?,
    onDeleteDebtConfirmed: (DebtTask) -> Unit,
    onDeleteDebtDismiss: () -> Unit,
    debtToEdit: DebtTask?,
    onEditDebtConfirmed: (DebtTask, String, Double, String?) -> Unit,
    onEditDebtDismiss: () -> Unit,
    taskToApprove: Triple<KanbanTask, String?, String>?,
    onApproveConfirmed: (KanbanTask, String?, String) -> Unit,
    onApproveDismiss: () -> Unit,
    paymentBill: BillTask?,
    onBillPaid: (BillTask) -> Unit,
    onBillPaymentDismiss: () -> Unit,
    paymentDebt: DebtTask?,
    paymentAmountText: String,
    onPaymentAmountChange: (String) -> Unit,
    onDebtPaymentApply: (DebtTask, Double) -> Unit,
    onDebtPaymentDismiss: () -> Unit,
    autoMarkFinishedDebtId: String?,
    onMarkFinishedConfirmed: (String) -> Unit,
    onMarkFinishedDismiss: () -> Unit,
    createBillAction: (() -> Unit)?,
    onCreateBillConfirmed: () -> Unit,
    onCreateBillDismiss: () -> Unit,
    createDebtAction: (() -> Unit)?,
    onCreateDebtConfirmed: () -> Unit,
    onCreateDebtDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    taskToReject?.let { task ->
        SciuroConfirmationDialog(
            title = "Reject Task",
            message = "Are you sure you want to reject '${task.title}'?",
            confirmText = "Reject",
            isDestructive = true,
            onConfirm = { onRejectConfirmed(task) },
            onDismiss = onRejectDismiss
        )
    }

    debtToDelete?.let { debt ->
        SciuroConfirmationDialog(
            title = "Delete Debt",
            message = "Are you sure you want to delete '${debt.name}'?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = { onDeleteDebtConfirmed(debt) },
            onDismiss = onDeleteDebtDismiss
        )
    }

    debtToEdit?.let { debt ->
        EditDebtSheet(
            debt = debt,
            onDismiss = onEditDebtDismiss,
            onEdit = { name, principalAmount, notes ->
                onEditDebtConfirmed(debt, name, principalAmount, notes)
            }
        )
    }

    taskToApprove?.let { (task, accountId, direction) ->
        SciuroConfirmationDialog(
            title = "Approve Task",
            message = "Are you sure you want to approve '${task.title}'?",
            confirmText = "Approve",
            onConfirm = { onApproveConfirmed(task, accountId, direction) },
            onDismiss = onApproveDismiss
        )
    }

    paymentBill?.let { bill ->
        SciuroBottomSheet(onDismissRequest = onBillPaymentDismiss) {
            Text("Mark Bill as Paid", style = MaterialTheme.typography.headlineSmall)
            Text("${bill.name} — RM ${"%.2f".format(bill.amount)}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            SciuroPrimaryButton(
                text = "Confirm Payment",
                onClick = { onBillPaid(bill) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    paymentDebt?.let { debt ->
        SciuroBottomSheet(onDismissRequest = onDebtPaymentDismiss) {
            Text("Record Payment", style = MaterialTheme.typography.headlineSmall)
            Text("${debt.name} — RM ${"%.2f".format(debt.remainingBalance)} remaining", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            SciuroTextField(
                value = paymentAmountText,
                onValueChange = onPaymentAmountChange,
                label = "Payment Amount (RM)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SciuroPrimaryButton(
                text = "Apply Payment",
                onClick = {
                    val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onDebtPaymentApply(debt, amt)
                    }
                },
                enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    autoMarkFinishedDebtId?.let { debtId ->
        SciuroConfirmationDialog(
            title = "Mark Debt as Finished",
            message = "This payment fully covers the remaining balance. Would you like to mark this debt as finished?",
            confirmText = "Mark Finished",
            isDestructive = false,
            onConfirm = { onMarkFinishedConfirmed(debtId) },
            onDismiss = onMarkFinishedDismiss
        )
    }

    createBillAction?.let {
        SciuroConfirmationDialog(
            title = "Create Bill",
            message = "Are you sure you want to create this bill?",
            confirmText = "Create",
            onConfirm = onCreateBillConfirmed,
            onDismiss = onCreateBillDismiss
        )
    }

    createDebtAction?.let {
        SciuroConfirmationDialog(
            title = "Create Debt",
            message = "Are you sure you want to create this debt?",
            confirmText = "Create",
            onConfirm = onCreateDebtConfirmed,
            onDismiss = onCreateDebtDismiss
        )
    }
}