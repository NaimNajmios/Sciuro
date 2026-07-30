package com.sciuro.feature.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sciuro.feature.wallet.R
import com.najmi.sciuro.core.ui.components.SciuroPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingCategoriesScreen(
    expenseCategories: List<com.sciuro.core.ledger.model.Category>,
    incomeCategories: List<com.sciuro.core.ledger.model.Category>,
    onSave: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategories = remember(expenseCategories, incomeCategories) {
        expenseCategories + incomeCategories
    }

    var enabledIds by remember {
        mutableStateOf(allCategories.map { it.id }.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallet_categories_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                stringResource(R.string.wallet_categories_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.wallet_categories_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.wallet_expense_categories),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                expenseCategories.forEach { cat ->
                    item {
                        CategoryToggleRow(
                            name = cat.name,
                            isEnabled = cat.id in enabledIds,
                            onToggle = {
                                enabledIds = if (it) enabledIds + cat.id else enabledIds - cat.id
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.wallet_income_categories),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                incomeCategories.forEach { cat ->
                    item {
                        CategoryToggleRow(
                            name = cat.name,
                            isEnabled = cat.id in enabledIds,
                            onToggle = {
                                enabledIds = if (it) enabledIds + cat.id else enabledIds - cat.id
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SciuroPrimaryButton(
                text = stringResource(R.string.wallet_save_and_continue),
                onClick = { onSave(enabledIds) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun CategoryToggleRow(
    name: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
