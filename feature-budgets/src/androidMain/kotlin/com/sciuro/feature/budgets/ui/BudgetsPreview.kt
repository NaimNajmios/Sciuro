package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Preview(showBackground = true, showSystemUi = true, name = "Budgets - Empty")
@Composable
private fun BudgetsPreviewEmpty() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Monthly Budget",
                heroFigure = {
                    Text("RM 0 / RM 3,000", style = MaterialTheme.typography.headlineSmall)
                },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                chartData = null
            )
            Text(
                "No budgets set yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Budgets - With Data")
@Composable
private fun BudgetsPreviewWithData() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Monthly Budget",
                heroFigure = {
                    Text("RM 1,450 / RM 3,000", style = MaterialTheme.typography.headlineSmall)
                },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                chartData = null
            )
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Food & Dining", style = MaterialTheme.typography.titleSmall)
                        Text("RM 420 / RM 800", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.525f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transport", style = MaterialTheme.typography.titleSmall)
                        Text("RM 180 / RM 400", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
