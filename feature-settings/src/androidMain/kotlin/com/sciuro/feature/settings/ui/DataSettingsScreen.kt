package com.sciuro.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.BrandPrimaryDark
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DataSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DataSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLinkedAccounts: () -> Unit = {},
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToMerchantRules: () -> Unit = {},
    onExportBackup: (String) -> Unit = {},
    onImportBackup: (Uri, String) -> Unit = { _, _ -> },
    viewModel: DataSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        importFileUri = uri
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.data_settings_title),
            heroFigure = { Text(stringResource(R.string.data_settings_title), style = MaterialTheme.typography.headlineLarge, color = BrandPrimaryDark) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.linked_accounts_back),
                        tint = BrandPrimaryDark
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
            ) {
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_data_backup))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.settings_data_backup_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.settings_export))
                                }
                                OutlinedButton(
                                    onClick = { importFilePickerLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.settings_import))
                                }
                            }
                        }
                    }
                }

                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToLinkedAccounts
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_linked_accounts), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_linked_accounts))
                        }
                    }
                }

                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToCategorySettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_manage_categories), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_manage_categories))
                        }
                    }
                }

                item {
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = onNavigateToMerchantRules
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_manage_merchant_rules), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_manage_merchant_rules))
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(stringResource(R.string.settings_budget_warning_threshold), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = uiState.budgetWarningThreshold,
                                    onValueChange = { viewModel.setBudgetWarningThreshold(it) },
                                    valueRange = 0.5f..1.0f,
                                    steps = 9,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${(uiState.budgetWarningThreshold * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        BackupPasswordDialog(
            title = stringResource(R.string.settings_backup_export_title),
            onConfirm = { showExportDialog = false; onExportBackup(it) },
            onDismiss = { showExportDialog = false }
        )
    }
    importFileUri?.let { uri ->
        BackupPasswordDialog(
            title = stringResource(R.string.settings_backup_import_title),
            onConfirm = { password ->
                importFileUri = null
                onImportBackup(uri, password)
            },
            onDismiss = { importFileUri = null }
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.settings_backup_passphrase_description))
                Spacer(modifier = Modifier.height(12.dp))
                SciuroTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.settings_backup_passphrase),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotBlank()) onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
