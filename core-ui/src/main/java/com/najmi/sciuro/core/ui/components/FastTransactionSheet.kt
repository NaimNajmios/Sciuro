package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class FastTxOption(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTransactionSheet(
    accounts: List<FastTxOption>,
    expenseCategories: List<FastTxOption>,
    incomeCategories: List<FastTxOption>,
    onDismissRequest: () -> Unit,
    onSubmit: (amount: Double, direction: String, merchant: String, categoryId: String?, accountId: String?, destinationAccountId: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf("0") }
    var direction by remember { mutableStateOf("OUTFLOW") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var destinationAccountId by remember { mutableStateOf<String?>(null) }
    var merchant by remember { mutableStateOf("") }
    val presetLabels = listOf("Breakfast", "Lunch", "Dinner", "Coffee", "Groceries", "Transport", "Shopping", "Salary", "Others")

    SciuroBottomSheet(onDismissRequest = onDismissRequest) {
        // Amount Display
        Text(
            text = "RM $amountStr",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = if (direction == "OUTFLOW") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )

        // Direction Toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = direction == "OUTFLOW",
                onClick = { direction = "OUTFLOW"; categoryId = null },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Expense")
            }
            SegmentedButton(
                selected = direction == "INFLOW",
                onClick = { direction = "INFLOW"; categoryId = null },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Income")
            }
            SegmentedButton(
                selected = direction == "TRANSFER",
                onClick = { direction = "TRANSFER"; categoryId = null },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("Transfer")
            }
        }

        // Description / Label
        SciuroTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = "Description / Label",
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetLabels) { label ->
                FilterChip(
                    selected = merchant == label,
                    onClick = { merchant = label },
                    label = { Text(label) }
                )
            }
        }

        // Category Selection
        if (direction != "TRANSFER") {
            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cats = if (direction == "OUTFLOW") expenseCategories else incomeCategories
                items(cats) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        // Account Selection
        Text(if (direction == "TRANSFER") "Source Account" else "Account", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { acc ->
                FilterChip(
                    selected = accountId == acc.id,
                    onClick = { accountId = acc.id },
                    label = { Text(acc.name) }
                )
            }
        }

        if (direction == "TRANSFER") {
            Text("Destination Account", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts.filter { it.id != accountId }) { acc ->
                    FilterChip(
                        selected = destinationAccountId == acc.id,
                        onClick = { destinationAccountId = acc.id },
                        label = { Text(acc.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Numpad
        Numpad(
            onNumberClick = { num ->
                if (amountStr == "0") {
                    amountStr = num
                } else if (!amountStr.contains('.') || amountStr.substringAfter('.').length < 2) {
                    amountStr += num
                }
            },
            onDecimalClick = {
                if (!amountStr.contains('.')) amountStr += "."
            },
            onBackspaceClick = {
                if (amountStr.length > 1) {
                    amountStr = amountStr.dropLast(1)
                } else {
                    amountStr = "0"
                }
            },
            onSaveClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    val finalMerchant = merchant.ifBlank { "Manual Entry" }
                    onSubmit(amt, direction, finalMerchant, categoryId, accountId, destinationAccountId)
                }
            },
            isSaveEnabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0 && accountId != null && (if (direction == "TRANSFER") destinationAccountId != null else categoryId != null)
        )
    }
}

@Composable
fun Numpad(
    onNumberClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaveEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (num in row) {
                    NumpadButton(text = num, onClick = { onNumberClick(num) }, modifier = Modifier.weight(1f))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadButton(text = ".", onClick = onDecimalClick, modifier = Modifier.weight(1f))
            NumpadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f))
            NumpadButton(text = "⌫", onClick = onBackspaceClick, modifier = Modifier.weight(1f))
        }
        
        SciuroPrimaryButton(
            text = "Save Transaction",
            onClick = onSaveClick,
            enabled = isSaveEnabled,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
