package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroFormSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.sciuro.core.ledger.db.Account
import com.sciuro.core.ledger.model.Category
import com.sciuro.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionSheet(
    amount: String,
    onAmountChange: (String) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    direction: String,
    onDirectionChange: (String) -> Unit,
    categoryId: String?,
    onCategoryIdChange: (String?) -> Unit,
    accountId: String?,
    onAccountIdChange: (String?) -> Unit,
    accounts: List<Account>,
    expenseCategories: List<Category>,
    incomeCategories: List<Category>,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    SciuroFormSheet(
        title = stringResource(R.string.dashboard_edit_transaction),
        onDismissRequest = onDismiss
    ) {
        SciuroAmountField(
            value = amount,
            onValueChange = onAmountChange,
            label = stringResource(R.string.dashboard_amount_label),
            imeAction = ImeAction.Next,
            modifier = Modifier.focusRequester(focusRequester)
        )
        
        SciuroTextField(
            value = merchant,
            onValueChange = onMerchantChange,
            label = stringResource(R.string.dashboard_merchant_label),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (amount.isNotBlank() && accountId != null) onSave()
            })
        )
        
        PillToggle(
            options = listOf("Expense", "Income"),
            selectedOption = if (direction == "OUTFLOW") "Expense" else "Income",
            onOptionSelected = { label ->
                onDirectionChange(if (label == "Expense") "OUTFLOW" else "INFLOW")
                onCategoryIdChange(null)
            },
            fillWidth = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(stringResource(R.string.dashboard_wallet_account), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                FilterChip(
                    selected = accountId == acc.id,
                    onClick = { onAccountIdChange(acc.id) },
                    label = { Text(acc.name) }
                )
            }
        }
        
        Text(stringResource(R.string.dashboard_category), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val relevantCategories = if (direction == "OUTFLOW") expenseCategories else incomeCategories
            relevantCategories.forEach { cat ->
                FilterChip(
                    selected = categoryId == cat.id,
                    onClick = { onCategoryIdChange(cat.id) },
                    label = { Text(cat.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SciuroPrimaryButton(
            text = stringResource(R.string.dashboard_save),
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = amount.isNotBlank() && accountId != null
        )

        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.dashboard_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}
