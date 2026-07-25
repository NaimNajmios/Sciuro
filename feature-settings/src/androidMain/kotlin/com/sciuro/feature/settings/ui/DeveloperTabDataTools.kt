package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel

@Composable
fun DeveloperTabDataTools(
    viewModel: DeveloperSettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        SciuroCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dev_data_tools_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.dev_data_tools_clear_inbox), color = MaterialTheme.colorScheme.onError)
                }
            }
        }

        if (showClearConfirmation) {
            SciuroConfirmationDialog(
                title = stringResource(R.string.dev_data_tools_clear_title),
                message = stringResource(R.string.dev_data_tools_clear_confirm),
                confirmText = stringResource(R.string.dev_data_tools_delete),
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
