package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sciuro.feature.wallet.model.WalletAccount

@OptIn(ExperimentalMaterial3Api::class)
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
    
    SciuroBottomSheet(
        onDismissRequest = onDismiss
    ) {
            Text("Add Manual Transaction", style = MaterialTheme.typography.headlineSmall)
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                val selectedAccount = accounts.find { it.id == selectedAccountId }
                SciuroTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = { },
                    readOnly = true,
                    label = "Account",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccountId = account.id
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            SciuroTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Amount (RM)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            
            SciuroTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = "Merchant / Description"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = direction == "OUTFLOW",
                    onClick = { direction = "OUTFLOW" },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                )
                FilterChip(
                    selected = direction == "INFLOW",
                    onClick = { direction = "INFLOW" },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
            
            SciuroPrimaryButton(
                text = "Save Transaction",
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
