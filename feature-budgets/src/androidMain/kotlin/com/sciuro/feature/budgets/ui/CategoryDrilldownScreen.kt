package com.sciuro.feature.budgets.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.theme.LocalSciuroSemanticTokens
import com.najmi.sciuro.core.ui.theme.SciuroSemanticTokens
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.najmi.sciuro.core.ui.util.mapCategoryIcon
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.feature.budgets.R
import com.sciuro.feature.budgets.viewmodel.CategoryDrilldownState
import com.sciuro.feature.budgets.viewmodel.CategoryDrilldownViewModel
import com.sciuro.feature.budgets.viewmodel.CategorySection
import com.sciuro.feature.budgets.viewmodel.CategorySpendGroup
import com.sciuro.feature.budgets.viewmodel.CategorySpendItem
import com.sciuro.feature.budgets.viewmodel.TimePeriod
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CategoryDrilldownScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToCategoryTransactions: (categoryId: String, categoryName: String) -> Unit = { _, _ -> },
    settingsProvider: SettingsProvider = koinInject(),
    viewModel: CategoryDrilldownViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val threshold = settingsProvider.getBudgetWarningThreshold()
    val tokens = LocalSciuroSemanticTokens.current

    val periodLabels = mapOf(
        TimePeriod.LAST_30_DAYS to stringResource(R.string.budget_period_30_days),
        TimePeriod.THIS_MONTH to stringResource(R.string.budget_period_this_month),
        TimePeriod.LAST_MONTH to stringResource(R.string.budget_period_last_month),
        TimePeriod.LAST_3_MONTHS to stringResource(R.string.budget_period_3_months)
    )

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "drilldownState"
    ) { currentState ->
        when (currentState) {
            is CategoryDrilldownState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CategoryDrilldownState.Loaded -> {
                CategoryDrilldownContent(
                    state = currentState,
                    threshold = threshold,
                    tokens = tokens,
                    periodLabels = periodLabels,
                    onNavigateBack = onNavigateBack,
                    onNavigateToCategoryTransactions = onNavigateToCategoryTransactions,
                    onPeriodSelected = { viewModel.setTimePeriod(it) }
                )
            }
            is CategoryDrilldownState.Empty -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    HeroPanel(
                        title = stringResource(R.string.budget_category_spending_title),
                        heroFigure = { Text("RM 0", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary, fontFamily = IBMPlexMono) },
                        toggleOptions = emptyList(), selectedToggle = "", onToggleSelected = {},
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(SciuroIcons.Back, contentDescription = stringResource(R.string.budget_back), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    )
                    SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        EmptyStateView(message = stringResource(R.string.budget_empty_drilldown), fallbackIcon = SciuroIcons.NavBudgets)
                    }
                }
            }
            is CategoryDrilldownState.Error -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    HeroPanel(
                        title = stringResource(R.string.budget_category_spending_title),
                        heroFigure = { Text("—", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary, fontFamily = IBMPlexMono) },
                        toggleOptions = emptyList(), selectedToggle = "", onToggleSelected = {},
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(SciuroIcons.Back, contentDescription = stringResource(R.string.budget_back), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    )
                    SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        EmptyStateView(
                            message = stringResource(R.string.budget_error_drilldown),
                            fallbackIcon = SciuroIcons.Warning,
                            primaryCtaText = stringResource(R.string.budget_retry),
                            onPrimaryCtaClick = { viewModel.setTimePeriod(TimePeriod.LAST_30_DAYS) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDrilldownContent(
    state: CategoryDrilldownState.Loaded,
    threshold: Float,
    tokens: SciuroSemanticTokens,
    periodLabels: Map<TimePeriod, String>,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryTransactions: (String, String) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    var selectedPeriodLabel by remember { mutableStateOf(periodLabels[TimePeriod.LAST_30_DAYS] ?: "") }
    val periodOptions = listOf(
        TimePeriod.LAST_30_DAYS,
        TimePeriod.THIS_MONTH,
        TimePeriod.LAST_MONTH,
        TimePeriod.LAST_3_MONTHS
    )

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
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.budget_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            toggleOptions = emptyList(), selectedToggle = "", onToggleSelected = {},
            chartData = state.sparklineData.ifEmpty { null },
            content = {
                val topName = state.topCategoryName
                if (topName != null) {
                    Text(
                        text = stringResource(R.string.budget_stats_format, state.categoryCount, topName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                PillToggle(
                    options = periodOptions.map { periodLabels[it] ?: "" },
                    selectedOption = selectedPeriodLabel,
                    onOptionSelected = { label ->
                        selectedPeriodLabel = label
                        val period = periodOptions.find { periodLabels[it] == label }
                        if (period != null) onPeriodSelected(period)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                state.sections.forEach { section ->
                    CategorySectionHeader(section = section, tokens = tokens)
                    Spacer(modifier = Modifier.height(8.dp))

                    section.items.forEach { item ->
                        if (item.group == CategorySpendGroup.UNCATEGORISED) {
                            UncategorisedCard(
                                item = item,
                                onViewTransactions = { onNavigateToCategoryTransactions(item.categoryId, item.name) }
                            )
                        } else {
                            CategoryCard(
                                item = item,
                                tokens = tokens,
                                onViewTransactions = { onNavigateToCategoryTransactions(item.categoryId, item.name) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    section: CategorySection,
    tokens: SciuroSemanticTokens
) {
    val color = when (section.group) {
        CategorySpendGroup.OVER_BUDGET -> tokens.signalDanger
        CategorySpendGroup.APPROACHING_LIMIT -> tokens.signalWarning
        CategorySpendGroup.ON_TRACK -> tokens.signalIncome
        CategorySpendGroup.NO_BUDGET -> MaterialTheme.colorScheme.onSurfaceVariant
        CategorySpendGroup.UNCATEGORISED -> tokens.signalDanger
    }
    val labelRes = when (section.group) {
        CategorySpendGroup.OVER_BUDGET -> R.string.budget_section_over
        CategorySpendGroup.APPROACHING_LIMIT -> R.string.budget_section_approaching
        CategorySpendGroup.ON_TRACK -> R.string.budget_section_on_track
        CategorySpendGroup.NO_BUDGET -> R.string.budget_section_no_budget
        CategorySpendGroup.UNCATEGORISED -> R.string.budget_section_uncategorised
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelRes).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = "%d".format(section.items.size),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CategoryCard(
    item: CategorySpendItem,
    tokens: SciuroSemanticTokens,
    onViewTransactions: () -> Unit
) {
    val icon = mapCategoryIcon(item.categoryId)
    val progressColor = when (item.group) {
        CategorySpendGroup.OVER_BUDGET -> tokens.signalDanger
        CategorySpendGroup.APPROACHING_LIMIT -> tokens.signalWarning
        else -> MaterialTheme.colorScheme.primary
    }

    SciuroCard(modifier = Modifier.fillMaxWidth()) {
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon ?: SciuroIcons.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        if (item.budgetPeriod != null) {
                            val periodLabel = when (item.budgetPeriod) {
                                "WEEKLY" -> stringResource(R.string.budget_weekly)
                                "MONTHLY" -> stringResource(R.string.budget_monthly)
                                "YEARLY" -> stringResource(R.string.budget_yearly)
                                else -> item.budgetPeriod
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
                        }
                    }
                }
                Text(
                    "RM %.2f".format(item.spend),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = IBMPlexMono
                )
            }

            if (item.budgetAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val percent = (item.spend / item.budgetAmount).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (item.remaining > 0) {
                        Text(
                            stringResource(R.string.budget_card_remaining, "%.0f".format(item.remaining)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.dailyAllowance > 0) {
                        Text(
                            stringResource(R.string.budget_card_per_day, "%.0f".format(item.dailyAllowance)),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = IBMPlexMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.remaining <= 0 && item.dailyAllowance <= 0) {
                        Text(
                            "%.0f%%".format((percent * 100).coerceAtMost(100f)),
                            style = MaterialTheme.typography.bodySmall,
                            color = progressColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewTransactions),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        SciuroIcons.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.budget_card_transactions, item.transactionCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.budget_card_view_transactions),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        SciuroIcons.Forward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun UncategorisedCard(
    item: CategorySpendItem,
    onViewTransactions: () -> Unit
) {
    SciuroCard(modifier = Modifier.fillMaxWidth()) {
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SciuroIcons.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.budget_card_transactions, item.transactionCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "RM %.2f".format(item.spend),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = IBMPlexMono
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewTransactions),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.budget_card_categorise_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Icon(
                        SciuroIcons.Forward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
