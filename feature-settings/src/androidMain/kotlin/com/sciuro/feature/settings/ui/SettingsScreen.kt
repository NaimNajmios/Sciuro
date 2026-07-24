package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SheetList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import java.net.HttpURLConnection
import java.net.URL
import com.najmi.sciuro.core.ui.util.OemAutostartHelper

@Composable
fun SettingsScreen(
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {},
    onNavigateToLinkedAccounts: () -> Unit = {},
    onExportBackup: (String) -> Unit = {},
    onImportBackup: (String) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    settingsProvider: SettingsProvider = koinInject()
) {
    var isLlmOptIn by rememberSaveable { mutableStateOf(settingsProvider.isLlmEnabled()) }
    var isLockEnabled by rememberSaveable { mutableStateOf(settingsProvider.isLockEnabled()) }
    var isAutoConfirmEnabled by rememberSaveable { mutableStateOf(settingsProvider.isAutoConfirmEnabled()) }
    var apiKey by rememberSaveable { mutableStateOf(settingsProvider.getApiKey() ?: "") }
    var testStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var llmModelName by rememberSaveable { mutableStateOf(settingsProvider.getLlmModelName()) }
    var budgetThreshold by rememberSaveable { mutableStateOf(settingsProvider.getBudgetWarningThreshold()) }
    var isTransactionAutoConfirm by rememberSaveable { mutableStateOf(settingsProvider.isTransactionAutoConfirmEnabled()) }
    var isTrustValidatedLlm by rememberSaveable { mutableStateOf(settingsProvider.isTrustValidatedLlmEnabled()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val themePref by themeManager.themePreference.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Settings",
            heroFigure = { Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Color.White) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {}
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Text(
                        "Application Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-confirm transactions", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Auto-book transactions with high confidence; undoable for 24h",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isTransactionAutoConfirm,
                                    onCheckedChange = {
                                        isTransactionAutoConfirm = it
                                        settingsProvider.setTransactionAutoConfirmEnabled(it)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Trust validated LLM results", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Accept LLM-parsed drafts when they pass amount and merchant validation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isTrustValidatedLlm,
                                    onCheckedChange = {
                                        isTrustValidatedLlm = it
                                        settingsProvider.setTrustValidatedLlmEnabled(it)
                                    }
                                )
                            }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Appearance", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            val themeLabels = listOf("System", "Light", "Dark")
                            val selectedLabel = when (themePref) {
                                ThemePreference.SYSTEM_DEFAULT -> "System"
                                ThemePreference.LIGHT -> "Light"
                                ThemePreference.DARK -> "Dark"
                            }
                            PillToggle(
                                options = themeLabels,
                                selectedOption = selectedLabel,
                                onOptionSelected = { label ->
                                    val pref = when (label) {
                                        "System" -> ThemePreference.SYSTEM_DEFAULT
                                        "Light" -> ThemePreference.LIGHT
                                        "Dark" -> ThemePreference.DARK
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

                item {
                    val isBatteryExempt = remember {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                            pm.isIgnoringBatteryOptimizations(context.packageName)
                        } else {
                            true
                        }
                    }
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Background Reliability", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isBatteryExempt) "Battery optimization is disabled. Sciuro can run reliably in the background."
                                else "Battery optimization is active and may interrupt background notification capture.",
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
                                    Text("Disable Battery Optimization")
                                }
                            }
                            val autostartIntent = remember { OemAutostartHelper.getAutostartIntent() }
                            if (autostartIntent != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(autostartIntent)
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Autostart Settings")
                                }
                            }
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Security", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Lock app on launch", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Require biometric or device PIN to open Sciuro",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isLockEnabled,
                                    onCheckedChange = {
                                        isLockEnabled = it
                                        settingsProvider.setLockEnabled(it)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    var showExportDialog by rememberSaveable { mutableStateOf(false) }
                    var showImportDialog by rememberSaveable { mutableStateOf(false) }
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Data Backup", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Encrypted export (AES-256-GCM) and import with pre-import backup. Exports are saved to app storage.",
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
                                    Text("Export")
                                }
                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Import")
                                }
                            }
                        }
                    }
                    if (showExportDialog) {
                        BackupPasswordDialog(
                            title = "Export Encrypted Backup",
                            onConfirm = { showExportDialog = false; onExportBackup(it) },
                            onDismiss = { showExportDialog = false }
                        )
                    }
                    if (showImportDialog) {
                        BackupPasswordDialog(
                            title = "Import Encrypted Backup", 
                            onConfirm = { showImportDialog = false; onImportBackup(it) },
                            onDismiss = { showImportDialog = false }
                        )
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("LLM Classification", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Use Groq Llama 3 for Fallback", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = isLlmOptIn,
                                    onCheckedChange = {
                                        isLlmOptIn = it
                                        settingsProvider.setLlmEnabled(it)
                                    }
                                )
                            }

                            if (isLlmOptIn) {
                                Spacer(modifier = Modifier.height(16.dp))
                                com.najmi.sciuro.core.ui.components.SciuroTextField(
                                    value = apiKey,
                                    onValueChange = {
                                        apiKey = it
                                        settingsProvider.setApiKey(it)
                                    },
                                    label = "Groq API Key",
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                com.najmi.sciuro.core.ui.components.SciuroTextField(
                                    value = llmModelName,
                                    onValueChange = {
                                        llmModelName = it
                                        settingsProvider.setLlmModelName(it)
                                    },
                                    label = "Groq LLM Model",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            testStatus = "Testing..."
                                            scope.launch {
                                                testStatus = withContext(Dispatchers.IO) {
                                                    try {
                                                        val url = URL("https://api.groq.com/openai/v1/models")
                                                        val connection = url.openConnection() as HttpURLConnection
                                                        connection.requestMethod = "GET"
                                                        connection.setRequestProperty("Authorization", "Bearer $apiKey")
                                                        connection.connectTimeout = 5000
                                                        connection.readTimeout = 5000

                                                        when (connection.responseCode) {
                                                            200 -> "Success! Connection established."
                                                            401 -> "Error: Invalid API Key."
                                                            else -> "Error: ${connection.responseCode}"
                                                        }
                                                    } catch (e: Exception) {
                                                        "Failed: ${e.message}"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = apiKey.isNotBlank()
                                    ) {
                                        Text("Test Connection")
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    testStatus?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (it.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Automation", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-confirm recurring bills", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Auto-create obligations from trusted merchants you've confirmed 3+ times",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isAutoConfirmEnabled,
                                    onCheckedChange = {
                                        isAutoConfirmEnabled = it
                                        settingsProvider.setAutoConfirmEnabled(it)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    var isQuietHoursEnabled by rememberSaveable { mutableStateOf(settingsProvider.isQuietHoursEnabled()) }
                    var quietStart by rememberSaveable { mutableStateOf(settingsProvider.getQuietHoursStart()) }
                    var quietEnd by rememberSaveable { mutableStateOf(settingsProvider.getQuietHoursEnd()) }
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Quiet Hours", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (isQuietHoursEnabled) "Suppressed: ${quietStart}:00\u2013${quietEnd}:00"
                                        else "Suppress non-critical notifications during your off-hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isQuietHoursEnabled,
                                    onCheckedChange = {
                                        isQuietHoursEnabled = it
                                        settingsProvider.setQuietHoursEnabled(it)
                                    }
                                )
                            }
                            if (isQuietHoursEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Start", style = MaterialTheme.typography.labelSmall)
                                        Text("${quietStart}:00", style = MaterialTheme.typography.titleMedium)
                                        Row {
                                            IconButton(onClick = {
                                                if (quietStart > 0) { quietStart -= 1; settingsProvider.setQuietHoursStart(quietStart) }
                                            }) { Text("\u2212") }
                                            IconButton(onClick = {
                                                if (quietStart < 23) { quietStart += 1; settingsProvider.setQuietHoursStart(quietStart) }
                                            }) { Text("+") }
                                        }
                                    }
                                    Text("to", style = MaterialTheme.typography.bodyMedium)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("End", style = MaterialTheme.typography.labelSmall)
                                        Text("${quietEnd}:00", style = MaterialTheme.typography.titleMedium)
                                        Row {
                                            IconButton(onClick = {
                                                if (quietEnd > 0) { quietEnd -= 1; settingsProvider.setQuietHoursEnd(quietEnd) }
                                            }) { Text("\u2212") }
                                            IconButton(onClick = {
                                                if (quietEnd < 23) { quietEnd += 1; settingsProvider.setQuietHoursEnd(quietEnd) }
                                            }) { Text("+") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        onClick = onNavigateToLinkedAccounts
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Linked Account Pairs", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Linked Account Pairs")
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        onClick = onNavigateToCategorySettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manage Categories", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Manage Categories")
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Budget Warning Threshold", style = MaterialTheme.typography.titleMedium)
                                Text("%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = budgetThreshold,
                                onValueChange = { budgetThreshold = it },
                                onValueChangeFinished = { settingsProvider.setBudgetWarningThreshold(budgetThreshold) },
                                valueRange = 0.5f..1.0f,
                                steps = 9
                            )
                        }
                    }
                }

                item {
                    com.najmi.sciuro.core.ui.components.SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        onClick = onNavigateToDeveloperSettings
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Developer Options", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Developer Options")
                        }
                    }
                }
            }
        }
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
                Text("Enter a passphrase to encrypt/decrypt your backup.")
                Spacer(modifier = Modifier.height(12.dp))
                com.najmi.sciuro.core.ui.components.SciuroTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Passphrase",
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotBlank()) onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}





