package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import androidx.compose.ui.unit.dp
import com.sciuro.feature.settings.viewmodel.SettingsViewModel

@Composable
fun DeveloperTabDataTools(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Database Tools", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Inbox (Unreviewed)", color = MaterialTheme.colorScheme.onError)
                }
            }
        }

        if (showClearConfirmation) {
            SciuroConfirmationDialog(
                title = "Clear Inbox",
                message = "Delete all unreviewed transactions? This cannot be undone.",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    viewModel.clearInbox()
                    showClearConfirmation = false
                },
                onDismiss = { showClearConfirmation = false }
            )
        }
    }
}
