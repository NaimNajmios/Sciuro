package com.sciuro.feature.dashboard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.TransactionCard
import com.najmi.sciuro.core.ui.util.SciuroHaptics
import com.najmi.sciuro.core.ui.util.mapCategoryIcon
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.db.Transaction_record
import com.sciuro.feature.dashboard.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    transactions: List<Transaction_record>,
    categoryMap: Map<String, Category>,
    onTransactionClick: (Transaction_record) -> Unit,
    onSwipeApprove: (Transaction_record) -> Unit,
    onSwipeReject: (Transaction_record) -> Unit,
    selectedRange: String = "Today",
    @Suppress("UnusedParameter")
    modifier: Modifier = Modifier
) {
    var contextMenuTx by remember { mutableStateOf<Transaction_record?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val showDateHeaders = selectedRange == "This Week" || selectedRange == "This Month"

    val dayFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    if (showDateHeaders) {
        val grouped = transactions.groupBy { dayFormat.format(Date(it.timestamp)) }
        grouped.forEach { (date, txs) ->
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            txs.forEach { tx ->
                TransactionListItem(
                    tx = tx,
                    categoryMap = categoryMap,
                    contextMenuTx = contextMenuTx,
                    onContextMenuChange = { contextMenuTx = it },
                    context = context,
                    haptic = haptic,
                    onTransactionClick = onTransactionClick,
                    onSwipeApprove = onSwipeApprove,
                    onSwipeReject = onSwipeReject
                )
            }
        }
    } else {
        transactions.forEach { tx ->
            TransactionListItem(
                tx = tx,
                categoryMap = categoryMap,
                contextMenuTx = contextMenuTx,
                onContextMenuChange = { contextMenuTx = it },
                context = context,
                haptic = haptic,
                onTransactionClick = onTransactionClick,
                onSwipeApprove = onSwipeApprove,
                onSwipeReject = onSwipeReject
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListItem(
    tx: Transaction_record,
    categoryMap: Map<String, Category>,
    contextMenuTx: Transaction_record?,
    onContextMenuChange: (Transaction_record?) -> Unit,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onTransactionClick: (Transaction_record) -> Unit,
    onSwipeApprove: (Transaction_record) -> Unit,
    onSwipeReject: (Transaction_record) -> Unit
) {
    val cat = categoryMap[tx.category_id]
    val catColor = cat?.color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val catIcon = mapCategoryIcon(tx.category_id)
    val isTransfer = tx.category_id == "cat_transfer"
    val statusText = if (tx.is_reviewed == 1L) stringResource(R.string.dashboard_reviewed) else stringResource(R.string.dashboard_swipe_instructions)

    val cardContent = @Composable {
        TransactionCard(
            merchantName = tx.merchant ?: stringResource(R.string.dashboard_unknown_merchant),
            amount = "RM ${"%.2f".format(tx.amount)}",
            direction = tx.direction,
            statusText = statusText,
            categoryIcon = catIcon,
            categoryColor = catColor,
            isTransfer = isTransfer,
            confidence = tx.confidence,
            extractionMethod = tx.extraction_method,
            onClick = { onTransactionClick(tx) },
            onLongClick = {
                SciuroHaptics.success(haptic)
                onContextMenuChange(tx)
            }
        )
    }

    if (tx.is_reviewed == 0L) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                when(it) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onSwipeApprove(tx)
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onSwipeReject(tx)
                        true
                    }
                    else -> false
                }
            }
        )
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> com.najmi.sciuro.core.ui.theme.SignalIncome
                    SwipeToDismissBoxValue.EndToStart -> com.najmi.sciuro.core.ui.theme.SignalDanger
                    else -> Color.Transparent
                }
                val icon = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> SciuroIcons.Check
                    SwipeToDismissBoxValue.EndToStart -> SciuroIcons.Delete
                    else -> null
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                        .clip(CardDefaults.shape)
                        .background(color),
                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) stringResource(R.string.dashboard_approve_cd) else stringResource(R.string.dashboard_reject_cd), tint = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        ) {
            cardContent()
        }
    } else {
        cardContent()
    }

    DropdownMenu(
        expanded = contextMenuTx?.id == tx.id,
        onDismissRequest = { onContextMenuChange(null) }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dashboard_context_copy_amount)) },
            onClick = {
                val amountText = "RM ${"%.2f".format(tx.amount)}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("amount", amountText))
                onContextMenuChange(null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dashboard_context_mark_transfer)) },
            onClick = {
                onTransactionClick(tx)
                onContextMenuChange(null)
            }
        )
    }
}

private fun parseColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}
