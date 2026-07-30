package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.R
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalIncome
import com.najmi.sciuro.core.ui.theme.SignalWarning

data class AuditEventDisplay(
    val label: String,
    val detail: String,
    val isCurrent: Boolean = false
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    merchantName: String,
    amount: String,
    direction: String,
    timestamp: String,
    extractionMethod: String?,
    confidence: Double?,
    rawEventTitle: String?,
    rawEventText: String?,
    hasTransferLink: Boolean,
    auditEvents: List<AuditEventDisplay> = emptyList(),
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (showSheet) {
        SciuroFormSheet(
            title = stringResource(R.string.tx_details_title),
            onDismissRequest = onDismiss,
            icon = SciuroIcons.Receipt
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchantName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = amount,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = IBMPlexMono,
                    fontWeight = FontWeight.Bold,
                    color = if (direction == "INFLOW") com.najmi.sciuro.core.ui.theme.SignalIncome else MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (direction == "INFLOW") stringResource(R.string.tx_income) else stringResource(R.string.tx_expense),
                style = MaterialTheme.typography.labelMedium,
                color = if (direction == "INFLOW") com.najmi.sciuro.core.ui.theme.SignalIncome else com.najmi.sciuro.core.ui.theme.SignalDanger
            )

            if (hasTransferLink) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = stringResource(R.string.tx_part_of_transfer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (extractionMethod != null) {
                SciuroCard(modifier = Modifier.fillMaxWidth()) {
                    val methodLabel = when (extractionMethod) {
                        "REGEX" -> stringResource(R.string.tx_auto_parsed)
                        "LLM_FALLBACK" -> stringResource(R.string.tx_ai_assisted)
                        "MANUAL" -> stringResource(R.string.tx_manual_entry)
                        else -> extractionMethod
                    }
                    val dotColor = when {
                        extractionMethod == "MANUAL" -> SignalIncome
                        confidence != null && confidence >= 0.85 -> SignalIncome
                        confidence != null && confidence >= 0.50 -> SignalWarning
                        else -> SignalDanger
                    }
    
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = methodLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (confidence != null) {
                                Text(
                                    text = "Confidence: ${(confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!rawEventTitle.isNullOrBlank() || !rawEventText.isNullOrBlank()) {
                var expanded by remember { mutableStateOf(false) }

                SciuroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.tx_original_notification),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (rawEventTitle != null) {
                                    Text(
                                        text = rawEventTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (rawEventText != null) {
                                    val displayText = if (!expanded && rawEventText.length > 150) {
                                        rawEventText.take(150) + "..."
                                    } else {
                                        rawEventText
                                    }
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = if (rawEventTitle != null) 4.dp else 0.dp)
                                    )
                                    if (rawEventText.length > 150) {
                                        TextButton(
                                            onClick = { expanded = !expanded },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = if (expanded) stringResource(R.string.tx_show_less) else stringResource(R.string.tx_show_more),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (auditEvents.isNotEmpty()) {
                SciuroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.shared_history),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            auditEvents.forEach { event ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (event.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.background,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = event.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = event.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = SciuroIcons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shared_delete))
                }

                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = SciuroIcons.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shared_edit))
                }
            }
        }
    }
}

