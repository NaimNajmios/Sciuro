package com.sciuro.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.BrandPrimaryDark
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.sciuro.feature.settings.viewmodel.ConnectionTestState
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import android.os.Build
import android.os.PowerManager
import com.najmi.sciuro.core.ui.util.OemAutostartHelper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.sciuro.feature.settings.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {},
    onNavigateToLinkedAccounts: () -> Unit = {},
    onExportBackup: (String) -> Unit = {},
    onImportBackup: (Uri, String) -> Unit = { _, _ -> },
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val themePref by themeManager.themePreference.collectAsState()

    var showLlmFields by remember { mutableStateOf(uiState.isLlmEnabled) }
    var showQuietHoursPicker by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableIntStateOf(0) }
    val snackbarHostState = LocalSnackbarHostState.current
    val devGateSnackbarText = stringResource(R.string.dev_gate_snackbar)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLlmEnabled) {
        showLlmFields = uiState.isLlmEnabled
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.settings_title),
            heroFigure = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = BrandPrimaryDark) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {}
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Section: Appearance
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val themeLabels = listOf(
                                stringResource(R.string.settings_theme_system),
                                stringResource(R.string.settings_theme_light),
                                stringResource(R.string.settings_theme_dark)
                            )
                            val themeSystemLabel = stringResource(R.string.settings_theme_system)
                            val themeLightLabel = stringResource(R.string.settings_theme_light)
                            val themeDarkLabel = stringResource(R.string.settings_theme_dark)
                            val selectedLabel = when (themePref) {
                                ThemePreference.SYSTEM_DEFAULT -> themeSystemLabel
                                ThemePreference.LIGHT -> themeLightLabel
                                ThemePreference.DARK -> themeDarkLabel
                            }
                            PillToggle(
                                options = themeLabels,
                                selectedOption = selectedLabel,
                                onOptionSelected = { label ->
                                    val pref = when (label) {
                                        themeSystemLabel -> ThemePreference.SYSTEM_DEFAULT
                                        themeLightLabel -> ThemePreference.LIGHT
                                        themeDarkLabel -> ThemePreference.DARK
                                        else -> ThemePreference.SYSTEM_DEFAULT
                                    }
                                    themeManager.setTheme(pref)
                                },
                                fillWidth = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Section: Background Reliability
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_background_reliability))
                }

                item {
                    val isBatteryExempt = remember {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                            pm.isIgnoringBatteryOptimizations(context.packageName)
                        } else {
                            true
                        }
                    }
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (isBatteryExempt) stringResource(R.string.settings_battery_optimization_disabled)
                                else stringResource(R.string.settings_battery_optimization_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            if (!isBatteryExempt) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.settings_disable_battery_optimization))
                                }
                            }
                            val autostartIntent = remember { OemAutostartHelper.getAutostartIntent() }
                            if (autostartIntent != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(autostartIntent)
                                        } catch (_: Exception) { }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.settings_open_autostart))
                                }
                            }
                        }
                    }
                }

                // Quiet Hours (under Reliability)
                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQuietHoursPicker = !showQuietHoursPicker },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_section_quiet_hours), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (uiState.isQuietHoursEnabled) stringResource(R.string.settings_quiet_hours_suppressed, uiState.quietHoursStart, uiState.quietHoursEnd)
                                        else stringResource(R.string.settings_quiet_hours_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (showQuietHoursPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = showQuietHoursPicker,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(R.string.settings_section_quiet_hours), style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = uiState.isQuietHoursEnabled,
                                            onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                                        )
                                    }
                                    if (uiState.isQuietHoursEnabled) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(stringResource(R.string.settings_quiet_hours_start), style = MaterialTheme.typography.labelSmall)
                                                Text("${uiState.quietHoursStart}:00", style = MaterialTheme.typography.titleMedium)
                                                Row {
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursStart > 0) viewModel.setQuietHoursStart(uiState.quietHoursStart - 1)
                                                    }) { Text("\u2212") }
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursStart < 23) viewModel.setQuietHoursStart(uiState.quietHoursStart + 1)
                                                    }) { Text("+") }
                                                }
                                            }
                                            Text(stringResource(R.string.settings_quiet_hours_to), style = MaterialTheme.typography.bodyMedium)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(stringResource(R.string.settings_quiet_hours_end), style = MaterialTheme.typography.labelSmall)
                                                Text("${uiState.quietHoursEnd}:00", style = MaterialTheme.typography.titleMedium)
                                                Row {
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursEnd > 0) viewModel.setQuietHoursEnd(uiState.quietHoursEnd - 1)
                                                    }) { Text("\u2212") }
                                                    IconButton(onClick = {
                                                        if (uiState.quietHoursEnd < 23) viewModel.setQuietHoursEnd(uiState.quietHoursEnd + 1)
                                                    }) { Text("+") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Security
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_security))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_lock_app_on_launch), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.settings_lock_app_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = uiState.isLockEnabled,
                                onCheckedChange = { viewModel.setLockEnabled(it) }
                            )
                        }
                    }
                }

                // Section: Data & Accounts
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_data_backup))
                }

                item {
                    var showExportDialog by remember { mutableStateOf(false) }
                    var importFileUri by remember { mutableStateOf<Uri?>(null) }

                    val importFilePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        importFileUri = uri
                    }

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

                // Section: Intelligence & Automation
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_llm))
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.settings_llm_toggle), style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = uiState.isLlmEnabled,
                                    onCheckedChange = {
                                        viewModel.setLlmEnabled(it)
                                        showLlmFields = it
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = showLlmFields,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SciuroTextField(
                                        value = uiState.apiKey,
                                        onValueChange = { viewModel.setApiKey(it) },
                                        label = stringResource(R.string.settings_llm_api_key),
                                        singleLine = true,
                                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        trailingIcon = {
                                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Text(
                                    text = if (apiKeyVisible) stringResource(R.string.settings_hide) else stringResource(R.string.settings_show),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    SciuroTextField(
                                        value = uiState.llmModelName,
                                        onValueChange = { viewModel.setLlmModelName(it) },
                                        label = stringResource(R.string.settings_llm_model),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { viewModel.testConnection() },
                                            enabled = uiState.apiKey.isNotBlank() && uiState.connectionTestState !is ConnectionTestState.Testing
                                        ) {
                                            Text(stringResource(R.string.settings_test_connection))
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        when (val state = uiState.connectionTestState) {
                                            is ConnectionTestState.Testing -> {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            }
                                            is ConnectionTestState.Success -> {
                                                Text(
                                                    text = stringResource(R.string.settings_connection_success),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            is ConnectionTestState.Error -> {
                                                Text(
                                                    text = state.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            is ConnectionTestState.Idle -> { }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_auto_confirm_recurring), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.settings_auto_confirm_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = uiState.isObligationAutoConfirmEnabled,
                                onCheckedChange = { viewModel.setObligationAutoConfirmEnabled(it) }
                            )
                        }
                    }
                }

                // Section: Developer
                item {
                    SettingsSectionHeader(
                        title = stringResource(R.string.settings_developer_options),
                        modifier = Modifier.clickable {
                            developerTapCount++
                            if (developerTapCount >= 7) {
                                developerTapCount = 0
                                viewModel.setDeveloperOptionsVisible(true)
                                scope.launch {
                                    snackbarHostState.showSnackbar(devGateSnackbarText)
                                }
                            }
                        }
                    )
                }

                if (uiState.isDeveloperOptionsVisible) {
                    item {
                        SciuroCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            onClick = onNavigateToDeveloperSettings
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.settings_developer_options), style = MaterialTheme.typography.titleMedium)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.settings_developer_options))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
    )
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
