package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.sciuro.feature.dashboard.R

@Composable
fun DashboardSummaryRow(
    activeBudgetsCount: Int,
    runway: Double,
    hasIncomePattern: Boolean,
    expectedIncomeAmount: Double = 0.0,
    expectedIncomeDate: Long? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSciuroSemanticTokens.current
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SciuroCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dashboard_active_budgets), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$activeBudgetsCount", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        SciuroCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.dashboard_runway),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "RM ${"%.0f".format(runway)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (runway < 0) tokens.signalDanger else MaterialTheme.colorScheme.onSurface
                )
                if (hasIncomePattern && expectedIncomeDate != null) {
                    val dateStr = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(expectedIncomeDate))
                    Text(
                        stringResource(R.string.dashboard_expected_income, expectedIncomeAmount.toInt(), dateStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!hasIncomePattern) {
                    Text(
                        stringResource(R.string.dashboard_based_on_bills_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}