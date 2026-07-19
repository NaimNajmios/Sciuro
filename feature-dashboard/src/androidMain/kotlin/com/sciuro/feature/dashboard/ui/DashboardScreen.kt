package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.feature.dashboard.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedRange by remember { mutableStateOf("All Time") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            HeroPanel(
                title = "Total Net Worth",
                heroFigure = "RM ${"%.2f".format(state.netWorth)}",
                toggleOptions = listOf("This Month", "All Time"),
                selectedToggle = selectedRange,
                onToggleSelected = { selectedRange = it },
                chartData = listOf(100f, 150f, 130f, 180f, 200f) // Mock chart data for now
            )
        }
        
        item {
            SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content inside the sheet
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (state.unreviewedTransactionsCount == 0 && state.activeBudgetsCount == 0) {
                        com.najmi.sciuro.core.ui.components.EmptyStateView(
                            message = "Nothing gathered yet — once your bank notifications start coming in, this is where they'll show up."
                        )
                    } else {
                        if (state.unreviewedTransactionsCount > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Column {
                                        Text(
                                            "Review Inbox",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "${state.unreviewedTransactionsCount} items pending your review",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Active Budgets", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${state.activeBudgetsCount} active this month",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

