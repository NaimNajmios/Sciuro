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
    onConfirm: (amount: Double, reason: String, remark: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var reasonExpanded by remember { mutableStateOf(false) }
    var customReason by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    val isCustom = reason == "Other"
    val effectiveReason = if (isCustom) customReason else reason

    val newBalance = amountStr.toDoubleOrNull()
    val delta = if (newBalance != null) newBalance - currentBalance else null
    val isLargeVariance = delta != null && kotlin.math.abs(delta) >= 50.0

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

        if (delta != null && kotlin.math.abs(delta) > 0.01) {
            val varianceColor = if (delta >= 0) com.najmi.sciuro.core.ui.theme.SignalIncome else com.najmi.sciuro.core.ui.theme.SignalDanger
            Text(
                "Variance: ${if (delta >= 0) "+" else ""}RM ${"%.2f".format(delta)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = IBMPlexMono,
                color = varianceColor
            )
        }

        SciuroTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = "New Balance (RM)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        ExposedDropdownMenuBox(
            expanded = reasonExpanded,
            onExpandedChange = { reasonExpanded = !reasonExpanded }
        ) {
            SciuroTextField(
                value = if (isCustom && customReason.isNotBlank()) customReason else reason.ifBlank { "Select reason..." },
                onValueChange = {},
                readOnly = !isCustom,
                label = "Reason",
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                modifier = Modifier.menuAnchor()
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
            SciuroTextField(
                value = customReason,
                onValueChange = { customReason = it },
                label = "Custom Reason"
            )
        }

        if (isLargeVariance) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Large variance detected. Add a note to help future reconciliation:",
                style = MaterialTheme.typography.bodySmall,
                color = com.najmi.sciuro.core.ui.theme.SignalWarning
            )
            Spacer(modifier = Modifier.height(4.dp))
            SciuroTextField(
                value = remark,
                onValueChange = { remark = it },
                label = "Remark (optional)"
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
                    if (delta != null && effectiveReason.isNotBlank()) {
                        onConfirm(delta, effectiveReason, remark.takeIf { it.isNotBlank() })
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = delta != null && kotlin.math.abs(delta) > 0.01 && effectiveReason.isNotBlank()
            ) {
                Text("Save Adjustment")
            }
        }
    }
}

