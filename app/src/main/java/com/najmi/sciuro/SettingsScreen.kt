package com.najmi.sciuro

import androidx.compose.foundation.layout.*
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

@Composable
fun SettingsScreen() {
    var isLlmOptIn by rememberSaveable { mutableStateOf(true) }
    var apiKey by rememberSaveable { mutableStateOf("") }
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
        
        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxHeight()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Application Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LLM Classification", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Use Groq Llama 3 for Fallback", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isLlmOptIn, 
                                onCheckedChange = { isLlmOptIn = it }
                            )
                        }
                        
                        if (isLlmOptIn) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
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
