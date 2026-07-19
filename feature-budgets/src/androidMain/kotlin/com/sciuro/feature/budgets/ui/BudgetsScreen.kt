package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.sciuro.feature.budgets.viewmodel.BudgetsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = koinViewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            com.najmi.sciuro.core.ui.components.HeroPanel(
                title = "Monthly Budgets",
                heroFigure = "${budgets.size} Active",
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {}
            )
        }
        
        item {
            com.najmi.sciuro.core.ui.components.SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (budgets.isEmpty()) {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(
                            message = "No budgets yet — set a monthly limit for any category to start tracking against it.",
                            primaryCtaText = "Set your first budget",
                            onPrimaryCtaClick = {}
                        )
                    } else {
                        budgets.forEach { budget ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(budget.categoryName, style = MaterialTheme.typography.titleMedium)
                                        Text("RM ${budget.currentSpent} / RM ${budget.allocatedAmount}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val progressColor = if (budget.progress > 1f) Color.Red else MaterialTheme.colorScheme.primary
                                    LinearProgressIndicator(
                                        progress = { if (budget.progress > 1f) 1f else budget.progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = progressColor,
                                        trackColor = Color.LightGray
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
