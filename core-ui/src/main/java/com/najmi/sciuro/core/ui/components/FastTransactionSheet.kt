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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

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
    var amountStr by remember { mutableStateOf("0") }
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
        val displayAmount = if (amountStr.contains('.')) amountStr else amountStr
        Text(
            text = "RM $displayAmount",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = if (direction == "OUTFLOW") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).graphicsLayer { translationX = shakeOffset.value },
            textAlign = TextAlign.Center
        )

        // Direction Toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = direction == "OUTFLOW",
                onClick = { direction = "OUTFLOW"; categoryId = null; showCategoryError = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Expense")
            }
            SegmentedButton(
                selected = direction == "INFLOW",
                onClick = { direction = "INFLOW"; categoryId = null; showCategoryError = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Income")
            }
            SegmentedButton(
                selected = direction == "TRANSFER",
                onClick = { direction = "TRANSFER"; categoryId = null; showCategoryError = false },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("Transfer")
            }
        }

        // Description / Label (Text Field Removed to prevent keyboard conflict)
        SciuroTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = "Description / Label"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(presetLabels) { label ->
                FilterChip(
                    selected = merchant == label,
                    onClick = { merchant = label },
                    label = { Text(label) }
                )
            }
        }

        // Category Selection
        AnimatedVisibility(visible = direction != "TRANSFER") {
            Column {
                Text("Category (Required)", style = MaterialTheme.typography.labelLarge, color = if (showCategoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = if (direction == "OUTFLOW") expenseCategories else incomeCategories
                    items(cats) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { 
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
        Text(if (direction == "TRANSFER") "Source Account" else "Account", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { acc ->
                FilterChip(
                    selected = accountId == acc.id,
                    onClick = { accountId = acc.id },
                    label = { Text(acc.name) }
                )
            }
        }

        AnimatedVisibility(visible = direction == "TRANSFER") {
            Column {
                Text("Destination Account", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts.filter { it.id != accountId }) { acc ->
                        FilterChip(
                            selected = destinationAccountId == acc.id,
                            onClick = { destinationAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Numpad
        Numpad(
            onNumberClick = { num ->
                if (amountStr == "0") {
                    amountStr = num
                } else if (!amountStr.contains('.') || amountStr.substringAfter('.').length < 2) {
                    amountStr += num
                }
            },
            onDecimalClick = {
                if (!amountStr.contains('.')) amountStr += "."
            },
            onBackspaceClick = {
                if (amountStr.length > 1) {
                    amountStr = amountStr.dropLast(1)
                } else {
                    amountStr = "0"
                }
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
                    val finalMerchant = merchant.ifBlank { "Manual Entry" }
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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNumberClick(num) 
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadButton(text = ".", onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDecimalClick()
            }, modifier = Modifier.weight(1f))
            NumpadButton(text = "0", onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNumberClick("0") 
            }, modifier = Modifier.weight(1f))
            NumpadButton(text = "Backspace", icon = Icons.Filled.Backspace, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBackspaceClick()
            }, modifier = Modifier.weight(1f))
        }
        
        SciuroPrimaryButton(
            text = "Save Transaction",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSaveClick()
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
            } else {
                Text(text, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

