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
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import java.util.UUID

data class DashboardState(
    val netWorth: Double = 0.0,
    val unreviewedTransactionsCount: Int = 0,
    val activeBudgetsCount: Int = 0,
    val allTransactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList(),
    val accounts: List<com.sciuro.core.ledger.db.Account> = emptyList(),
    val expenseCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val incomeCategories: List<com.sciuro.core.ledger.model.Category> = emptyList()
)

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val notificationSourceAdapter: NotificationSourceAdapter
) : ViewModel() {
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.seedCategories()
        }
    }

    val state: StateFlow<DashboardState> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeUnreviewedTransactions(),
        budgetRepository.observeBudgets(),
        transactionRepository.observeAllTransactions(),
        categoryRepository.observeCategoriesByType("OUTFLOW"),
        categoryRepository.observeCategoriesByType("INFLOW")
    ) { data ->
        val accounts = data[0] as List<com.sciuro.core.ledger.db.Account>
        val unreviewed = data[1] as List<com.sciuro.core.ledger.db.Transaction_record>
        val budgets = data[2] as List<com.sciuro.core.budget.model.Budget>
        val allTransactions = data[3] as List<com.sciuro.core.ledger.db.Transaction_record>
        val expenseCats = data[4] as List<com.sciuro.core.ledger.model.Category>
        val incomeCats = data[5] as List<com.sciuro.core.ledger.model.Category>
        
        DashboardState(
            netWorth = accounts.sumOf { it.balance },
            unreviewedTransactionsCount = unreviewed.size,
            activeBudgetsCount = budgets.size,
            allTransactions = allTransactions,
            accounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats
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

    fun approveTransaction(transactionId: String, accountId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (accountId != null) {
                transactionRepository.reviewTransaction(transactionId, null, accountId)
            } else {
                transactionRepository.approveTransaction(transactionId)
            }
        }
    }

    fun rejectTransaction(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.rejectTransaction(transactionId)
        }
    }

    fun editTransaction(transactionId: String, amount: Double, merchant: String, categoryId: String?, accountId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.editTransaction(
                transactionId = transactionId,
                newAmount = amount,
                newMerchant = merchant,
                newCategoryId = categoryId,
                newAccountId = accountId
            )
        }
    }
    
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.deleteTransaction(transactionId)
        }
    }

    fun simulateNotification(title: String, text: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawEvent = RawEvent(
                id = UUID.randomUUID().toString(),
                sourceType = SourceType.NOTIFICATION,
                sourcePackageOrAddress = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            notificationSourceAdapter.emitNotification(rawEvent)
        }
    }
}
