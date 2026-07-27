package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.PillToggle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import com.sciuro.feature.wallet.R
import com.sciuro.feature.wallet.model.WalletAccount

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    accounts: List<WalletAccount>,
    onDismiss: () -> Unit,
    onSave: (accountId: String, amount: Double, direction: String, merchant: String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("OUTFLOW") }
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    SciuroFormSheet(
        title = stringResource(R.string.wallet_add_manual_transaction),
        onDismissRequest = onDismiss
    ) {
            
            Text(stringResource(R.string.wallet_account), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id },
                        label = { Text(account.name) }
                    )
                }
            }
            
            SciuroAmountField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(R.string.wallet_amount_rm),
                imeAction = ImeAction.Next,
                modifier = Modifier.focusRequester(focusRequester)
            )
            
            SciuroTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = stringResource(R.string.wallet_merchant_description),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (selectedAccountId.isNotBlank() && parsedAmount > 0) {
                        onSave(selectedAccountId, parsedAmount, direction, merchant)
                        onDismiss()
                    }
                })
            )
            
            PillToggle(
                options = listOf("Expense", "Income"),
                selectedOption = if (direction == "OUTFLOW") "Expense" else "Income",
                onOptionSelected = { direction = if (it == "Expense") "OUTFLOW" else "INFLOW" },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            SciuroPrimaryButton(
                text = stringResource(R.string.wallet_save_transaction),
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (selectedAccountId.isNotBlank() && parsedAmount > 0) {
                        onSave(selectedAccountId, parsedAmount, direction, merchant)
                        onDismiss()
                    }
                },
                enabled = selectedAccountId.isNotBlank() && amount.isNotBlank()
            )
    }
}
