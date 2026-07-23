package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.core.parsing.engine.SimulationResult
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import com.najmi.sciuro.core.ui.components.SciuroTextField

@Composable
fun DeveloperTabDiagnostics(
    viewModel: SettingsViewModel,
    simulationResult: SimulationResult?,
    modifier: Modifier = Modifier
) {
    var diagTitle by remember { mutableStateOf("") }
    var diagText by remember { mutableStateOf("") }
    var diagPackage by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Parser Diagnostics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagPackage,
                onValueChange = { diagPackage = it },
                label = "Package Name"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagTitle,
                onValueChange = { diagTitle = it },
                label = "Title"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagText,
                onValueChange = { diagText = it },
                label = "Text",
                minLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.simulateNotification(diagTitle, diagText, diagPackage)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Diagnostics")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        simulationResult?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rule Match Results", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            items(result.allRuleResults.size) { index ->
                val ruleResult = result.allRuleResults[index]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ruleResult.matches) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${ruleResult.ruleName}: ${if (ruleResult.matches) "MATCH" else "NO MATCH"}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        ruleResult.extractedDraft?.let { draft ->
                            Text("Amount: RM ${"%.2f".format(draft.amount)}")
                            Text("Direction: ${draft.direction ?: "Unknown"}")
                            Text("Merchant: ${draft.merchant ?: "N/A"}")
                            Text("Account: ${draft.accountOrChannel ?: "N/A"}")
                            Text("Confidence: ${"%.0f".format(draft.confidenceScore * 100)}%")
                        }
                    }
                }
            }

            result.llmDebugInfo?.let { debug ->
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("LLM Debug", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            debug.modelUsed?.let { Text("Model: $it") }
                            debug.latencyMs?.let { Text("Latency: ${it}ms") }
                            debug.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                            debug.prompt?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Prompt:", style = MaterialTheme.typography.labelMedium)
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 20)
                            }
                            debug.rawResponse?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Raw Response:", style = MaterialTheme.typography.labelMedium)
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 10)
                            }
                        }
                    }
                }
            }

            result.error?.let { err ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text("Error: $err", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
