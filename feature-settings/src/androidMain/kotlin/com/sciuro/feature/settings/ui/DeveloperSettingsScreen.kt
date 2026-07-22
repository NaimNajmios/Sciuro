package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.sciuro.feature.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Simulator", "Sources", "Ingestion Log", "Diagnostics", "Data Tools")
    val simulationResult by viewModel.simulationResult.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val deadLetterCount by viewModel.deadLetterCount.collectAsState()
    val lastCapturedAt by viewModel.lastCapturedAt.collectAsState()

    val lastCaptureText = remember(lastCapturedAt) {
        if (lastCapturedAt == null) "No captures yet" else {
            val elapsed = (System.currentTimeMillis() - lastCapturedAt!!) / 1000
            when {
                elapsed < 60 -> "Just now"
                elapsed < 3600 -> "${elapsed / 60}m ago"
                elapsed < 86400 -> "${elapsed / 3600}h ago"
                else -> "${elapsed / 86400}d ago"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Developer Options",
            heroFigure = lastCaptureText,
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White
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
                        text = "Pending: $pendingCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Dead: $deadLetterCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (deadLetterCount > 0) com.najmi.sciuro.core.ui.theme.SignalDanger else Color.White.copy(alpha = 0.7f)
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
            1 -> DeveloperTabSources()
            2 -> DeveloperTabIngestionLog(viewModel)
            3 -> DeveloperTabDiagnostics(viewModel, simulationResult)
            4 -> DeveloperTabDataTools(viewModel)
        }
    }
}

