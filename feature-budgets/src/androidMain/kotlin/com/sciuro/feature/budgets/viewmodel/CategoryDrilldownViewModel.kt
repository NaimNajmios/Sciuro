package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategorySpendItem(
    val name: String,
    val spend: Double,
    val budgetAmount: Double,
    val transactionCount: Int
)

data class CategoryDrilldownState(
    val categories: List<CategorySpendItem> = emptyList(),
    val totalSpend: Double = 0.0
)

class CategoryDrilldownViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val state: StateFlow<CategoryDrilldownState> = combine(
        transactionRepository.observeAllTransactions(),
        categoryRepository.observeCategoriesByType("OUTFLOW"),
        budgetRepository.observeBudgets()
    ) { transactions, categories, budgets ->
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recentOutflows = transactions.filter { tx ->
            tx.direction == "OUTFLOW" &&
            tx.timestamp >= thirtyDaysAgo
        }

        val byCategory = recentOutflows.groupBy { it.category_id ?: "uncategorised" }
        val items = categories.map { cat ->
            val catTxs = byCategory[cat.id] ?: emptyList()
            val budget = budgets.find { it.category_id == cat.id }
            CategorySpendItem(
                name = cat.name,
                spend = catTxs.sumOf { it.amount },
                budgetAmount = budget?.allocated_amount ?: 0.0,
                transactionCount = catTxs.size
            )
        }.sortedByDescending { it.spend }

        val uncategorisedTxs = byCategory["uncategorised"] ?: emptyList()
        val uncategorised = if (uncategorisedTxs.isNotEmpty()) {
            listOf(CategorySpendItem(
                name = "Uncategorised",
                spend = uncategorisedTxs.sumOf { it.amount },
                budgetAmount = 0.0,
                transactionCount = uncategorisedTxs.size
            ))
        } else emptyList()

        CategoryDrilldownState(
            categories = items + uncategorised,
            totalSpend = items.sumOf { it.spend } + uncategorised.sumOf { it.spend }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDrilldownState()
    )
}
