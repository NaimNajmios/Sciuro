package com.sciuro.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.PillToggle
import com.najmi.sciuro.core.ui.components.SciuroBottomSheet
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.model.MerchantAccountRuleUiModel
import com.sciuro.core.ledger.model.MerchantRuleUiModel
import com.sciuro.feature.settings.R
import com.sciuro.feature.settings.viewmodel.MerchantRulesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MerchantRulesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MerchantRulesViewModel = koinViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var rulePendingDelete by remember { mutableStateOf<MerchantRuleUiModel?>(null) }
    var rulePendingOverride by remember { mutableStateOf<MerchantRuleUiModel?>(null) }
    var accountRulePendingDelete by remember { mutableStateOf<MerchantAccountRuleUiModel?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.merchant_rules_title),
            heroFigure = { Text(stringResource(R.string.merchant_rules_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = listOf("Categories", "Accounts"),
            selectedToggle = uiState.selectedTab,
            onToggleSelected = { viewModel.setSelectedTab(it) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.merchant_rules_back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        SheetList(modifier = Modifier.offset(y = (-24).dp).fillMaxWidth().weight(1f)) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Text(
                        stringResource(if (uiState.selectedTab == "Categories") R.string.merchant_rules_manage else R.string.merchant_rules_account_manage),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                if (uiState.selectedTab == "Categories") {
                    if (uiState.rules.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.merchant_rules_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                            )
                        }
                    }

                    items(uiState.rules) { rule ->
                        MerchantRuleCard(
                            rule = rule,
                            onDelete = { rulePendingDelete = it },
                            onOverride = { rulePendingOverride = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                } else {
                    if (uiState.accountRules.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.merchant_rules_account_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                            )
                        }
                    }

                    items(uiState.accountRules) { rule ->
                        MerchantAccountRuleCard(
                            rule = rule,
                            onDelete = { accountRulePendingDelete = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    rulePendingDelete?.let { rule ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.merchant_rules_delete),
            message = stringResource(R.string.merchant_rules_delete_confirm, rule.displayName),
            confirmText = stringResource(R.string.merchant_rules_delete_action),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteRule(rule.merchantKey)
                rulePendingDelete = null
            },
            onDismiss = { rulePendingDelete = null }
        )
    }

    rulePendingOverride?.let { rule ->
        OverrideCategorySheet(
            categories = uiState.categories,
            currentCategoryName = rule.categoryName,
            onSelect = { newCategoryId ->
                viewModel.overrideRule(rule.merchantKey, newCategoryId)
                rulePendingOverride = null
            },
            onDismiss = { rulePendingOverride = null }
        )
    }

    accountRulePendingDelete?.let { rule ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.merchant_rules_account_delete),
            message = stringResource(R.string.merchant_rules_account_delete_confirm, rule.displayName, rule.accountName),
            confirmText = stringResource(R.string.merchant_rules_delete_action),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAccountRule(rule.merchantKey, rule.accountId)
                accountRulePendingDelete = null
            },
            onDismiss = { accountRulePendingDelete = null }
        )
    }
}

@Composable
private fun MerchantRuleCard(
    rule: MerchantRuleUiModel,
    onDelete: (MerchantRuleUiModel) -> Unit,
    onOverride: (MerchantRuleUiModel) -> Unit
) {
    SciuroCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        onClick = { onOverride(rule) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.displayName, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    TrustBadge(isTrusted = rule.isTrusted)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.merchant_rules_category, rule.categoryName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.merchant_rules_confirmation_count, rule.confirmationCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = { onDelete(rule) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.merchant_rules_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MerchantAccountRuleCard(
    rule: MerchantAccountRuleUiModel,
    onDelete: (MerchantAccountRuleUiModel) -> Unit
) {
    SciuroCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.displayName, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Account: ${rule.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.merchant_rules_confirmation_count, rule.confirmationCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = { onDelete(rule) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.merchant_rules_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TrustBadge(isTrusted: Boolean) {
    val color = if (isTrusted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val icon = if (isTrusted) Icons.Filled.Check else Icons.Filled.Close
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun OverrideCategorySheet(
    categories: List<Category>,
    currentCategoryName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SciuroBottomSheet(onDismissRequest = onDismiss) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.merchant_rules_override_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.merchant_rules_override_hint, currentCategoryName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category.id) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (category.name == currentCategoryName) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
