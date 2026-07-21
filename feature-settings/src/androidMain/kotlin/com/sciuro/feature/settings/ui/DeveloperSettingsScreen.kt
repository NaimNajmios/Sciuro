package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Developer Options",
            heroFigure = "Tools",
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
