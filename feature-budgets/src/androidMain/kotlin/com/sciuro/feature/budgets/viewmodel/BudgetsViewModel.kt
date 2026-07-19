package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.feature.budgets.model.BudgetUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetsViewModel : ViewModel() {
    private val _budgets = MutableStateFlow<List<BudgetUiModel>>(emptyList())
    val budgets: StateFlow<List<BudgetUiModel>> = _budgets.asStateFlow()
    
    init {
        // Mock data for Phase C3 scaffolding
        _budgets.value = listOf(
            BudgetUiModel("1", "Food & Dining", 500.0, 345.50),
            BudgetUiModel("2", "Transport", 200.0, 210.00),
            BudgetUiModel("3", "Entertainment", 150.0, 50.0)
        )
    }
}
