package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.core.ledger.db.Account
import com.sciuro.core.ledger.model.Category

@OptIn(ExperimentalMaterial3Api::class)
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
    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Text("Edit Transaction", style = MaterialTheme.typography.headlineSmall)
        
        SciuroTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Amount (RM)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        SciuroTextField(
            value = merchant,
            onValueChange = onMerchantChange,
            label = "Merchant / Note"
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
        
        var accountExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = it }
        ) {
            val selAcc = accounts.find { it.id == accountId }
            SciuroTextField(
                value = selAcc?.name ?: "Select Account",
                onValueChange = {},
                readOnly = true,
                label = "Wallet Account",
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
        
        Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val relevantCategories = if (direction == "OUTFLOW") expenseCategories else incomeCategories
            items(relevantCategories) { cat ->
                FilterChip(
                    selected = categoryId == cat.id,
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
                Text("Delete")
            }
            
            SciuroPrimaryButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = amount.isNotBlank() && accountId != null
            )
        }
    }
}
