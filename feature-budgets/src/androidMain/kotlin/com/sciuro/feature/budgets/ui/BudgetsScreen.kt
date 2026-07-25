package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroFigurePair
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.LocalSnackbarHostState
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalWarning
import com.sciuro.core.budget.model.BudgetPeriod
import com.sciuro.core.budget.engine.BudgetLimitSuggester
import com.sciuro.feature.budgets.model.BudgetHealth
import com.sciuro.feature.budgets.viewmodel.BudgetsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onNavigateToCategoryDrilldown: () -> Unit = {},
    viewModel: BudgetsViewModel = koinViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var suggestedAmount by remember { mutableStateOf<Double?>(null) }
    val suggester: BudgetLimitSuggester = getKoin().get()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                val totalSpent = remember(budgets) { budgets.sumOf { it.currentSpent } }
                val totalAllocated = remember(budgets) { budgets.sumOf { it.allocatedAmount } }
                val atRisk = remember(budgets) {
                    budgets
                        .sortedByDescending { it.progress }
                        .take(3)
                }

                HeroPanel(
                    title = "Budgets",
                    heroFigure = if (budgets.isEmpty()) {
                        { Text("No Active Budgets", style = MaterialTheme.typography.headlineLarge, color = Color.White) }
                    } else {
                        { HeroFigurePair(first = totalSpent, second = totalAllocated) }
                    },
                    toggleOptions = emptyList(),
                    selectedToggle = "",
                    onToggleSelected = {},
                    content = {
                        if (atRisk.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                atRisk.forEach { budget ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = budget.categoryName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "${(budget.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (budget.health()) {
                                                BudgetHealth.OVER -> SignalDanger
                                                BudgetHealth.APPROACHING -> SignalWarning
                                                BudgetHealth.HEALTHY -> Color.White.copy(alpha = 0.6f)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "View Categories",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.clickable { onNavigateToCategoryDrilldown() }
                                )
                            }
                        }
                    }
                )
            }

            item {
                SheetList(modifier = Modifier.offset(y = (-24).dp).fillParentMaxHeight()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (budgets.isEmpty()) {
                            EmptyStateView(
                                message = "No budgets yet \u2014 set a limit for any category to start tracking.",
                                primaryCtaText = "Create Budget",
                                onPrimaryCtaClick = {
                                    selectedCategoryId = null
                                    amountText = ""
                                    selectedPeriod = BudgetPeriod.MONTHLY
                                    editingBudgetId = null
                                    showSheet = true
                                }
                            )
                        } else {
                            budgets.forEach { budget ->
                                SciuroCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clickable {
                                            editingBudgetId = budget.id
                                            selectedCategoryId = null
                                            amountText = budget.allocatedAmount.roundToInt().toString()
                                            selectedPeriod = BudgetPeriod.valueOf(budget.period)
                                            showSheet = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (budget.categoryIcon != null) {
                                                    val iconBg = MaterialTheme.colorScheme.primaryContainer
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(iconBg),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = budget.categoryIcon,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp),
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(budget.categoryName, style = MaterialTheme.typography.titleMedium)
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val periodLabel = when (BudgetPeriod.valueOf(budget.period)) {
                                                            BudgetPeriod.WEEKLY -> "Weekly"
                                                            BudgetPeriod.MONTHLY -> "Monthly"
                                                            BudgetPeriod.YEARLY -> "Yearly"
                                                        }
                                                        Surface(
                                                            shape = MaterialTheme.shapes.small,
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                periodLabel,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (budget.rollover) {
                                                            Text(
                                                                "Rollover",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Text(
                                                "RM ${"%.2f".format(budget.currentSpent)} / RM ${"%.2f".format(budget.allocatedAmount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = IBMPlexMono
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val health = budget.health()
                                        val progressColor = when (health) {
                                            BudgetHealth.OVER -> SignalDanger
                                            BudgetHealth.APPROACHING -> SignalWarning
                                            BudgetHealth.HEALTHY -> MaterialTheme.colorScheme.primary
                                        }
                                        LinearProgressIndicator(
                                            progress = { if (budget.progress > 1f) 1f else budget.progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = progressColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${budget.daysRemaining} days left",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "RM ${"%.2f".format(budget.dailyAllowance)}/day",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = IBMPlexMono,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (budgets.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    selectedCategoryId = null
                    amountText = ""
                    selectedPeriod = BudgetPeriod.MONTHLY
                    editingBudgetId = null
                    showSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Budget")
            }
        }
    }

    if (showSheet) {
        val isEditing = editingBudgetId != null
        val title = if (isEditing) "Edit Budget" else "Create Budget"

        LaunchedEffect(selectedCategoryId) {
            if (!isEditing && selectedCategoryId != null) {
                suggestedAmount = suggester.suggestLimit(selectedCategoryId!!)
            } else {
                suggestedAmount = null
            }
        }

        SciuroBottomSheet(onDismissRequest = { showSheet = false }) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            if (!isEditing) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenseCategories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            val parsedAmount = amountText.toDoubleOrNull()
            val isAmountError = amountText.isNotEmpty() && (parsedAmount == null || parsedAmount <= 0 || parsedAmount > 100_000)
            SciuroTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Monthly Limit (RM)",
                isError = isAmountError,
                supportingText = if (isAmountError) "Enter a valid amount (1 \u2013 100,000)" else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            if (!isEditing && suggestedAmount != null && suggestedAmount!! > 0.0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Suggested:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = { amountText = "%.0f".format(suggestedAmount!!) },
                        label = { Text("RM ${"%.0f".format(suggestedAmount!!)}") }
                    )
                }
            }

            Text("Period", style = MaterialTheme.typography.labelLarge)
            val periodLabels = BudgetPeriod.entries.map { it.name.lowercase().replaceFirstChar { it.uppercaseChar() } }
            PillToggle(
                options = periodLabels,
                selectedOption = selectedPeriod.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                onOptionSelected = { label ->
                    selectedPeriod = BudgetPeriod.entries.first {
                        it.name.lowercase().replaceFirstChar { it.uppercaseChar() } == label
                    }
                },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }

                    val isValidAmount = parsedAmount != null && parsedAmount > 0 && parsedAmount <= 100_000
                    SciuroPrimaryButton(
                        text = "Save",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.updateBudget(
                                    id = editingBudgetId!!,
                                    allocatedAmount = parsedAmount!!,
                                    period = selectedPeriod
                                )
                                showSheet = false
                                snackbarHostState.showSnackbar("Budget updated")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isValidAmount
                    )
                }
            } else {
                val isValidAmount = parsedAmount != null && parsedAmount > 0 && parsedAmount <= 100_000
                SciuroPrimaryButton(
                    text = "Create Budget",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.createBudget(
                                categoryId = selectedCategoryId!!,
                                allocatedAmount = parsedAmount!!,
                                period = selectedPeriod
                            )
                            showSheet = false
                            val catName = expenseCategories.find { it.id == selectedCategoryId }?.name ?: "category"
                            snackbarHostState.showSnackbar("Budget created for $catName")
                        }
                    },
                    enabled = isValidAmount && selectedCategoryId != null
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        SciuroConfirmationDialog(
            title = "Delete Budget",
            message = "Are you sure you want to delete this budget? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                coroutineScope.launch {
                    viewModel.deleteBudget(editingBudgetId!!)
                    showDeleteConfirmation = false
                    showSheet = false
                    snackbarHostState.showSnackbar("Budget deleted")
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}
