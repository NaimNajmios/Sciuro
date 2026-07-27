package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.audit.model.TransactionIntent
import com.sciuro.feature.dashboard.R
import com.sciuro.feature.dashboard.viewmodel.ReviewSuggestion

@Composable
fun ReviewInboxBanner(
    unreviewedCount: Int,
    suggestions: List<ReviewSuggestion>,
    onConfirm: (String, String?, String?) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = stringResource(R.string.dashboard_review_inbox_warning_cd),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dashboard_review_inbox),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "$unreviewedCount items pending your review",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            suggestions.take(3).forEach { suggestion ->
                Spacer(modifier = Modifier.height(8.dp))
                SuggestionChip(
                    suggestion = suggestion,
                    onConfirm = { onConfirm(suggestion.transactionId, suggestion.suggestedCategoryId, suggestion.suggestedAccountId) },
                    onReject = { onReject(suggestion.transactionId) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: ReviewSuggestion,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (val intent = suggestion.intent) {
        is TransactionIntent.SubscriptionPayment -> "Subscription: ${intent.obligationName}"
        is TransactionIntent.DebtPayment -> "Debt: ${intent.debtName}"
        is TransactionIntent.DebtCollection -> "Collection: ${intent.counterparty}"
        is TransactionIntent.Transfer -> "Transfer: ${intent.sourceAccountName}"
        is TransactionIntent.Unknown -> "${suggestion.merchant ?: "Unknown"}"
        null -> "${suggestion.merchant ?: "Unknown"}"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RM ${"%.2f".format(suggestion.amount)} $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onConfirm) {
            Text("Confirm", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onReject) {
            Text("Skip", style = MaterialTheme.typography.labelSmall)
        }
    }
}
