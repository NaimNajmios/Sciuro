package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.parsing.engine.SimulationResult
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroTextField

@Composable
fun DeveloperTabDiagnostics(
    viewModel: DeveloperSettingsViewModel,
    simulationResult: SimulationResult?,
    modifier: Modifier = Modifier
) {
    var diagTitle by remember { mutableStateOf("") }
    var diagText by remember { mutableStateOf("") }
    var diagPackage by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.dev_diagnostics_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagPackage,
                onValueChange = { diagPackage = it },
                label = stringResource(R.string.dev_diagnostics_package_label)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagTitle,
                onValueChange = { diagTitle = it },
                label = stringResource(R.string.dev_diagnostics_title_label)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SciuroTextField(
                value = diagText,
                onValueChange = { diagText = it },
                label = stringResource(R.string.dev_diagnostics_text_label),
                singleLine = false,
                minLines = 3,
                maxLines = 8,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.simulateNotification(diagTitle, diagText, diagPackage)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dev_diagnostics_run))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        simulationResult?.let { result ->
            item {
                SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.dev_diagnostics_rule_results), style = MaterialTheme.typography.titleMedium)
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
                            "${ruleResult.ruleName}: ${if (ruleResult.matches) stringResource(R.string.dev_diagnostics_match) else stringResource(R.string.dev_diagnostics_no_match)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        ruleResult.extractedDraft?.let { draft ->
                            Text(stringResource(R.string.dev_diagnostics_amount, "%.2f".format(draft.amount)))
                            Text(stringResource(R.string.dev_diagnostics_direction, draft.direction ?: stringResource(R.string.dev_simulator_unknown)))
                            Text(stringResource(R.string.dev_diagnostics_merchant, draft.merchant ?: stringResource(R.string.dev_simulator_na)))
                            Text(stringResource(R.string.dev_diagnostics_account, draft.accountOrChannel ?: stringResource(R.string.dev_simulator_na)))
                            Text(stringResource(R.string.dev_diagnostics_confidence, "%.0f".format(draft.confidenceScore * 100)))
                        }
                    }
                }
            }

            result.llmDebugInfo?.let { debug ->
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.dev_diagnostics_llm_debug), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            debug.modelUsed?.let { Text(stringResource(R.string.dev_diagnostics_model, it)) }
                            debug.latencyMs?.let { Text(stringResource(R.string.dev_diagnostics_latency, it)) }
                            debug.error?.let { Text(stringResource(R.string.dev_diagnostics_error, it), color = MaterialTheme.colorScheme.error) }
                            debug.prompt?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.dev_diagnostics_prompt_label), style = MaterialTheme.typography.labelMedium)
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 20)
                            }
                            debug.rawResponse?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.dev_diagnostics_response_label), style = MaterialTheme.typography.labelMedium)
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 10)
                            }
                        }
                    }
                }
            }

            result.llmPackageMarker?.let { marker ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.dev_diagnostics_llm_candidate), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.dev_diagnostics_llm_candidate_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(marker, style = MaterialTheme.typography.bodySmall, maxLines = 10)
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
                        Text(stringResource(R.string.dev_diagnostics_error, err), modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
