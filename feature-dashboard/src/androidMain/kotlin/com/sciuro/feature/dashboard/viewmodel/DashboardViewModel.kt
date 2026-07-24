package com.sciuro.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.repository.CashAdjustmentRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.transfer.model.TransferLink
import com.sciuro.core.transfer.repository.TransferRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.investment.repository.InvestmentRepository
import com.sciuro.core.obligations.engine.IncomeRecurrencePatternDetector
import com.sciuro.core.obligations.repository.ObligationRepository

data class DashboardState(
    val netPosition: Double = 0.0,
    val unreviewedTransactionsCount: Int = 0,
    val autoBookedTransactionsCount: Int = 0,
    val activeBudgetsCount: Int = 0,
    val allTransactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList(),
    val accounts: List<com.sciuro.core.ledger.db.Account> = emptyList(),
    val expenseCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val incomeCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val recentAdjustmentCount: Int = 0,
    val balanceHistory: List<Float> = emptyList(),
    val runway: Double = 0.0,
    val hasIncomePattern: Boolean = false
)

data class TransactionDetailData(
    val auditLogs: List<AuditLog> = emptyList(),
    val transferLink: TransferLink? = null,
    val rawEvent: Raw_event_staging? = null
)

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transferRepository: TransferRepository,
    private val cashAdjustmentRepository: CashAdjustmentRepository,
    private val auditRepository: AuditRepository,
    private val rawEventRepository: RawEventRepository,
    private val debtRepository: DebtRepository,
    private val investmentRepository: InvestmentRepository,
    private val obligationRepository: ObligationRepository,
    private val incomeDetector: IncomeRecurrencePatternDetector
) : ViewModel() {
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.seedCategories()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(600)
            _isRefreshing.value = false
        }
    }

    val state: StateFlow<DashboardState> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeUnreviewedTransactions(),
        budgetRepository.observeBudgets(),
        transactionRepository.observeAllTransactions(),
        categoryRepository.observeCategoriesByType("OUTFLOW"),
        categoryRepository.observeCategoriesByType("INFLOW"),
        cashAdjustmentRepository.observeAllAdjustments(),
        debtRepository.observeDebts(),
        investmentRepository.observeInvestments(),
        obligationRepository.observeActiveObligations()
    ) { data ->
        val accounts = data[0] as List<com.sciuro.core.ledger.db.Account>
        val unreviewed = data[1] as List<com.sciuro.core.ledger.db.Transaction_record>
        val budgets = data[2] as List<com.sciuro.core.budget.model.Budget>
        val allTransactions = data[3] as List<com.sciuro.core.ledger.db.Transaction_record>
        val expenseCats = data[4] as List<com.sciuro.core.ledger.model.Category>
        val incomeCats = data[5] as List<com.sciuro.core.ledger.model.Category>
        val allAdjustments = data[6] as List<com.sciuro.core.ledger.db.Cash_adjustment>
        val debts = data[7] as List<*>
        val investments = data[8] as List<*>
        val obligations = data[9] as List<com.sciuro.core.obligations.model.Obligation>
        
        val oneWeekAgo = currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        val recentAdjustments = allAdjustments.filter { it.timestamp > oneWeekAgo }
        
        val balanceHistory = computeBalanceHistory(allTransactions)
        
        val totalAccounts = accounts.sumOf { it.balance }
        val totalInvestments = investments.filterIsInstance<com.sciuro.core.investment.model.Investment>().sumOf { (it.unitsHeld * it.averageBuyPrice).toDouble() }
        val totalDebts = debts.filterIsInstance<com.sciuro.core.debt.model.Debt>().sumOf {
            if (it.direction == DebtDirection.OWED_TO_ME) it.remainingBalance.toDouble()
            else -it.remainingBalance.toDouble()
        }
        val netPosition = totalAccounts + totalInvestments + totalDebts

        val incomePattern = incomeDetector.detectAndPublish()
        val thirtyDaysFromNow = currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L
        val nextIncome = incomePattern?.nextExpectedDate ?: thirtyDaysFromNow
        val expectedIncome = incomePattern?.amount ?: 0.0

        val obligationsDue = obligations.filter {
            it.nextDueDate <= nextIncome
        }.sumOf { it.amount }

        val debtsDue = debts.filterIsInstance<com.sciuro.core.debt.model.Debt>().filter { debt ->
            debt.direction == DebtDirection.I_OWE && debt.dueDate != null && debt.dueDate!! <= nextIncome
        }.sumOf { it.remainingBalance.toDouble() }

        val runway = totalAccounts + expectedIncome - obligationsDue - debtsDue
        
        DashboardState(
            netPosition = netPosition,
            unreviewedTransactionsCount = unreviewed.size,
            activeBudgetsCount = budgets.size,
            allTransactions = allTransactions,
            accounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            recentAdjustmentCount = recentAdjustments.size,
            balanceHistory = balanceHistory,
            runway = runway,
            hasIncomePattern = incomePattern != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    val autoBookedTransactionsCount: StateFlow<Int> = transactionRepository
        .observeRecentlyAutoConfirmed(currentTimeMillis() - 24L * 60L * 60L * 1000L)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoBookedTransactions: StateFlow<List<com.sciuro.core.ledger.db.Transaction_record>> = transactionRepository
        .observeRecentlyAutoConfirmed(currentTimeMillis() - 24L * 60L * 60L * 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Removed ensureDefaultAccountExists() as it's now handled by the Onboarding flow.

    fun bookManualTransaction(
        amount: Double,
        direction: String,
        merchant: String,
        accountId: String?,
        categoryId: String?,
        destinationAccountId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (direction == "TRANSFER" && destinationAccountId != null) {
                val outTxId = java.util.UUID.randomUUID().toString()
                val inTxId = java.util.UUID.randomUUID().toString()
                
                val outTx = com.sciuro.core.ledger.model.Transaction(
                    id = outTxId, accountId = accountId, categoryId = "cat_transfer", amount = amount, direction = "OUTFLOW",
                    merchant = "Transfer to $destinationAccountId", timestamp = System.currentTimeMillis(), referenceId = inTxId, isReviewed = true,
                    extractionMethod = "MANUAL", confidence = 1.0
                )
                val inTx = com.sciuro.core.ledger.model.Transaction(
                    id = inTxId, accountId = destinationAccountId, categoryId = "cat_transfer", amount = amount, direction = "INFLOW",
                    merchant = "Transfer from $accountId", timestamp = System.currentTimeMillis(), referenceId = outTxId, isReviewed = true,
                    extractionMethod = "MANUAL", confidence = 1.0
                )
                
                transactionRepository.bookTransaction(outTx, source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL, confidence = 1.0f)
                transactionRepository.bookTransaction(inTx, source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL, confidence = 1.0f)
                
                transferRepository.linkTransactions(
                    com.sciuro.core.transfer.model.TransferLink(
                        id = java.util.UUID.randomUUID().toString(),
                        outflowTransactionId = outTxId,
                        inflowTransactionId = inTxId,
                        amount = amount,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                val transaction = com.sciuro.core.ledger.model.Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    direction = direction,
                    merchant = merchant,
                    timestamp = System.currentTimeMillis(),
                    referenceId = null,
                    isReviewed = true,
                    extractionMethod = "MANUAL",
                    confidence = 1.0
                )
                transactionRepository.bookTransaction(
                    transaction = transaction,
                    source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL,
                    confidence = 1.0f
                )
            }
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

    fun undoAutoConfirm(transactionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.undoAutoConfirm(transactionId)
        }
    }

    fun editTransaction(transactionId: String, amount: Double, direction: String, merchant: String, categoryId: String?, accountId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.editTransaction(
                transactionId = transactionId,
                newAmount = amount,
                newDirection = direction,
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

    suspend fun loadTransactionDetail(tx: com.sciuro.core.ledger.db.Transaction_record): TransactionDetailData {
        val auditLogs = auditRepository.getLogsForEntity(tx.id, EntityType.TRANSACTION)
        val transferLink = transferRepository.getTransferForTransaction(tx.id)
        val rawEvent = tx.raw_event_id?.let { rawEventRepository.getRawEventById(it) }
        return TransactionDetailData(
            auditLogs = auditLogs,
            transferLink = transferLink,
            rawEvent = rawEvent
        )
    }

    private fun computeBalanceHistory(transactions: List<com.sciuro.core.ledger.db.Transaction_record>): List<Float> {
        if (transactions.isEmpty()) return emptyList()

        val dayMs = 24L * 60L * 60L * 1000L
        val dailyChanges = transactions.groupBy { it.timestamp / dayMs }
            .mapValues { (_, txs) ->
                txs.sumOf { if (it.direction == "INFLOW") it.amount else -it.amount }
            }
            .entries
            .sortedBy { it.key }

        var balance = 0.0
        val history = mutableListOf<Float>()
        for ((_, change) in dailyChanges) {
            balance += change
            history.add(balance.toFloat())
        }

        return history
    }
}
