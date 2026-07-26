package com.sciuro.feature.kanban.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.SciuroTheme

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Kanban - Review")
@Composable
private fun KanbanPreviewReview() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Review Inbox",
                heroFigure = {
                    Text("5", style = MaterialTheme.typography.displayLarge)
                },
                toggleOptions = listOf("Review", "Bills", "Debts"),
                selectedToggle = "Review",
                onToggleSelected = {},
                chartData = null
            )
            PillToggle(
                options = listOf("To Do", "In Progress", "Done"),
                selectedOption = "To Do",
                onOptionSelected = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                fillWidth = true
            )
            SciuroCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Transfer detected", style = MaterialTheme.typography.titleMedium)
                    Text("RM 45.00 — GrabFood", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, showSystemUi = true, name = "Kanban - Bills")
@Composable
private fun KanbanPreviewBills() {
    SciuroTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel(
                title = "Bills & Obligations",
                heroFigure = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RM 1,200", style = MaterialTheme.typography.headlineMedium)
                        Text("due this month", style = MaterialTheme.typography.bodySmall)
                    }
                },
                toggleOptions = listOf("Review", "Bills", "Debts"),
                selectedToggle = "Bills",
                onToggleSelected = {},
                chartData = null
            )
        }
    }
}
