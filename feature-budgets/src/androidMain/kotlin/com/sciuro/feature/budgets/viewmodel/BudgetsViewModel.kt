package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.model.Budget
import com.sciuro.core.budget.model.BudgetPeriod
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.feature.budgets.model.BudgetUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
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
            BudgetUiModel(
                id = it.id,
                categoryName = categoryMap[it.category_id]?.name ?: it.category_id,
                allocatedAmount = it.allocated_amount,
                currentSpent = it.current_spent
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createBudget(categoryId: String, allocatedAmount: Double, period: BudgetPeriod) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.createBudget(
                Budget(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    allocatedAmount = allocatedAmount,
                    currentSpent = 0.0,
                    period = period
                )
            )
        }
    }

    fun updateBudget(id: String, allocatedAmount: Double, period: BudgetPeriod) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.updateBudget(id, allocatedAmount, period.name)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.deleteBudget(id)
        }
    }
}
