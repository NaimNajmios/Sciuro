package com.sciuro.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.sciuro.feature.settings.R
import java.text.DateFormat
import java.util.Date

@Composable
fun DatabaseRecoveryScreen(
    quarantineCount: Int,
    lastQuarantineTimestamp: Long,
    onImportBackup: (Uri, String) -> Unit,
    onStartFresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    var showStartFreshConfirm by remember { mutableStateOf(false) }

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        importFileUri = uri
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.recovery_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.recovery_screen_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))
                SciuroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.recovery_screen_preserved_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RecoveryRow(
                            label = stringResource(R.string.recovery_screen_quarantine_count),
                            value = quarantineCount.toString()
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        RecoveryRow(
                            label = stringResource(R.string.recovery_screen_last_quarantine),
                            value = if (lastQuarantineTimestamp > 0) {
                                formatTimestamp(lastQuarantineTimestamp)
                            } else {
                                stringResource(R.string.recovery_screen_never)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { importFilePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.recovery_screen_import))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showStartFreshConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.recovery_screen_start_fresh))
                }
            }
        }
    }

    importFileUri?.let { uri ->
        BackupPasswordDialog(
            title = stringResource(R.string.recovery_screen_import_title),
            onConfirm = { password ->
                importFileUri = null
                onImportBackup(uri, password)
            },
            onDismiss = { importFileUri = null }
        )
    }

    if (showStartFreshConfirm) {
        AlertDialog(
            onDismissRequest = { showStartFreshConfirm = false },
            title = { Text(stringResource(R.string.recovery_screen_start_fresh_title)) },
            text = { Text(stringResource(R.string.recovery_screen_start_fresh_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartFreshConfirm = false
                        onStartFresh()
                    }
                ) {
                    Text(stringResource(R.string.recovery_screen_start_fresh_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartFreshConfirm = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }
}

@Composable
private fun RecoveryRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
