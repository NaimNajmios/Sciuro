package com.sciuro.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.model.TransactionIntent
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.budget.engine.RunwayPredictor
import com.sciuro.core.budget.engine.RunwayPrediction
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.investment.repository.InvestmentRepository
import com.sciuro.core.obligations.engine.IncomeRecurrencePatternDetector
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.core.ledger.config.SettingsProvider

import kotlinx.coroutines.flow.first

data class WeeklyDigestData(
    val totalSpent: Double = 0.0,
    val topCategoryName: String? = null,
    val transactionCount: Int = 0,
    val unreviewedCount: Int = 0
)

data class NetPositionBreakdown(
    val cash: Double = 0.0,
    val debts: Double = 0.0,
    val investments: Double = 0.0
)

data class DashboardState(
    val isLoading: Boolean = true,
    val netPosition: Double = 0.0,
    val netPositionBreakdown: NetPositionBreakdown = NetPositionBreakdown(),
    val unreviewedTransactionsCount: Int = 0,
    val autoBookedTransactionsCount: Int = 0,
    val activeBudgetsCount: Int = 0,
    val allTransactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList(),
    val accounts: List<com.sciuro.core.ledger.db.Account> = emptyList(),
    val expenseCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val incomeCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val balanceHistory: List<Float> = emptyList(),
    val runway: Double = 0.0,
    val hasIncomePattern: Boolean = false,
    val expectedIncomeAmount: Double = 0.0,
    val expectedIncomeDate: Long? = null,
    val lastMilestoneReached: Double = 0.0,
    val weeklyDigest: WeeklyDigestData? = null,
    val runwayPrediction: RunwayPrediction? = null
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
    private val auditRepository: AuditRepository,
    private val rawEventRepository: RawEventRepository,
    private val debtRepository: DebtRepository,
    private val investmentRepository: InvestmentRepository,
    private val obligationRepository: ObligationRepository,
    private val incomeDetector: IncomeRecurrencePatternDetector,
    private val settingsProvider: SettingsProvider,
    private val runwayPredictor: RunwayPredictor
) : ViewModel() {
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.seedCategories()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    private val _typeFilter = MutableStateFlow<String?>("All")
    val typeFilter: StateFlow<String?> = _typeFilter.asStateFlow()

    fun setTypeFilter(filter: String?) {
        _typeFilter.value = filter
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                categoryRepository.seedCategories()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _transactionLimit = MutableStateFlow(50L)
    val transactionLimit: StateFlow<Long> = _transactionLimit.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val paginatedTransactions: StateFlow<List<com.sciuro.core.ledger.db.Transaction_record>> = combine(
        _transactionLimit, _startDate, _endDate, _typeFilter
    ) { limit, start, end, type ->
        val direction = when (type) {
            "Income" -> "INFLOW"
            "Expense" -> "OUTFLOW"
            else -> null
        }
        transactionRepository.observeTransactionsFilteredPaginated(start, end, direction, limit, 0)
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun loadMoreTransactions() {
        _transactionLimit.value += 50L
    }

    val state: StateFlow<DashboardState> = combine(
        combine(accountRepository.observeAccounts(), transactionRepository.observeUnreviewedTransactions(), budgetRepository.observeBudgets(), ::Triple),
        combine(transactionRepository.observeAllTransactions(), _startDate, _endDate, ::Triple),
        combine(categoryRepository.observeCategoriesByType("OUTFLOW"), categoryRepository.observeCategoriesByType("INFLOW"), ::Pair),
        combine(debtRepository.observeDebts(), investmentRepository.observeInvestments(), obligationRepository.observeActiveObligations(), ::Triple)
    ) { (accounts, unreviewed, budgets), (allTxs, start, end), (expenseCats, incomeCats), (debts, investments, obligations) ->
        
        val filteredTxs = allTxs.filter { tx ->
            val afterStart = start?.let { tx.timestamp >= it } ?: true
            val beforeEnd = end?.let { tx.timestamp <= it } ?: true
            afterStart && beforeEnd
        }

        val balanceHistory = computeBalanceHistory(allTxs)
        
        val totalAccounts = accounts.sumOf { it.balance }
        val totalInvestments = investments.filterIsInstance<com.sciuro.core.investment.model.Investment>().sumOf { (it.unitsHeld * it.averageBuyPrice).toDouble() }
        val totalDebts = debts.filterIsInstance<com.sciuro.core.debt.model.Debt>().sumOf {
            if (it.direction == DebtDirection.OWED_TO_ME) it.remainingBalance.toDouble()
            else -it.remainingBalance.toDouble()
        }
        val netPosition = totalAccounts + totalInvestments + totalDebts
        val breakdown = NetPositionBreakdown(
            cash = totalAccounts,
            debts = totalDebts,
            investments = totalInvestments
        )

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

        val oneWeekMs = 7L * 24L * 60L * 60L * 1000L
        val weekAgo = currentTimeMillis() - oneWeekMs
        val weekTxs = allTxs.filter { it.timestamp >= weekAgo && it.direction == "OUTFLOW" }
        val weekTotal = weekTxs.sumOf { it.amount }
        val weekCount = weekTxs.size
        val allCatMap = (expenseCats + incomeCats).associateBy { it.id }
        val topCategory = weekTxs.groupBy { it.category_id }.maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        val topCatName = topCategory?.key?.let { allCatMap[it]?.name }
        val weeklyDigest = if (weekCount > 0) WeeklyDigestData(weekTotal, topCatName, weekCount, unreviewed.size) else null

        val thirtyDayOutflows = allTxs.filter {
            it.direction == "OUTFLOW" && it.timestamp >= currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
        }.map { it.amount }
        val upcomingObligations = obligations.sortedBy { it.nextDueDate }.take(30).map { it.amount }
        val prediction = runwayPredictor.predict(
            accountBalance = totalAccounts,
            recentOutflows = thirtyDayOutflows,
            upcomingObligationAmounts = upcomingObligations,
            upstreamIncome = if (incomePattern != null) incomePattern.amount else null
        )

        DashboardState(
            isLoading = false,
            netPosition = netPosition,
            netPositionBreakdown = breakdown,
            unreviewedTransactionsCount = unreviewed.size,
            activeBudgetsCount = budgets.size,
            allTransactions = filteredTxs,
            accounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            balanceHistory = balanceHistory,
            runway = runway,
            hasIncomePattern = incomePattern != null,
            expectedIncomeAmount = incomePattern?.amount ?: 0.0,
            expectedIncomeDate = incomePattern?.nextExpectedDate,
            lastMilestoneReached = settingsProvider.getLastMilestoneReached(),
            weeklyDigest = weeklyDigest,
            runwayPrediction = prediction
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    val autoBookedTransactionsCount: StateFlow<Int> = transactionRepository
        .observeRecentlyAutoConfirmed(currentTimeMillis() - 24L * 60L * 60L * 1000L)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoBookedTransactions: StateFlow<List<com.sciuro.core.ledger.db.Transaction_record>> = transactionRepository
        .observeRecentlyAutoConfirmed(currentTimeMillis() - 24L * 60L * 60L * 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoConfirmedForCategoryReview: StateFlow<List<com.sciuro.core.ledger.db.Transaction_record>> = transactionRepository
        .observeAllAutoConfirmed(currentTimeMillis() - 24L * 60L * 60L * 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviewSuggestions: StateFlow<List<ReviewSuggestion>> = transactionRepository
        .observeUnreviewedTransactions()
        .map { txs -> computeSuggestions(txs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeTransactionCategory(transactionId: String, newCategoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.updateTransactionCategory(transactionId, newCategoryId)
        }
    }

    private suspend fun computeSuggestions(txs: List<com.sciuro.core.ledger.db.Transaction_record>): List<ReviewSuggestion> {
        if (txs.isEmpty()) return emptyList()

        val activeDebtsIowe = debtRepository.observeDebts().first()
            .filter { it.direction == DebtDirection.I_OWE && it.status == DebtStatus.ACTIVE }
        val activeDebtsOwedToMe = debtRepository.observeDebts().first()
            .filter { it.direction == DebtDirection.OWED_TO_ME && it.status == DebtStatus.ACTIVE }
        val activeObligations = obligationRepository.observeActiveObligations().first()

        return txs.map { tx ->
            val suggestion = if (tx.direction == "OUTFLOW") {
                val obligationMatch = activeObligations.firstOrNull {
                    tx.merchant != null && (it.name.contains(tx.merchant!!, ignoreCase = true) || tx.merchant!!.contains(it.name, ignoreCase = true)) &&
                    kotlin.math.abs(it.amount - tx.amount) < tx.amount * 0.05
                }
                if (obligationMatch != null) {
                    ReviewSuggestion(
                        transactionId = tx.id,
                        merchant = tx.merchant,
                        amount = tx.amount,
                        direction = tx.direction,
                        suggestedCategoryId = obligationMatch.categoryId,
                        suggestedAccountId = obligationMatch.accountId,
                        intent = TransactionIntent.SubscriptionPayment(obligationMatch.id, obligationMatch.name, obligationMatch.nextDueDate),
                        reason = "obligation_match"
                    )
                } else {
                    val debtMatch = activeDebtsIowe.firstOrNull {
                        tx.merchant != null && (it.name.contains(tx.merchant!!, ignoreCase = true) || tx.merchant!!.contains(it.name, ignoreCase = true))
                    }
                    if (debtMatch != null) {
                        ReviewSuggestion(
                            transactionId = tx.id,
                            merchant = tx.merchant,
                            amount = tx.amount,
                            direction = tx.direction,
                            suggestedCategoryId = "cat_debt_payment",
                            suggestedAccountId = debtMatch.associatedAccountId,
                            intent = TransactionIntent.DebtPayment(debtMatch.id, debtMatch.name, debtMatch.remainingBalance, debtMatch.counterpartyName),
                            reason = "debt_match"
                        )
                    } else null
                }
            } else {
                val collectibleMatch = activeDebtsOwedToMe.firstOrNull {
                    it.counterpartyName != null && tx.merchant != null && tx.merchant!!.contains(it.counterpartyName!!, ignoreCase = true)
                }
                if (collectibleMatch != null) {
                    ReviewSuggestion(
                        transactionId = tx.id,
                        merchant = tx.merchant,
                        amount = tx.amount,
                        direction = tx.direction,
                        suggestedCategoryId = null,
                        suggestedAccountId = null,
                        intent = TransactionIntent.DebtCollection(collectibleMatch.id, collectibleMatch.counterpartyName ?: collectibleMatch.name),
                        reason = "debt_collection"
                    )
                } else null
            }

            suggestion ?: ReviewSuggestion(
                transactionId = tx.id,
                merchant = tx.merchant,
                amount = tx.amount,
                direction = tx.direction,
                suggestedCategoryId = null,
                suggestedAccountId = null,
                intent = TransactionIntent.Unknown,
                reason = "unknown"
            )
        }
    }

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

    fun confirmReviewSuggestion(transactionId: String, categoryId: String?, accountId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.reviewTransaction(transactionId, categoryId, accountId)
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
