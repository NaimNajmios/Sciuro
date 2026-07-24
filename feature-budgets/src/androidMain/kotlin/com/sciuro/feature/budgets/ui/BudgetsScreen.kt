package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import com.sciuro.core.ledger.config.SettingsProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroFigurePair
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.budget.model.BudgetPeriod
import com.sciuro.core.budget.engine.BudgetLimitSuggester
import com.sciuro.feature.budgets.model.BudgetHealth
import com.sciuro.feature.budgets.viewmodel.BudgetsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.compose.getKoin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    settingsProvider: SettingsProvider = koinInject(),
    viewModel: BudgetsViewModel = koinViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var suggestedAmount by remember { mutableStateOf<Double?>(null) }
    val suggester: BudgetLimitSuggester = getKoin().get()

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier
        .nestedScroll(pullToRefreshState.nestedScrollConnection)
        .fillMaxSize()
    ) {
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
                    title = "Monthly Budgets",
                    heroFigure = if (budgets.isEmpty()) {
                        { Text("0 Active", style = MaterialTheme.typography.headlineLarge, color = Color.White) }
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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                            color = when (budget.health(settingsProvider.getBudgetWarningThreshold())) {
                                                BudgetHealth.OVER -> com.najmi.sciuro.core.ui.theme.SignalDanger
                                                BudgetHealth.APPROACHING -> com.najmi.sciuro.core.ui.theme.SignalWarning
                                                BudgetHealth.HEALTHY -> Color.White.copy(alpha = 0.6f)
                                            }
                                        )
                                    }
                                }
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
                                message = "No budgets yet — set a monthly limit for any category to start tracking against it.",
                                primaryCtaText = "Set your first budget",
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
                                com.najmi.sciuro.core.ui.components.SciuroCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clickable {
                                            editingBudgetId = budget.id
                                            selectedCategoryId = null
                                            amountText = budget.allocatedAmount.roundToInt().toString()
                                            selectedPeriod = BudgetPeriod.MONTHLY
                                            showSheet = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(budget.categoryName, style = MaterialTheme.typography.titleMedium)
                                            Text("RM ${"%.2f".format(budget.currentSpent)} / RM ${"%.2f".format(budget.allocatedAmount)}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val health = budget.health(settingsProvider.getBudgetWarningThreshold())
                                        val progressColor = when (health) {
                                            BudgetHealth.OVER -> com.najmi.sciuro.core.ui.theme.SignalDanger
                                            BudgetHealth.APPROACHING -> com.najmi.sciuro.core.ui.theme.SignalWarning
                                            BudgetHealth.HEALTHY -> MaterialTheme.colorScheme.primary
                                        }
                                        LinearProgressIndicator(
                                            progress = { if (budget.progress > 1f) 1f else budget.progress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            color = progressColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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

            SciuroTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Monthly Limit (RM)",
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

                    SciuroPrimaryButton(
                        text = "Save",
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.updateBudget(
                                    id = editingBudgetId!!,
                                    allocatedAmount = amt,
                                    period = selectedPeriod
                                )
                                showSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
                    )
                }
            } else {
                SciuroPrimaryButton(
                    text = "Create Budget",
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedCategoryId != null) {
                            viewModel.createBudget(
                                categoryId = selectedCategoryId!!,
                                allocatedAmount = amt,
                                period = selectedPeriod
                            )
                            showSheet = false
                        }
                    },
                    enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
                            && selectedCategoryId != null
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
                if (editingBudgetId != null) {
                    viewModel.deleteBudget(editingBudgetId!!)
                }
                showDeleteConfirmation = false
                showSheet = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}


