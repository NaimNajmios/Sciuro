package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.najmi.sciuro.core.ui.R
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.najmi.sciuro.core.ui.util.bounceClick


@Composable
fun TransactionCard(
    merchantName: String,
    amount: String,
    direction: String,
    statusText: String,
    categoryIcon: ImageVector? = null,
    categoryColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    isTransfer: Boolean = false,
    confidence: Double? = null,
    extractionMethod: String? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val t = LocalSciuroSemanticTokens.current
    val directionTint = if (direction == "INFLOW") t.signalIncome else t.signalDanger
    val amountColor = if (direction == "INFLOW") t.signalIncome else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (direction == "INFLOW") SciuroIcons.ArrowDown else SciuroIcons.ArrowUp,
                    contentDescription = null,
                    tint = directionTint,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryIcon != null) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusText == "Reviewed") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.error
                )
            }

            if (isTransfer) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = stringResource(R.string.tx_transfer),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.tx_transfer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (confidence != null && extractionMethod != null) {
                val dotColor = when {
                    extractionMethod == "MANUAL" -> t.signalIncome
                    confidence >= 0.85f -> t.signalIncome
                    confidence >= 0.50f -> t.signalWarning
                    else -> t.signalDanger
                }
                val manualEntryLabel = stringResource(R.string.tx_manual_entry)
                val confidenceLabel = if (extractionMethod == "MANUAL") manualEntryLabel else "Confidence ${(confidence * 100).toInt()} percent"
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .semantics { contentDescription = confidenceLabel }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = IBMPlexMono,
                color = amountColor
            )
        }
    }
}

