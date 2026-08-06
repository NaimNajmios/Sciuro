package com.sciuro.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import com.najmi.sciuro.core.ui.util.SciuroIcons

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroNavigationCard
import com.najmi.sciuro.core.ui.components.SciuroSectionHeader
import com.najmi.sciuro.core.ui.components.SheetList

import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DataSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DataSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLinkedAccounts: () -> Unit = {},
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToMerchantRules: () -> Unit = {},
    onNavigateToInvestmentPrice: () -> Unit = {},
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
            heroFigure = { Text(stringResource(R.string.data_settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.linked_accounts_back),
                        tint = MaterialTheme.colorScheme.onPrimary
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
                    SciuroSectionHeader(stringResource(R.string.settings_section_data_backup), icon = SciuroIcons.Info)
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
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.settings_export))
                                }
                                OutlinedButton(
                                    onClick = { importFilePickerLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.settings_import))
                                }
                            }
                        }
                    }
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_linked_accounts),
                        summary = "",
                        leadingIcon = Icons.Filled.Settings,
                        onClick = onNavigateToLinkedAccounts
                    )
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_manage_categories),
                        summary = "",
                        leadingIcon = Icons.Filled.Settings,
                        onClick = onNavigateToCategorySettings
                    )
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_manage_merchant_rules),
                        summary = "",
                        leadingIcon = Icons.Filled.Settings,
                        onClick = onNavigateToMerchantRules
                    )
                }

                item {
                    SciuroNavigationCard(
                        title = stringResource(R.string.settings_manage_investment_prices),
                        summary = "",
                        leadingIcon = Icons.Filled.Settings,
                        onClick = onNavigateToInvestmentPrice
                    )
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

