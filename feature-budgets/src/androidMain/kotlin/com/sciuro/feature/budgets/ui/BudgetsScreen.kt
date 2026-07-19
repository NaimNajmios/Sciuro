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
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Budgets", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(budgets) { budget ->
                Card(modifier = Modifier.fillMaxWidth()) {
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
