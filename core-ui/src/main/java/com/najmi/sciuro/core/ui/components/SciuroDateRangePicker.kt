package com.najmi.sciuro.core.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SciuroDateRangePicker(
    dateRangePickerState: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    modifier: Modifier = Modifier
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    dateRangePickerState.selectedStartDateMillis,
                    dateRangePickerState.selectedEndDateMillis
                )
            }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.weight(1f)
        )
    }
}
