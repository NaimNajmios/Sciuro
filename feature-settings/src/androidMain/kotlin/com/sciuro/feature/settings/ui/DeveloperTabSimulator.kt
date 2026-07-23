package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sciuro.core.parsing.engine.SimulationResult
import com.sciuro.core.parsing.fixture.FixtureLibrary
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import com.najmi.sciuro.core.ui.components.SciuroTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperTabSimulator(
    viewModel: SettingsViewModel,
    simulationResult: SimulationResult?,
    modifier: Modifier = Modifier
) {
    var customPackage by remember { mutableStateOf("com.google.android.gm") }
    var customTitle by remember { mutableStateOf("m2u Notification") }
    var customText by remember { mutableStateOf("Please find the details of the transfer below: A transfer of RM 50.00 has been successfully processed from my M2U account.") }
    var selectedPackage by remember { mutableStateOf("") }
    var expandedPackage by remember { mutableStateOf(false) }
    var expandedTemplate by remember { mutableStateOf(false) }

    val templates = remember(selectedPackage) {
        if (selectedPackage.isNotBlank()) FixtureLibrary.fixturesForPackage(selectedPackage)
        else emptyList()
    }
    var selectedTemplate by remember { mutableStateOf<FixtureLibrary.Fixture?>(null) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dynamic Pipeline Simulator", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    SciuroTextField(
                        value = customPackage,
                        onValueChange = { customPackage = it; selectedPackage = "" },
                        label = "Package Name"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = "Notification Title"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = "Notification Text"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.simulateNotification(customTitle, customText, customPackage) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send to Pipeline")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Simulators", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedPackage,
                        onExpandedChange = { expandedPackage = !expandedPackage }
                    ) {
                        SciuroTextField(
                            value = selectedPackage.ifBlank { "Select package..." },
                            onValueChange = {},
                            readOnly = true,
                            label = "Package",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPackage) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPackage,
                            onDismissRequest = { expandedPackage = false }
                        ) {
                            FixtureLibrary.allPackages().sorted().forEach { pkg ->
                                DropdownMenuItem(
                                    text = { Text(pkg) },
                                    onClick = {
                                        selectedPackage = pkg
                                        expandedPackage = false
                                        selectedTemplate = null
                                        customPackage = pkg
                                        val first = FixtureLibrary.fixturesForPackage(pkg).firstOrNull()
                                        if (first != null) {
                                            selectedTemplate = first
                                            customTitle = first.title
                                            customText = first.text
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (templates.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedTemplate,
                            onExpandedChange = { expandedTemplate = !expandedTemplate }
                        ) {
                            SciuroTextField(
                                value = selectedTemplate?.description ?: "Select template...",
                                onValueChange = {},
                                readOnly = true,
                                label = "Template",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTemplate) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTemplate,
                                onDismissRequest = { expandedTemplate = false }
                            ) {
                                templates.forEach { fixture ->
                                    DropdownMenuItem(
                                        text = { Text(fixture.description) },
                                        onClick = {
                                            selectedTemplate = fixture
                                            expandedTemplate = false
                                            customTitle = fixture.title
                                            customText = fixture.text
                                            customPackage = fixture.packageName
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val f = selectedTemplate ?: return@Button
                                viewModel.simulateNotification(f.title, f.text, f.packageName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Selected Template")
                        }
                    }
                }
            }

            simulationResult?.let { result ->
                SimulationResultCard(result)
            }
        }
    }
}

@Composable
fun SimulationResultCard(result: SimulationResult) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.error != null) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Simulation Result", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Matched Rule: ${result.matchedRule ?: "None"}")
            result.finalDraft?.let { draft ->
                Text("Amount: RM ${"%.2f".format(draft.amount)}")
                Text("Direction: ${draft.direction ?: "Unknown"}")
                Text("Merchant: ${draft.merchant ?: "N/A"}")
                Text("Account: ${draft.accountOrChannel ?: "N/A"}")
                Text("Confidence: ${"%.0f".format(draft.confidenceScore * 100)}%")
            } ?: Text("No draft produced", color = MaterialTheme.colorScheme.error)
            Text("LLM Fallback: ${if (result.usedLlmFallback) "Yes" else "No"}")
            result.llmLatencyMs?.let { Text("LLM Latency: ${it}ms") }
            result.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}
