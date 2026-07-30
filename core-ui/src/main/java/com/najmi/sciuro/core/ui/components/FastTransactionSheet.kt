package com.najmi.sciuro.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.najmi.sciuro.core.ui.util.SciuroHaptics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.najmi.sciuro.core.ui.R
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.najmi.sciuro.core.ui.util.formatDecimalFirstInput

data class FastTxOption(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTransactionSheet(
    presetLabels: List<String>,
    accounts: List<FastTxOption>,
    expenseCategories: List<FastTxOption>,
    incomeCategories: List<FastTxOption>,
    onDismissRequest: () -> Unit,
    onSubmit: (amount: Double, direction: String, merchant: String, categoryId: String?, accountId: String?, destinationAccountId: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf("0.00") }
    var direction by remember { mutableStateOf("OUTFLOW") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id) }
    var destinationAccountId by remember { mutableStateOf<String?>(accounts.firstOrNull { it.id != accountId }?.id) }
    var merchant by remember { mutableStateOf(presetLabels.firstOrNull() ?: "") }
    
    var showCategoryError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    suspend fun triggerShake() {
        val amplitude = 12f
        repeat(3) {
            shakeOffset.animateTo(amplitude, tween(45))
            shakeOffset.animateTo(-amplitude, tween(45))
        }
        shakeOffset.animateTo(0f, tween(45))
    }

    // Auto-update destination account if accountId changes and matches
    LaunchedEffect(accountId) {
        if (direction == "TRANSFER" && destinationAccountId == accountId) {
            destinationAccountId = accounts.firstOrNull { it.id != accountId }?.id
        }
    }

    SciuroBottomSheet(onDismissRequest = onDismissRequest) {
        // Amount Display
        Text(
            text = "RM $amountStr",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = if (direction == "OUTFLOW") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).graphicsLayer { translationX = shakeOffset.value },
            textAlign = TextAlign.Center
        )

        // Direction Toggle
        val expenseLabel = stringResource(R.string.tx_expense)
        val incomeLabel = stringResource(R.string.tx_income)
        val transferLabel = stringResource(R.string.tx_transfer)
        val directionLabels = listOf(expenseLabel, incomeLabel, transferLabel)
        PillToggle(
            options = directionLabels,
            selectedOption = when (direction) {
                "OUTFLOW" -> expenseLabel
                "INFLOW" -> incomeLabel
                else -> transferLabel
            },
            onOptionSelected = { label ->
                direction = when (label) {
                    expenseLabel -> "OUTFLOW"
                    incomeLabel -> "INFLOW"
                    else -> "TRANSFER"
                }
                categoryId = null
                showCategoryError = false
            },
            fillWidth = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        val haptic = LocalHapticFeedback.current

        // Description / Label (Text Field Removed to prevent keyboard conflict)
        SciuroTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = stringResource(R.string.fast_tx_description_label)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(presetLabels) { label ->
                FilterChip(
                    selected = merchant == label,
                    onClick = {
                        SciuroHaptics.selection(haptic)
                        merchant = label
                    },
                    label = { Text(label) }
                )
            }
        }

        // Category Selection
        AnimatedVisibility(visible = direction != "TRANSFER") {
            Column {
                Text(stringResource(R.string.fast_tx_category_required), style = MaterialTheme.typography.labelLarge, color = if (showCategoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = if (direction == "OUTFLOW") expenseCategories else incomeCategories
                    items(cats) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = {
                                SciuroHaptics.selection(haptic)
                                categoryId = cat.id
                                showCategoryError = false
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        // Account Selection
        Text(if (direction == "TRANSFER") stringResource(R.string.fast_tx_source_account) else stringResource(R.string.shared_account), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { acc ->
                FilterChip(
                        selected = accountId == acc.id,
                    onClick = {
                        SciuroHaptics.selection(haptic)
                        accountId = acc.id
                    },
                    label = { Text(acc.name) }
                )
            }
        }

        AnimatedVisibility(visible = direction == "TRANSFER") {
            Column {
                Text(stringResource(R.string.fast_tx_destination_account), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts.filter { it.id != accountId }) { acc ->
                        FilterChip(
                            selected = destinationAccountId == acc.id,
                            onClick = {
                                SciuroHaptics.selection(haptic)
                                destinationAccountId = acc.id
                            },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val manualEntryLabel = stringResource(R.string.tx_manual_entry_title)

        // Numpad
        Numpad(
            onNumberClick = { num ->
                amountStr = formatDecimalFirstInput(amountStr + num)
            },
            onDecimalClick = { },
            onBackspaceClick = {
                val digits = amountStr.filter { it.isDigit() }
                val truncated = digits.dropLast(1)
                amountStr = formatDecimalFirstInput(truncated)
            },
            onSaveClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                val isCategoryValid = direction == "TRANSFER" || categoryId != null
                val isDestinationValid = direction != "TRANSFER" || destinationAccountId != null
                
                if (amt <= 0.0) {
                    scope.launch { triggerShake() }
                } else if (!isCategoryValid) {
                    showCategoryError = true
                } else if (accountId != null && isDestinationValid) {
                    val finalMerchant = merchant.ifBlank { manualEntryLabel }
                    onSubmit(amt, direction, finalMerchant, categoryId, accountId, destinationAccountId)
                }
            }
        )
    }
}

@Composable
fun Numpad(
    onNumberClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (num in row) {
                    NumpadButton(text = num, onClick = { 
                        SciuroHaptics.selection(haptic)
                        onNumberClick(num) 
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadButton(text = ".", onClick = {
                SciuroHaptics.selection(haptic)
                onDecimalClick()
            }, modifier = Modifier.weight(1f))
            NumpadButton(text = "0", onClick = { 
                SciuroHaptics.selection(haptic)
                onNumberClick("0") 
            }, modifier = Modifier.weight(1f))
            NumpadButton(text = stringResource(R.string.shared_backspace), icon = Icons.Filled.Backspace, onClick = {
                SciuroHaptics.selection(haptic)
                onBackspaceClick()
            }, modifier = Modifier.weight(1f))
        }
        
        SciuroPrimaryButton(
            text = stringResource(R.string.tx_save_transaction),
            onClick = {
                onSaveClick()
            },
            icon = SciuroIcons.Check,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
        } else {
            Text(text, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
