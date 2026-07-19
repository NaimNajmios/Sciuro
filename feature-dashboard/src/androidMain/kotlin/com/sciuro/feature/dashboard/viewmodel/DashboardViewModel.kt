package com.sciuro.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val netWorth: Double = 0.0,
    val unreviewedTransactionsCount: Int = 0,
    val activeBudgetsCount: Int = 0
)

class DashboardViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {
    
    val state: StateFlow<DashboardState> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeUnreviewedTransactions(),
        budgetRepository.observeBudgets()
    ) { accounts, unreviewed, budgets ->
        DashboardState(
            netWorth = accounts.sumOf { it.balance },
            unreviewedTransactionsCount = unreviewed.size,
            activeBudgetsCount = budgets.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
}
