package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sciuro.core.ledger.db.Transaction_record
import com.sciuro.feature.dashboard.R

@Composable
fun CategoryReviewBanner(
    autoConfirmedTxs: List<Transaction_record>,
    expenseCategories: List<com.sciuro.core.ledger.model.Category>,
    incomeCategories: List<com.sciuro.core.ledger.model.Category>,
    categoryMap: Map<String, com.sciuro.core.ledger.model.Category>,
    onChangeCategory: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (autoConfirmedTxs.isEmpty()) return

    var showPickerTxId by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.RateReview,
                    contentDescription = stringResource(R.string.dashboard_category_review_cd),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    stringResource(R.string.dashboard_category_review),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            autoConfirmedTxs.take(5).forEach { tx ->
                val cat = categoryMap[tx.category_id]
                val catName = cat?.name ?: stringResource(R.string.dashboard_uncategorized)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${tx.merchant ?: "Unknown"} — RM ${"%.2f".format(tx.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            catName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    TextButton(
                        onClick = { showPickerTxId = tx.id },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(R.string.dashboard_change),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (autoConfirmedTxs.size > 5) {
                Text(
                    stringResource(R.string.dashboard_n_more, autoConfirmedTxs.size - 5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }

    showPickerTxId?.let { txId ->
        val tx = autoConfirmedTxs.find { it.id == txId } ?: return
        val categories = if (tx.direction == "INFLOW") incomeCategories else expenseCategories
        var selectedCatId by remember(txId) { mutableStateOf(tx.category_id ?: categories.firstOrNull()?.id ?: "") }

        AlertDialog(
            onDismissRequest = { showPickerTxId = null },
            title = { Text(stringResource(R.string.dashboard_choose_category)) },
            text = {
                Column {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedCatId.isNotEmpty()) {
                        onChangeCategory(txId, selectedCatId)
                    }
                    showPickerTxId = null
                }) {
                    Text(stringResource(R.string.dashboard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPickerTxId = null }) {
                    Text(stringResource(R.string.dashboard_cancel))
                }
            }
        )
    }
}
