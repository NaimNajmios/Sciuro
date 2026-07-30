package com.sciuro.feature.wallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.sciuro.core.ledger.engine.SpendingVelocity
import com.sciuro.core.ledger.engine.VelocityTrend
import com.sciuro.feature.wallet.R
import kotlin.math.min

@Composable
fun AccountVelocityCard(velocity: SpendingVelocity) {
    val tokens = LocalSciuroSemanticTokens.current
    val burnColor = when {
        velocity.burnRatePercent > 0.8f -> tokens.signalDanger
        velocity.burnRatePercent > 0.5f -> tokens.signalWarning
        else -> tokens.signalIncome
    }

    val trendSymbol = when (velocity.trend) {
        VelocityTrend.UP -> "\u2191"
        VelocityTrend.DOWN -> "\u2193"
        VelocityTrend.STABLE -> "\u2192"
        VelocityTrend.N_A -> "\u2014"
    }
    val trendColor = when (velocity.trend) {
        VelocityTrend.UP -> tokens.signalDanger
        VelocityTrend.DOWN -> tokens.signalIncome
        VelocityTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        VelocityTrend.N_A -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    stringResource(R.string.wallet_spending_velocity),
                    style = MaterialTheme.typography.titleSmall
                )
                if (velocity.trend != VelocityTrend.N_A) {
                    Text(
                        text = trendSymbol,
                        color = trendColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "RM ${"%.0f".format(velocity.dailyAverage)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = IBMPlexMono,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { min(1f, velocity.burnRatePercent) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = burnColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                if (velocity.daysUntilDepleted == Long.MAX_VALUE) stringResource(R.string.wallet_no_spending_yet)
                else stringResource(R.string.wallet_days_remaining, velocity.daysUntilDepleted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
