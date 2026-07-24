package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.sciuro.feature.budgets.viewmodel.CategoryDrilldownViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoryDrilldownScreen(
    viewModel: CategoryDrilldownViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Category Spending",
            heroFigure = {
                Text(
                    "RM %.0f".format(state.totalSpend),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontFamily = IBMPlexMono
                )
            },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {}
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                if (state.categories.isEmpty()) {
                    item {
                        Text(
                            "No spending data for this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(state.categories) { cat ->
                        SciuroCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "RM %.2f".format(cat.spend),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = IBMPlexMono
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val budget = cat.budgetAmount
                                if (budget > 0) {
                                    val percent = (cat.spend / budget).toFloat().coerceIn(0f, 1f)
                                    val barColor = when {
                                        percent >= 0.9f -> com.najmi.sciuro.core.ui.theme.SignalDanger
                                        percent >= 0.7f -> com.najmi.sciuro.core.ui.theme.SignalWarning
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "%.0f%% of RM %.0f budget".format(percent * 100, budget),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
