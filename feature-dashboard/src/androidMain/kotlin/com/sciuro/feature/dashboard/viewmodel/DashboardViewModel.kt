package com.sciuro.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
    val activeBudgetsCount: Int = 0,
    val allTransactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList(),
    val accounts: List<com.sciuro.core.ledger.db.Account> = emptyList()
)

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    
    val state: StateFlow<DashboardState> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeUnreviewedTransactions(),
        budgetRepository.observeBudgets(),
        transactionRepository.observeAllTransactions()
    ) { accounts, unreviewed, budgets, allTransactions ->
        DashboardState(
            netWorth = accounts.sumOf { it.balance },
            unreviewedTransactionsCount = unreviewed.size,
            activeBudgetsCount = budgets.size,
            allTransactions = allTransactions,
            accounts = accounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // Removed ensureDefaultAccountExists() as it's now handled by the Onboarding flow.

    fun bookManualTransaction(
        amount: Double,
        direction: String,
        merchant: String,
        accountId: String?,
        categoryId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = com.sciuro.core.ledger.model.Transaction(
                id = java.util.UUID.randomUUID().toString(),
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                direction = direction,
                merchant = merchant,
                timestamp = System.currentTimeMillis(),
                referenceId = null,
                isReviewed = true // Manual transactions are inherently reviewed
            )
            transactionRepository.bookTransaction(
                transaction = transaction,
                source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL,
                confidence = 1.0f
            )
        }
    }

    fun approveTransaction(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.approveTransaction(transactionId)
        }
    }

    fun rejectTransaction(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.rejectTransaction(transactionId)
        }
    }
}
