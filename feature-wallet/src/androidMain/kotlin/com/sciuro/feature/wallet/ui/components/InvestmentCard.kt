package com.sciuro.feature.wallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.sciuro.core.investment.model.Investment

@Composable
fun InvestmentCard(
    investment: Investment,
    livePrice: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookValue = investment.unitsHeld * investment.averageBuyPrice
    val showLive = livePrice != null && livePrice > 0.0 && kotlin.math.abs(livePrice - bookValue) > 0.01
    val liveColor = if (showLive && livePrice!! >= bookValue) Color(0xFF4CAF50) else Color(0xFFE53935)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (investment.assetType == "Gold") Icons.Filled.Toll else Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(investment.assetSymbol, style = MaterialTheme.typography.titleMedium)
                    Text(investment.assetName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "RM ${"%.2f".format(bookValue)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = IBMPlexMono
                )
                if (showLive) {
                    Text(
                        "Live: RM ${"%.2f".format(livePrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = IBMPlexMono,
                        color = liveColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}