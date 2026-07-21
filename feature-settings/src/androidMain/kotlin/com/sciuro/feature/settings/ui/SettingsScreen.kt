package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
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
import com.sciuro.core.parsing.config.SettingsProvider
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDeveloperSettings: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    settingsProvider: SettingsProvider = koinInject()
) {
    var isLlmOptIn by rememberSaveable { mutableStateOf(settingsProvider.isLlmEnabled()) }
    var apiKey by rememberSaveable { mutableStateOf(settingsProvider.getApiKey() ?: "") }
    var testStatus by rememberSaveable { mutableStateOf<String?>(null) }



    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val themePref by themeManager.themePreference.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Application Settings ---
            item {
                Text(
                    "Application Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // --- Appearance ---
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreference.values().forEach { pref ->
                                FilterChip(
                                    selected = themePref == pref,
                                    onClick = { themeManager.setTheme(pref) },
                                    label = { Text(pref.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }
            }

            // --- LLM Classification ---
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
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
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    settingsProvider.setApiKey(it)
                                },
                                label = { Text("Groq API Key") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

            // --- Developer Options Link ---
            item {
                Card(
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
