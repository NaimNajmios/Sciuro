package com.sciuro.feature.wallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroAmountField
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.sciuro.feature.wallet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    accounts: List<com.sciuro.core.ledger.db.Account>,
    editTxAmount: String,
    onAmountChange: (String) -> Unit,
    editTxMerchant: String,
    onMerchantChange: (String) -> Unit,
    editTxDirection: String,
    onDirectionChange: (String) -> Unit,
    editTxCategoryId: String?,
    onCategoryIdChange: (String?) -> Unit,
    editTxAccountId: String?,
    onAccountIdChange: (String?) -> Unit,
    expenseCategories: List<com.sciuro.core.ledger.model.Category>,
    incomeCategories: List<com.sciuro.core.ledger.model.Category>,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.wallet_edit_transaction), style = MaterialTheme.typography.headlineSmall)

        SciuroAmountField(
            value = editTxAmount,
            onValueChange = onAmountChange,
            label = stringResource(R.string.wallet_amount_rm)
        )

        SciuroTextField(
            value = editTxMerchant,
            onValueChange = onMerchantChange,
            label = stringResource(R.string.wallet_merchant_note)
        )

        PillToggle(
            options = listOf("Expense", "Income"),
            selectedOption = if (editTxDirection == "OUTFLOW") "Expense" else "Income",
            onOptionSelected = { label ->
                onDirectionChange(if (label == "Expense") "OUTFLOW" else "INFLOW")
                onCategoryIdChange(null)
            },
            fillWidth = true,
            modifier = Modifier.fillMaxWidth()
        )

        var accountExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = it }
        ) {
            val selAcc = accounts.find { it.id == editTxAccountId }
            SciuroTextField(
                value = selAcc?.name ?: stringResource(R.string.wallet_select_account),
                onValueChange = {},
                readOnly = true,
                label = stringResource(R.string.wallet_wallet_account),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = accountExpanded,
                onDismissRequest = { accountExpanded = false }
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name) },
                        onClick = {
                            onAccountIdChange(acc.id)
                            accountExpanded = false
                        }
                    )
                }
            }
        }

        Text(stringResource(R.string.wallet_category), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val relevantCategories = if (editTxDirection == "OUTFLOW") expenseCategories else incomeCategories

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(relevantCategories) { cat ->
                FilterChip(
                    selected = editTxCategoryId == cat.id,
                    onClick = { onCategoryIdChange(cat.id) },
                    label = { Text(cat.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.wallet_delete))
            }

            SciuroPrimaryButton(
                text = stringResource(R.string.wallet_save),
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = editTxAmount.isNotBlank() && editTxAccountId != null
            )
        }
    }
}
