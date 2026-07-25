package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.theme.BrandPrimaryDark
import com.najmi.sciuro.core.ui.theme.SignalDanger
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
    val tabs = listOf("Simulator", "Sources", "Ingestion Log", "Diagnostics", "Data Tools", "Health", "Pipeline Trace")
    val simulationResult by viewModel.simulationResult.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val deadLetterCount by viewModel.deadLetterCount.collectAsState()
    val lastCapturedAt by viewModel.lastCapturedAt.collectAsState()
    val uiError by viewModel.uiError.collectAsState()
    val noCapturesText = stringResource(R.string.developer_no_captures)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                heroFigure = { Text(lastCaptureText, style = MaterialTheme.typography.headlineLarge, color = BrandPrimaryDark) },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.linked_accounts_back),
                            tint = BrandPrimaryDark
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
                            color = BrandPrimaryDark.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.developer_dead, deadLetterCount.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (deadLetterCount > 0) SignalDanger else BrandPrimaryDark.copy(alpha = 0.7f)
                        )
                    }
                }
            )

            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DeveloperTabSimulator(viewModel, simulationResult)
                1 -> DeveloperTabSources(viewModel)
                2 -> DeveloperTabIngestionLog(viewModel)
                3 -> DeveloperTabDiagnostics(viewModel, simulationResult)
                4 -> DeveloperTabDataTools(viewModel)
                5 -> DeveloperTabHealth(viewModel)
                6 -> DeveloperTabPipelineTrace(viewModel)
            }
        }
    }
}
