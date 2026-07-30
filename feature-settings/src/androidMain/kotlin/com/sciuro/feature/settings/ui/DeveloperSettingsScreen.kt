package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.DeveloperSettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
    initialTab: Int = 0,
    viewModel: DeveloperSettingsViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        stringResource(R.string.dev_tab_simulator),
        stringResource(R.string.dev_tab_sources),
        stringResource(R.string.dev_tab_ingestion_log),
        stringResource(R.string.dev_tab_diagnostics),
        stringResource(R.string.dev_tab_data_tools),
        stringResource(R.string.dev_tab_health),
        stringResource(R.string.dev_tab_pipeline_trace)
    )
    val simulationResult by viewModel.simulationResult.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val deadLetterCount by viewModel.deadLetterCount.collectAsState()
    val lastCapturedAt by viewModel.lastCapturedAt.collectAsState()
    val uiError by viewModel.uiError.collectAsState()
    val noCapturesText = stringResource(R.string.developer_no_captures)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tokens = LocalSciuroSemanticTokens.current

    LaunchedEffect(uiError) {
        uiError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
            viewModel.clearUiError()
        }
    }

    val lastCaptureText = remember(lastCapturedAt, noCapturesText) {
        if (lastCapturedAt == null) noCapturesText else {
            val elapsed = (System.currentTimeMillis() - lastCapturedAt!!) / 1000
            when {
                elapsed < 60 -> "Just now"
                elapsed < 3600 -> "${elapsed / 60}m ago"
                elapsed < 86400 -> "${elapsed / 3600}h ago"
                else -> "${elapsed / 86400}d ago"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HeroPanel(
                title = stringResource(R.string.developer_title),
                heroFigure = { Text(lastCaptureText, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = SciuroIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.linked_accounts_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                content = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.developer_pending, pendingCount.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.developer_dead, deadLetterCount.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (deadLetterCount > 0) tokens.signalDanger else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            )

            SheetList(modifier = Modifier.offset(y = (-24).dp).weight(1f)) {
                PillToggle(
                    options = tabs,
                    selectedOption = tabs.getOrElse(selectedTab) { tabs[0] },
                    onOptionSelected = { option -> selectedTab = tabs.indexOf(option) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    scrollable = true
                )

                val tabModifier = Modifier.weight(1f)
                when (selectedTab) {
                    0 -> DeveloperTabSimulator(viewModel, simulationResult, modifier = tabModifier)
                    1 -> DeveloperTabSources(viewModel, modifier = tabModifier)
                    2 -> DeveloperTabIngestionLog(viewModel, modifier = tabModifier)
                    3 -> DeveloperTabDiagnostics(viewModel, simulationResult, modifier = tabModifier)
                    4 -> DeveloperTabDataTools(viewModel, modifier = tabModifier)
                    5 -> DeveloperTabHealth(viewModel, modifier = tabModifier)
                    6 -> DeveloperTabPipelineTrace(viewModel, modifier = tabModifier)
                }
            }
        }
    }
}
