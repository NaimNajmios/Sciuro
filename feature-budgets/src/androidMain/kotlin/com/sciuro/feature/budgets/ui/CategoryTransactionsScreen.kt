package com.sciuro.feature.budgets.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.EmptyStateView
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SheetList
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import com.najmi.sciuro.core.ui.util.SciuroIcons
import com.sciuro.feature.budgets.R

@Composable
fun CategoryTransactionsScreen(
    categoryId: String,
    categoryName: String,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = categoryName,
            heroFigure = {
                Text(
                    categoryName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = IBMPlexMono
                )
            },
            toggleOptions = emptyList(), selectedToggle = "", onToggleSelected = {},
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.budget_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(24.dp))
            EmptyStateView(
                message = "Transactions for $categoryName coming soon.",
                fallbackIcon = SciuroIcons.Receipt
            )
        }
    }
}
