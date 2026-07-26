package com.sciuro.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroFigure
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Dashboard - Empty")
@Composable
private fun DashboardPreviewEmpty() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Total Net Position",
                heroFigure = { HeroFigure(0.0) },
                toggleOptions = listOf("This Month", "All Time"),
                selectedToggle = "All Time",
                onToggleSelected = {},
                chartData = emptyList()
            )
            Text(
                "Nothing gathered yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Dashboard - With Data")
@Composable
private fun DashboardPreviewWithData() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Total Net Position",
                heroFigure = { HeroFigure(12450.00) },
                toggleOptions = listOf("This Month", "All Time"),
                selectedToggle = "All Time",
                onToggleSelected = {},
                chartData = listOf(100f, 200f, 150f, 300f, 250f, 400f, 350f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SciuroCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Budgets", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                SciuroCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Runway", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RM 4,200", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}
