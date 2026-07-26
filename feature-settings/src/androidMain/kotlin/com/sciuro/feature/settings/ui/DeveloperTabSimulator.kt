package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.parsing.engine.SimulationResult
import com.sciuro.core.parsing.fixture.FixtureLibrary
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperTabSimulator(
    viewModel: DeveloperSettingsViewModel,
    simulationResult: SimulationResult?,
    modifier: Modifier = Modifier
) {
    var customPackage by remember { mutableStateOf("com.google.android.gm") }
    var customTitle by remember { mutableStateOf("m2u Notification") }
    var customText by remember { mutableStateOf("Please find the details of the transfer below: A transfer of RM 50.00 has been successfully processed from my M2U account.") }
    var selectedPackage by remember { mutableStateOf("") }
    var expandedPackage by remember { mutableStateOf(false) }
    var expandedTemplate by remember { mutableStateOf(false) }

    val batchRunning by viewModel.batchRunning.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
    val batchProgressFraction by viewModel.batchProgressFraction.collectAsState()

    val templates = remember(selectedPackage) {
        if (selectedPackage.isNotBlank()) FixtureLibrary.fixturesForPackage(selectedPackage)
        else emptyList()
    }
    var selectedTemplate by remember { mutableStateOf<FixtureLibrary.Fixture?>(null) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_simulator_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    SciuroTextField(
                        value = customPackage,
                        onValueChange = { customPackage = it; selectedPackage = "" },
                        label = stringResource(R.string.dev_simulator_package)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = stringResource(R.string.dev_simulator_title_label)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SciuroTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = stringResource(R.string.dev_simulator_text),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 8,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.simulateNotification(customTitle, customText, customPackage) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dev_simulator_send))
                    }
                }
            }

            SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_simulator_quick_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedPackage,
                        onExpandedChange = { expandedPackage = !expandedPackage }
                    ) {
                        SciuroTextField(
                            value = selectedPackage.ifBlank { stringResource(R.string.dev_simulator_package_hint) },
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.dev_simulator_package_label),
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
                                value = selectedTemplate?.description ?: stringResource(R.string.dev_simulator_template_hint),
                                onValueChange = {},
                                readOnly = true,
                                label = stringResource(R.string.dev_simulator_template_label),
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
                            Text(stringResource(R.string.dev_simulator_simulate))
                        }
                    }
                }
            }

            simulationResult?.let { result ->
                SimulationResultCard(result)
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.dev_simulator_batch_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dev_simulator_batch_description, FixtureLibrary.count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var forceLlm by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = forceLlm, onCheckedChange = { forceLlm = it })
                            Text(stringResource(R.string.dev_simulator_force_llm), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = { viewModel.runAllFixtures(forceLlm = forceLlm) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !batchRunning
                    ) {
                        Text(if (batchRunning) stringResource(R.string.dev_simulator_running) else stringResource(R.string.dev_simulator_run_all, FixtureLibrary.count))
                    }
                    if (batchProgress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { batchProgressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            batchProgress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            Text(stringResource(R.string.dev_simulator_result_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.dev_simulator_matched_rule, result.matchedRule ?: "None"))
            result.finalDraft?.let { draft ->
                Text(stringResource(R.string.dev_simulator_amount, "%.2f".format(draft.amount)))
                Text(stringResource(R.string.dev_simulator_direction, draft.direction ?: stringResource(R.string.dev_simulator_unknown)))
                Text(stringResource(R.string.dev_simulator_merchant, draft.merchant ?: stringResource(R.string.dev_simulator_na)))
                Text(stringResource(R.string.dev_simulator_account, draft.accountOrChannel ?: stringResource(R.string.dev_simulator_na)))
                Text(stringResource(R.string.dev_simulator_confidence, "%.0f".format(draft.confidenceScore * 100)))
            } ?: Text(stringResource(R.string.dev_simulator_no_draft), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.dev_simulator_llm_fallback, if (result.usedLlmFallback) stringResource(R.string.dev_simulator_yes) else stringResource(R.string.dev_simulator_no)))
            result.llmLatencyMs?.let { Text(stringResource(R.string.dev_simulator_llm_latency, it)) }
            result.error?.let { Text(stringResource(R.string.dev_simulator_error, it), color = MaterialTheme.colorScheme.error) }
        }
    }
}
