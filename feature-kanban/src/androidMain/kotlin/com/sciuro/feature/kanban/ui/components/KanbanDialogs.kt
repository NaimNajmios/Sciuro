package com.sciuro.feature.kanban.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.sciuro.feature.kanban.R
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

@Suppress("LongParameterList")
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
    @Suppress("UnusedParameter")
    modifier: Modifier = Modifier
) {
    taskToReject?.let { task ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_reject_task_title),
            message = stringResource(R.string.kanban_confirm_reject_message, task.title),
            confirmText = stringResource(R.string.kanban_confirm_reject),
            isDestructive = true,
            onConfirm = { onRejectConfirmed(task) },
            onDismiss = onRejectDismiss
        )
    }

    debtToDelete?.let { debt ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_delete_debt_title),
            message = stringResource(R.string.kanban_confirm_delete_debt_message, debt.name),
            confirmText = stringResource(R.string.kanban_confirm_delete),
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
            title = stringResource(R.string.kanban_approve_task_title),
            message = stringResource(R.string.kanban_confirm_approve_message, task.title),
            confirmText = stringResource(R.string.kanban_confirm_approve),
            onConfirm = { onApproveConfirmed(task, accountId, direction) },
            onDismiss = onApproveDismiss
        )
    }

    paymentBill?.let { bill ->
        SciuroBottomSheet(onDismissRequest = onBillPaymentDismiss) {
            Text(stringResource(R.string.kanban_mark_bill_paid_title), style = MaterialTheme.typography.headlineSmall)
            Text("${bill.name} — RM ${"%.2f".format(bill.amount)}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            SciuroPrimaryButton(
                text = stringResource(R.string.kanban_confirm_payment),
                onClick = { onBillPaid(bill) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    paymentDebt?.let { debt ->
        SciuroBottomSheet(onDismissRequest = onDebtPaymentDismiss) {
            Text(stringResource(R.string.kanban_record_payment), style = MaterialTheme.typography.headlineSmall)
            Text("${debt.name} — RM ${"%.2f".format(debt.remainingBalance)} remaining", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            SciuroTextField(
                value = paymentAmountText,
                onValueChange = onPaymentAmountChange,
                label = stringResource(R.string.kanban_payment_amount_rm),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SciuroPrimaryButton(
                text = stringResource(R.string.kanban_apply_payment),
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
            title = stringResource(R.string.kanban_mark_debt_finished_title),
            message = stringResource(R.string.kanban_mark_finished_message),
            confirmText = stringResource(R.string.kanban_mark_finished),
            isDestructive = false,
            onConfirm = { onMarkFinishedConfirmed(debtId) },
            onDismiss = onMarkFinishedDismiss
        )
    }

    createBillAction?.let {
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_create_bill_title),
            message = stringResource(R.string.kanban_create_bill_message),
            confirmText = stringResource(R.string.kanban_confirm_create),
            onConfirm = onCreateBillConfirmed,
            onDismiss = onCreateBillDismiss
        )
    }

    createDebtAction?.let {
        SciuroConfirmationDialog(
            title = stringResource(R.string.kanban_create_debt_title),
            message = stringResource(R.string.kanban_create_debt_message),
            confirmText = stringResource(R.string.kanban_confirm_create),
            onConfirm = onCreateDebtConfirmed,
            onDismiss = onCreateDebtDismiss
        )
    }
}