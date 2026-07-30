package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import com.najmi.sciuro.core.ui.util.SciuroIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SciuroConfirmationDialog
import com.najmi.sciuro.core.ui.components.SciuroTextField
import com.najmi.sciuro.core.ui.components.SheetList


import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.feature.settings.R
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun CategorySettingsScreen(
    onNavigateBack: () -> Unit = {},
    categoryRepository: CategoryRepository = koinInject()
) {
    val expenseLabel = stringResource(R.string.categories_type_expense)
    val incomeLabel = stringResource(R.string.categories_type_income)
    var selectedToggle by remember { mutableStateOf(expenseLabel) }
    var categoryPendingDelete by remember { mutableStateOf<Category?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val categories by categoryRepository.observeCategoriesByType(if (selectedToggle == expenseLabel) "OUTFLOW" else "INFLOW").collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = stringResource(R.string.categories_title),
            heroFigure = { Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary) },
            toggleOptions = listOf(expenseLabel, incomeLabel),
            selectedToggle = selectedToggle,
            onToggleSelected = { selectedToggle = it },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = SciuroIcons.Back,
                        contentDescription = stringResource(R.string.linked_accounts_back),
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
                        stringResource(R.string.categories_manage),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                if (categories.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.categories_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        )
                    }
                }

                items(categories) { category ->
                    SciuroCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.name, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { categoryPendingDelete = category }) {
                                Icon(SciuroIcons.Delete, contentDescription = stringResource(R.string.categories_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(SciuroIcons.Add, contentDescription = stringResource(R.string.categories_add))
            }
        }
    }

    if (showAddDialog) {
        SciuroAddCategoryDialog(
            categoryName = newCategoryName,
            onCategoryNameChange = { newCategoryName = it },
            onConfirm = {
                val type = if (selectedToggle == expenseLabel) "OUTFLOW" else "INFLOW"
                val category = Category(
                    id = UUID.randomUUID().toString(),
                    name = newCategoryName.trim(),
                    type = type
                )
                scope.launch {
                    categoryRepository.createCategory(category)
                }
                newCategoryName = ""
                showAddDialog = false
            },
            onDismiss = {
                newCategoryName = ""
                showAddDialog = false
            }
        )
    }

    categoryPendingDelete?.let { category ->
        SciuroConfirmationDialog(
            title = stringResource(R.string.categories_delete),
            message = stringResource(R.string.categories_delete_confirm, category.name),
            confirmText = stringResource(R.string.categories_delete_action),
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    categoryRepository.deleteCategory(category.id)
                }
                categoryPendingDelete = null
            },
            onDismiss = { categoryPendingDelete = null }
        )
    }
}

@Composable
private fun SciuroAddCategoryDialog(
    categoryName: String,
    onCategoryNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_add)) },
        text = {
            Column {
                Text(stringResource(R.string.categories_add_hint))
                Spacer(modifier = Modifier.height(12.dp))
                SciuroTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = stringResource(R.string.categories_add_hint),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = categoryName.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
