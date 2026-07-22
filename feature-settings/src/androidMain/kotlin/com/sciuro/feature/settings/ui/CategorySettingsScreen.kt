package com.sciuro.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.components.HeroPanel
import com.najmi.sciuro.core.ui.components.SciuroCard
import com.najmi.sciuro.core.ui.components.SheetList
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.repository.CategoryRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CategorySettingsScreen(
    onNavigateBack: () -> Unit = {},
    categoryRepository: CategoryRepository = koinInject()
) {
    var selectedToggle by remember { mutableStateOf("Expense") }
    val scope = rememberCoroutineScope()
    
    val categories by categoryRepository.observeCategoriesByType(if (selectedToggle == "Expense") "OUTFLOW" else "INFLOW").collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            title = "Categories",
            heroFigure = "Config",
            toggleOptions = listOf("Income", "Expense"),
            selectedToggle = selectedToggle,
            onToggleSelected = { selectedToggle = it },
            onBackClick = onNavigateBack
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
                        "Manage Categories",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
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
                            IconButton(onClick = { 
                                scope.launch {
                                    categoryRepository.deleteCategory(category.id)
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
