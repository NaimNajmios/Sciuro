package com.sciuro.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import com.najmi.sciuro.core.ui.util.SciuroIcons

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroSectionHeader
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList


import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.ConnectionTestState
import com.sciuro.feature.settings.viewmodel.IntelligenceSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun IntelligenceSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: IntelligenceSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLlmFields by remember { mutableStateOf(uiState.isLlmEnabled) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLlmEnabled) {
        showLlmFields = uiState.isLlmEnabled
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.intelligence_settings_title),
            heroFigure = { Text(stringResource(R.string.intelligence_settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.linked_accounts_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp)
            ) {
                item {
                    SciuroSectionHeader(stringResource(R.string.settings_section_llm), icon = SciuroIcons.Star)
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

                                    SciuroTextField(
                                        value = uiState.dailyLlmLimit.toString(),
                                        onValueChange = { newValue ->
                                            newValue.toIntOrNull()?.let { viewModel.setDailyLlmLimit(it) }
                                        },
                                        label = stringResource(R.string.settings_llm_daily_limit),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
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
            }
        }
    }
}
