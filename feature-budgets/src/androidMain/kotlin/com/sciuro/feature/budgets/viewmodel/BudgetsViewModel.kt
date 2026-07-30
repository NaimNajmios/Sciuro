package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.budget.model.Budget
import com.sciuro.core.budget.model.BudgetPeriod
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.feature.budgets.model.BudgetUiModel
import com.najmi.sciuro.core.ui.util.mapCategoryIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.UUID

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetEngine: BudgetEngine
) : ViewModel() {

    val expenseCategories: StateFlow<List<Category>> = categoryRepository
        .observeCategoriesByType("OUTFLOW")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetUiModel>> = combine(
        budgetRepository.observeBudgets(),
        expenseCategories
    ) { budgets, categories ->
        val categoryMap = categories.associateBy { it.id }
        budgets.map {
            val cat = categoryMap[it.category_id]
            BudgetUiModel(
                id = it.id,
                categoryName = cat?.name ?: it.category_id,
                allocatedAmount = it.allocated_amount,
                currentSpent = it.current_spent,
                alertThresholdPercent = it.alert_threshold_percent,
                categoryIcon = mapCategoryIcon(it.category_id).let { icon -> icon ?: cat?.icon?.let { mapCategoryIcon(it) } },
                categoryColor = cat?.color,
                period = it.period,
                rollover = it.rollover == 1L
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun createBudget(categoryId: String, allocatedAmount: Double, period: BudgetPeriod) {
        withContext(Dispatchers.IO) {
            budgetRepository.createBudget(
                Budget(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    allocatedAmount = allocatedAmount,
                    currentSpent = 0.0,
                    period = period
                )
            )
            budgetEngine.processBudgets()
        }
    }

    suspend fun updateBudget(id: String, allocatedAmount: Double, period: BudgetPeriod) {
        withContext(Dispatchers.IO) {
            budgetRepository.updateBudget(id, allocatedAmount, period.name)
            budgetEngine.processBudgets()
        }
    }

    suspend fun deleteBudget(id: String) {
        withContext(Dispatchers.IO) {
            budgetRepository.deleteBudget(id)
        }
    }
}
