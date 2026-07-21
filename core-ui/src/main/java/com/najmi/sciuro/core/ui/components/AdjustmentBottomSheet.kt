package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.IBMPlexMono

val AdjustmentReasonPresets = listOf(
    "Lost Cash",
    "Gift Received",
    "Found Money",
    "Bank Error Correction",
    "ATM Discrepancy",
    "Rounding Correction",
    "Forgot to Log",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentBottomSheet(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, reason: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var reasonExpanded by remember { mutableStateOf(false) }
    var customReason by remember { mutableStateOf("") }
    val isCustom = reason == "Other"
    val effectiveReason = if (isCustom) customReason else reason

    val parsedAmount = amountStr.toDoubleOrNull()
    val displayDelta = parsedAmount
    val newBalance = if (parsedAmount != null) currentBalance + parsedAmount else currentBalance

    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Adjust Balance",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Current Balance: RM ${"%.2f".format(currentBalance)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (parsedAmount != null) {
            Text(
                "New Balance: RM ${"%.2f".format(newBalance)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = IBMPlexMono,
                color = if (parsedAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
            )
        }

        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Adjustment Amount (RM)") },
            placeholder = { Text("Positive = add, Negative = subtract") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = reasonExpanded,
            onExpandedChange = { reasonExpanded = !reasonExpanded }
        ) {
            OutlinedTextField(
                value = if (isCustom && customReason.isNotBlank()) customReason else reason.ifBlank { "Select reason..." },
                onValueChange = {},
                readOnly = !isCustom,
                label = { Text("Reason") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = reasonExpanded,
                onDismissRequest = { reasonExpanded = false }
            ) {
                AdjustmentReasonPresets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = {
                            reason = preset
                            if (preset != "Other") customReason = ""
                            reasonExpanded = false
                        }
                    )
                }
            }
        }

        if (isCustom) {
            OutlinedTextField(
                value = customReason,
                onValueChange = { customReason = it },
                label = { Text("Custom Reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (parsedAmount != null && effectiveReason.isNotBlank()) {
                        onConfirm(parsedAmount, effectiveReason)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = parsedAmount != null && parsedAmount != 0.0 && effectiveReason.isNotBlank()
            ) {
                Text("Save Adjustment")
            }
        }
    }
}
