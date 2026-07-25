package com.sciuro.feature.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.TransactionCard
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.db.Transaction_record

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    transactions: List<Transaction_record>,
    categoryMap: Map<String, Category>,
    onTransactionClick: (Transaction_record) -> Unit,
    onSwipeApprove: (Transaction_record) -> Unit,
    onSwipeReject: (Transaction_record) -> Unit,
    modifier: Modifier = Modifier
) {

    transactions.forEach { tx ->
        val cat = categoryMap[tx.category_id]
        val catColor = cat?.color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant
        val catIcon = mapCategoryIcon(tx.category_id)
        val isTransfer = tx.category_id == "cat_transfer"
        val statusText = if (tx.is_reviewed == 1L) "Reviewed" else "Swipe right to approve, left to reject"

        val cardContent = @Composable {
            TransactionCard(
                merchantName = tx.merchant ?: "Unknown Merchant",
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
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Check
                        SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
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
                            Icon(icon, contentDescription = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) "Approve" else "Reject", tint = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            ) {
                cardContent()
            }
        } else {
            cardContent()
        }
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

private fun mapCategoryIcon(categoryId: String?): ImageVector? {
    return when (categoryId) {
        "cat_dining", "cat_exp_1" -> Icons.Filled.Restaurant
        "cat_groceries", "cat_exp_6" -> Icons.Filled.LocalGroceryStore
        "cat_transport", "cat_exp_2" -> Icons.Filled.DirectionsCar
        "cat_utilities", "cat_exp_3" -> Icons.Filled.Home
        "cat_exp_4" -> Icons.Filled.ShoppingCart
        "cat_exp_5" -> Icons.Filled.Description
        "cat_exp_7" -> Icons.Filled.LocalHospital
        "cat_exp_8" -> Icons.Filled.School
        "cat_exp_9", "cat_inc_6" -> Icons.Filled.MoreHoriz
        "cat_inc_1" -> Icons.Filled.AccountBalance
        "cat_inc_2" -> Icons.Filled.Computer
        "cat_inc_3" -> Icons.Filled.CardGiftcard
        "cat_inc_4" -> Icons.AutoMirrored.Filled.TrendingUp
        "cat_inc_5" -> Icons.Filled.Refresh
        "cat_transfer" -> Icons.Filled.SwapHoriz
        else -> null
    }
}