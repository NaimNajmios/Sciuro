package com.najmi.sciuro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.parsing.config.SettingsProvider
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import java.util.UUID
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalContext
import com.najmi.sciuro.core.ui.theme.ThemeManager
import com.najmi.sciuro.core.ui.theme.ThemePreference

@Composable
fun SettingsScreen(
    settingsProvider: SettingsProvider = koinInject(),
    notificationSourceAdapter: NotificationSourceAdapter = koinInject()
) {
    var isLlmOptIn by rememberSaveable { mutableStateOf(settingsProvider.isLlmEnabled()) }
    var apiKey by rememberSaveable { mutableStateOf(settingsProvider.getApiKey() ?: "") }
    var testStatus by rememberSaveable { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Settings",
            heroFigure = "More",
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {}
        )
        
        val context = LocalContext.current
        val themeManager = remember { ThemeManager.getInstance(context) }
        val themePref by themeManager.themePreference.collectAsState()
        
        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxHeight()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Application Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleSmall)
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
                
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LLM Classification", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                            
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        testStatus = "Testing..."
                                        scope.launch {
                                            testStatus = "Testing..."
                                            val result = withContext(Dispatchers.IO) {
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
                                            testStatus = result
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
                
                data class SimulationTemplate(val name: String, val packageName: String, val title: String, val text: String)
                val templates = remember { listOf(
                    SimulationTemplate("MAE Valid Payment", "com.maybank2u.life", "MAE", "RM15.50 has been deducted from your account for a payment to STARBUCKS on 28 Apr 2024."),
                    SimulationTemplate("MAE Invalid Format (LLM Test)", "com.maybank2u.life", "MAE Transfer", "Your payment of MYR 45.00 via DuitNow to NETFLIX is complete."),
                    SimulationTemplate("Garbage/Spam", "com.cimbmalaysia", "Promo", "Get 50% off your next loan application!")
                ) }
                
                var selectedTemplateIndex by remember { mutableStateOf(0) }
                var simPackageName by remember { mutableStateOf(templates[0].packageName) }
                var simTitle by remember { mutableStateOf(templates[0].title) }
                var simText by remember { mutableStateOf(templates[0].text) }
                var expanded by remember { mutableStateOf(false) }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dynamic Pipeline Simulator", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Test the parser and routing pipeline with custom mock notifications.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = templates[selectedTemplateIndex].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Scenario Template") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                templates.forEachIndexed { index, template ->
                                    DropdownMenuItem(
                                        text = { Text(template.name) },
                                        onClick = {
                                            selectedTemplateIndex = index
                                            simPackageName = template.packageName
                                            simTitle = template.title
                                            simText = template.text
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simPackageName,
                            onValueChange = { simPackageName = it },
                            label = { Text("Package Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simTitle,
                            onValueChange = { simTitle = it },
                            label = { Text("Notification Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simText,
                            onValueChange = { simText = it },
                            label = { Text("Notification Text") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    val event = RawEvent(
                                        id = UUID.randomUUID().toString(),
                                        sourceType = SourceType.NOTIFICATION,
                                        sourcePackageOrAddress = simPackageName,
                                        title = simTitle,
                                        text = simText,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    notificationSourceAdapter.emitNotification(event)
                                }
                            }
                        ) {
                            Text("Inject Notification")
                        }
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Database Backup", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Local database is explicitly excluded from Android Auto-Backup (ADR-020).", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
