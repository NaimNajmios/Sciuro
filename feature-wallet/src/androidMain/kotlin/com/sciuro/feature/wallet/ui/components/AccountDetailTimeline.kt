package com.sciuro.feature.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.TransactionCard
import com.najmi.sciuro.core.ui.components.AdjustmentCard
import com.najmi.sciuro.core.ui.components.SciuroSectionHeader
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.najmi.sciuro.core.ui.util.mapCategoryIcon
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.sciuro.feature.wallet.viewmodel.TimelineItem
import com.sciuro.feature.wallet.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val filterOptions = listOf("All", "Transactions", "Adjustments", "Income", "Expense")

@Composable
fun AccountDetailTimeline(
    timeline: List<TimelineItem>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    categoryMap: Map<String, com.sciuro.core.ledger.model.Category>,
    onTransactionClick: (com.sciuro.core.ledger.db.Transaction_record) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.animateContentSize()) {
        Text(
            stringResource(R.string.wallet_transaction_history),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        PillToggle(
            options = filterOptions,
            selectedOption = selectedFilter,
            onOptionSelected = onFilterSelected,
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
            scrollable = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (timeline.isEmpty()) {
            EmptyStateView(
                message = when {
                    selectedFilter == "Adjustments" -> stringResource(R.string.wallet_empty_no_adjustments_recorded)
                    selectedFilter == "All" -> stringResource(R.string.wallet_empty_no_tx_or_adjustments)
                    else -> stringResource(R.string.wallet_empty_no_items_filter)
                },
                fallbackIcon = SciuroIcons.Receipt
            )
        } else {
            MonthGroupedTimeline(
                timeline = timeline,
                categoryMap = categoryMap,
                onTransactionClick = onTransactionClick
            )
        }
    }
}

@Composable
private fun MonthGroupedTimeline(
    timeline: List<TimelineItem>,
    categoryMap: Map<String, com.sciuro.core.ledger.model.Category>,
    onTransactionClick: (com.sciuro.core.ledger.db.Transaction_record) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    var lastMonth = ""
    val groupedItems = mutableListOf<Pair<String, List<TimelineItem>>>()
    val currentGroup = mutableListOf<TimelineItem>()

    for (item in timeline) {
        val ts = when (item) {
            is TimelineItem.TransactionItem -> item.tx.timestamp
            is TimelineItem.AdjustmentItem -> item.adjustment.timestamp
        }
        val month = monthFormat.format(Date(ts))
        if (month != lastMonth && currentGroup.isNotEmpty()) {
            groupedItems.add(lastMonth to currentGroup.toList())
            currentGroup.clear()
        }
        lastMonth = month
        currentGroup.add(item)
    }
    if (currentGroup.isNotEmpty()) {
        groupedItems.add(lastMonth to currentGroup.toList())
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for ((month, items) in groupedItems) {
            val total = items.sumOf { item ->
                when (item) {
                    is TimelineItem.TransactionItem -> item.tx.amount
                    is TimelineItem.AdjustmentItem -> item.adjustment.amount
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "RM ${"%.2f".format(total)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = IBMPlexMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            items.forEach { item ->
                when (item) {
                    is TimelineItem.TransactionItem -> {
                        val tx = item.tx
                        val isTransfer = tx.category_id == "cat_transfer"
                        val statusText = if (tx.is_reviewed == 1L) stringResource(R.string.wallet_reviewed) else stringResource(R.string.wallet_unreviewed)
                        val cat = categoryMap[tx.category_id]
                        val catColor = cat?.color?.let { parseColorRef(it) } ?: MaterialTheme.colorScheme.surfaceVariant
                        val catIcon = mapCategoryIcon(tx.category_id)
                        TransactionCard(
                            merchantName = tx.merchant ?: stringResource(R.string.wallet_unknown_merchant),
                            amount = "RM ${"%.2f".format(tx.amount)}",
                            direction = tx.direction,
                            statusText = statusText,
                            categoryIcon = catIcon,
                            categoryColor = catColor,
                            isTransfer = isTransfer,
                            confidence = tx.confidence,
                            extractionMethod = tx.extraction_method,
                            onClick = { onTransactionClick(tx) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    is TimelineItem.AdjustmentItem -> {
                        val adj = item.adjustment
                        AdjustmentCard(reason = adj.reason, amount = adj.amount)
                    }
                }
            }
        }
    }
}

private fun parseColorRef(hex: String?): Color? {
    if (hex == null) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}
