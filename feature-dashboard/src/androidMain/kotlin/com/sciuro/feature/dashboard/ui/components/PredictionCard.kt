package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.Sparkline
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.sciuro.core.budget.engine.RunwayPrediction
import com.sciuro.feature.dashboard.R

@Composable
fun PredictionCard(prediction: RunwayPrediction) {
    val tokens = LocalSciuroSemanticTokens.current
    val accentColor = when {
        prediction.daysUntilNegative < 7 -> tokens.signalDanger
        prediction.daysUntilNegative < 14 -> tokens.signalWarning
        else -> tokens.signalIncome
    }

    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.dashboard_projected_runway),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${(prediction.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "${prediction.daysUntilNegative}d",
                style = MaterialTheme.typography.headlineLarge,
                color = accentColor
            )

            Sparkline(
                data = prediction.projectedBalance.map { it.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                lineColor = accentColor,
                height = 48.dp,
                showDot = true
            )

            Text(
                stringResource(R.string.dashboard_daily_burn, "RM ${"%.0f".format(prediction.dailyAverageSpend)}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
