package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
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

                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Active Budgets", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${state.activeBudgetsCount} active this month",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                        
                        Text(
                            "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        if (state.recentTransactions.isEmpty()) {
                            Text("No recent transactions", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                            state.recentTransactions.forEach { tx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (tx.direction == "INFLOW") Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else Color(0xFFE53935)
                                            )
                                            Column {
                                                Text(
                                                    tx.merchant ?: "Unknown Merchant",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    if (tx.is_reviewed == 1L) "Reviewed" else "Unreviewed",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (tx.is_reviewed == 1L) Color.Gray else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Text(
                                            "RM ${"%.2f".format(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (tx.direction == "INFLOW") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
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
}

