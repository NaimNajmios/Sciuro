package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.feature.budgets.model.BudgetUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    
    val budgets: StateFlow<List<BudgetUiModel>> = budgetRepository.observeBudgets()
        .map { budgets ->
            budgets.map {
                BudgetUiModel(
                    id = it.id,
                    categoryName = it.category_id, // Would need joining with category table for name
                    allocatedAmount = it.allocated_amount,
                    currentSpent = it.current_spent
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
