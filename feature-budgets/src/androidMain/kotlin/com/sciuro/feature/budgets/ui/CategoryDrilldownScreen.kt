package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.SignalDanger
import com.najmi.sciuro.core.ui.theme.SignalWarning
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.feature.budgets.R
import com.sciuro.feature.budgets.viewmodel.CategoryDrilldownViewModel
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoryDrilldownScreen(
    onNavigateBack: () -> Unit = {},
    settingsProvider: SettingsProvider = koinInject(),
    viewModel: CategoryDrilldownViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val threshold = settingsProvider.getBudgetWarningThreshold()

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.budget_category_spending_title),
            heroFigure = {
                Text(
                    "RM %.0f".format(state.totalSpend),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = IBMPlexMono
                )
            },
            toggleOptions = emptyList(),
            selectedToggle = "",
            onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.budget_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (state.categories.isEmpty()) {
                    EmptyStateView(
                        message = stringResource(R.string.budget_no_outflow)
                    )
                } else {
                    state.categories.forEach { cat ->
                        SciuroCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "RM %.2f".format(cat.spend),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = IBMPlexMono
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val budget = cat.budgetAmount
                                if (budget > 0) {
                                    val percent = (cat.spend / budget).toFloat().coerceIn(0f, 1f)
                                    val barColor = when {
                                        percent >= threshold -> SignalDanger
                                        percent >= threshold - 0.1f -> SignalWarning
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "%.0f%% of RM %.0f budget".format(percent * 100, budget),
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
    }
}
