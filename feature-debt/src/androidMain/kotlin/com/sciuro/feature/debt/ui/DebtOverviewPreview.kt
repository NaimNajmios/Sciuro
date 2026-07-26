package com.sciuro.feature.debt.ui

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

@Preview(showBackground = true, showSystemUi = true, name = "Debt Overview - Empty")
@Composable
private fun DebtOverviewPreviewEmpty() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Debt Overview",
                heroFigure = {
                    Text("RM 0.00", style = MaterialTheme.typography.headlineLarge)
                },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                chartData = null
            )
            Text(
                "No debts tracked",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Debt Overview - With Data")
@Composable
private fun DebtOverviewPreviewWithData() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Debt Overview",
                heroFigure = {
                    Column {
                        Text("RM 23,500", style = MaterialTheme.typography.headlineLarge)
                        Text("remaining", style = MaterialTheme.typography.bodySmall)
                    }
                },
                toggleOptions = emptyList(),
                selectedToggle = "",
                onToggleSelected = {},
                chartData = null
            )
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PTPTN Education Loan", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("RM 15,000 / RM 30,000 remaining", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
