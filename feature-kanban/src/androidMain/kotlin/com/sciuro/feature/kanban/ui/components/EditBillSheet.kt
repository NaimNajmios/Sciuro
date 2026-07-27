package com.sciuro.feature.kanban.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.obligations.model.ObligationFrequency
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.PillToggle
import com.sciuro.feature.kanban.R
import com.sciuro.feature.kanban.model.BillTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillSheet(
    bill: BillTask,
    accounts: List<Account>,
    expenseCategories: List<Category>,
    onDismiss: () -> Unit,
    onEdit: (name: String, amount: Double, frequency: ObligationFrequency, nextDueDate: Long, categoryId: String?, accountId: String?) -> Unit
) {
    var name by remember { mutableStateOf(bill.name) }
    var amountText by remember { mutableStateOf(bill.amount.toString()) }
    var frequency by remember { mutableStateOf(bill.obligation.frequency) }
    var dueDate by remember { mutableStateOf<Long?>(bill.obligation.nextDueDate) }
    var categoryId by remember { mutableStateOf<String?>(bill.obligation.categoryId) }
    var selectedAccount by remember { mutableStateOf<Account?>(accounts.find { it.id == bill.obligation.accountId }) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    SciuroFormSheet(
        title = stringResource(R.string.kanban_edit_bill_title),
        onDismissRequest = onDismiss
    ) {
        SciuroTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.kanban_label_name)
        )

        Spacer(modifier = Modifier.height(8.dp))
        SciuroAmountField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.kanban_label_amount_rm)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.kanban_label_frequency), style = MaterialTheme.typography.labelLarge)
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
        Text(stringResource(R.string.kanban_label_next_due_date), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dueDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.kanban_select_date),
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.kanban_pick_date))
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
                    }) { Text(stringResource(R.string.kanban_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.kanban_cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.kanban_label_account), style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = accountDropdownExpanded,
            onExpandedChange = { accountDropdownExpanded = it }
        ) {
            SciuroTextField(
                value = selectedAccount?.name ?: stringResource(R.string.kanban_select_account),
                onValueChange = {},
                readOnly = true,
                label = stringResource(R.string.kanban_wallet_account),
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

        Spacer(modifier = Modifier.height(24.dp))

        SciuroPrimaryButton(
            text = stringResource(R.string.kanban_save_changes),
            onClick = {
                onEdit(
                    name,
                    amountText.toDoubleOrNull() ?: 0.0,
                    frequency,
                    dueDate ?: 0L,
                    categoryId,
                    selectedAccount?.id
                )
            },
            enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && dueDate != null && selectedAccount != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
