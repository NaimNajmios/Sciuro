package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.core.ledger.db.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveTransactionDialog(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    var accountExpanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Transaction") },
        text = {
            Column {
                Text("Select an account for this transaction:")
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val selAcc = accounts.find { it.id == selectedAccountId }
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
                                    onAccountSelected(acc.id)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                enabled = selectedAccountId != null
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
