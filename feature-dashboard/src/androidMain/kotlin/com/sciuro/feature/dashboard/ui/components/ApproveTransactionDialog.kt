package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.sciuro.core.ledger.db.Account
import com.sciuro.feature.dashboard.R

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
        title = { Text(stringResource(R.string.dashboard_approve_transaction)) },
        text = {
            Column {
                Text(stringResource(R.string.dashboard_select_account_for_transaction))
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val selAcc = accounts.find { it.id == selectedAccountId }
                    SciuroTextField(
                        value = selAcc?.name ?: stringResource(R.string.dashboard_select_account),
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.dashboard_wallet_account),
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
                Text(stringResource(R.string.dashboard_approve))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dashboard_cancel))
            }
        }
    )
}
