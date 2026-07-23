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
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SettingsScreen(
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {},
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
                    com.najmi.sciuro.core.ui.components.SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Data Backup", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Your financial data is encrypted at rest and excluded from cloud backup. Encrypted manual export and import will be available in a future update.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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





