package com.sciuro.feature.kanban.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.feature.kanban.R
import com.sciuro.feature.kanban.model.DebtTask

@Composable
fun EditDebtSheet(
    debt: DebtTask,
    onDismiss: () -> Unit,
    onEdit: (name: String, principalAmount: Double, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(debt.name) }
    var amountText by remember { mutableStateOf(debt.debt.principalAmount.toString()) }
    var notes by remember { mutableStateOf(debt.debt.notes ?: "") }

    SciuroFormSheet(
        title = stringResource(R.string.kanban_edit_debt_title),
        onDismissRequest = onDismiss
    ) {
        SciuroTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.kanban_label_debt_name)
        )

        Spacer(modifier = Modifier.height(8.dp))
        SciuroAmountField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.kanban_label_principal_amount)
        )

        Spacer(modifier = Modifier.height(8.dp))
        SciuroTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.kanban_label_notes_optional)
        )

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
