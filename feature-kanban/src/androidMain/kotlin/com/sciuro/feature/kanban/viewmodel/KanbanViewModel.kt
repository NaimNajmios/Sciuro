package com.sciuro.feature.kanban.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.model.DebtType
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.model.ObligationFrequency
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.transfer.repository.TransferRepository
import com.sciuro.feature.kanban.model.BillTask
import com.sciuro.feature.kanban.model.DebtTask
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent
import java.util.UUID

class KanbanViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val obligationRepository: ObligationRepository,
    private val debtRepository: DebtRepository,
    private val transferRepository: TransferRepository,
    eventBus: DomainEventBus
) : ViewModel() {

    enum class DebtsFilter(val label: String) {
        ACTIVE("Active"),
        INCLUDING_PAID_OFF("+Paid Off"),
        ALL("All")
    }

    private val _animationTriggers = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val animationTriggers: SharedFlow<String> = _animationTriggers

    private val _errorEvents = MutableStateFlow<String?>(null)
    val errorEvents: StateFlow<String?> = _errorEvents.asStateFlow()

    fun clearError() {
        _errorEvents.value = null
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _processingTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val processingTaskIds: StateFlow<Set<String>> = _processingTaskIds.asStateFlow()

    private val _driftedAmounts = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val driftedAmounts: StateFlow<Map<String, Pair<Double, Double>>> = _driftedAmounts.asStateFlow()

    private val _transferCandidates = MutableStateFlow<Map<String, String>>(emptyMap())
    val transferCandidateIds: StateFlow<Set<String>> = _transferCandidates
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(600)
            _isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.ObligationCycleSettled -> _animationTriggers.emit(event.obligationId)
                    is DomainEvent.DebtBalanceUpdated -> _animationTriggers.emit(event.debtId)
                    is DomainEvent.DebtFullyPaidOff -> _animationTriggers.emit(event.debtId)
                    is DomainEvent.ObligationAmountDrifted -> {
                        _driftedAmounts.value = _driftedAmounts.value + (event.obligationId to Pair(event.oldAmount, event.newAmount))
                        _animationTriggers.emit(event.obligationId)
                    }
                    is DomainEvent.TransferUnmatchedFlagged -> {
                        if (event.candidateTransactionId.isNotBlank()) {
                            _transferCandidates.value = _transferCandidates.value + (event.transactionId to event.candidateTransactionId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    val tasks: StateFlow<List<KanbanTask>> = transactionRepository.observeUnreviewedTransactions()
        .map { unreviewedTxs ->
            unreviewedTxs.map { tx ->
                KanbanTask(
                    id = tx.id,
                    title = "Review Transaction: ${tx.merchant}",
                    description = "Amount: RM ${tx.amount} (${tx.direction})",
                    status = TaskStatus.TODO,
                    accountId = tx.account_id,
                    categoryId = tx.category_id,
                    direction = tx.direction
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { it.map { acc -> Account(acc.id, acc.name, acc.type, acc.currency, acc.balance, acc.associated_package) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bills: StateFlow<List<BillTask>> = obligationRepository.observeActiveObligations()
        .map { obligations ->
            val now = System.currentTimeMillis()
            obligations.map { BillTask.fromObligation(it, now) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _debtsFilter = MutableStateFlow(DebtsFilter.ACTIVE)
    val debtsFilter: StateFlow<DebtsFilter> = _debtsFilter.asStateFlow()

    fun setDebtsFilter(filter: DebtsFilter) {
        _debtsFilter.value = filter
    }

    val debtTasks: StateFlow<List<DebtTask>> = debtRepository.observeDebts()
        .combine(_debtsFilter) { debts, filter ->
            val allowedStatuses = when (filter) {
                DebtsFilter.ACTIVE -> setOf(DebtStatus.ACTIVE)
                DebtsFilter.INCLUDING_PAID_OFF -> setOf(DebtStatus.ACTIVE, DebtStatus.PAID_OFF)
                DebtsFilter.ALL -> DebtStatus.entries.toSet()
            }
            debts.filter { it.status in allowedStatuses }.map { DebtTask.fromDebt(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenseCategories: StateFlow<List<Category>> = categoryRepository
        .observeCategoriesByType("OUTFLOW")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus, newAccountId: String? = null, newDirection: String? = null) {
        if (taskId in _processingTaskIds.value) return
        _processingTaskIds.value = _processingTaskIds.value + taskId

        viewModelScope.launch {
            try {
                if (newStatus == TaskStatus.DONE) {
                    transactionRepository.reviewTransaction(taskId, null, newAccountId, newDirection)
                } else if (newStatus == TaskStatus.REJECTED) {
                    transactionRepository.rejectTransaction(taskId)
                }
            } catch (e: Exception) {
                _errorEvents.value = "Failed to ${if (newStatus == TaskStatus.DONE) "approve" else "reject"} transaction: ${e.message}"
            } finally {
                _processingTaskIds.value = _processingTaskIds.value - taskId;
            }
        }
    }

    fun linkTransferCandidate(transactionId: String) {
        if (transactionId in _processingTaskIds.value) return
        _processingTaskIds.value = _processingTaskIds.value + transactionId

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val candidateId = _transferCandidates.value[transactionId]
                if (candidateId == null) {
                    _errorEvents.value = "Transfer candidate is no longer available"
                    return@launch
                }
                val link = transferRepository.linkCandidatePair(transactionId, candidateId)
                if (link == null) {
                    _errorEvents.value = "Could not link transfer — the pair may already be linked"
                    return@launch
                }
                _transferCandidates.value = _transferCandidates.value - transactionId
            } catch (e: Exception) {
                _errorEvents.value = "Failed to link transfer: ${e.message}"
            } finally {
                _processingTaskIds.value = _processingTaskIds.value - transactionId
            }
        }
    }

    fun markBillAsPaid(obligation: Obligation) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    accountId = obligation.accountId,
                    categoryId = obligation.categoryId,
                    amount = obligation.amount,
                    direction = "OUTFLOW",
                    merchant = obligation.name,
                    timestamp = System.currentTimeMillis(),
                    referenceId = null,
                    isReviewed = true,
                    extractionMethod = "MANUAL",
                    confidence = 1.0,
                    rawEventId = null
                )
                transactionRepository.bookTransaction(tx, source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL, confidence = 1.0f)

                val newDueDate = ObligationCycleMatcher.computeNextDueDate(obligation.nextDueDate, obligation.frequency.name)
                obligationRepository.recordPayment(obligation.id, newDueDate)
            } catch (e: Exception) {
                _errorEvents.value = "Failed to mark bill as paid: ${e.message}"
            }
        }
    }

    fun recordDebtPayment(debtId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                debtRepository.applyPayment(debtId, amount)
            } catch (e: Exception) {
                _errorEvents.value = "Failed to record debt payment: ${e.message}"
            }
        }
    }

    fun markDebtAsFinished(debtId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                debtRepository.markAsPaidOff(debtId)
            } catch (e: Exception) {
                _errorEvents.value = "Failed to mark debt as finished: ${e.message}"
            }
        }
    }

    fun updateObligation(
        id: String,
        name: String,
        amount: Double,
        frequency: ObligationFrequency,
        nextDueDate: Long,
        categoryId: String?,
        accountId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentBills = bills.value
                val existing = currentBills.find { it.id == id }
                if (existing != null) {
                    obligationRepository.updateObligation(
                        Obligation(
                            id = id,
                            name = name,
                            amount = amount,
                            frequency = frequency,
                            nextDueDate = nextDueDate,
                            categoryId = categoryId,
                            accountId = accountId,
                            isActive = true
                        )
                    )
                }
            } catch (e: Exception) {
                _errorEvents.value = "Failed to update obligation: ${e.message}"
            }
        }
    }

    fun deleteObligation(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                obligationRepository.deleteObligation(id)
            } catch (e: Exception) {
                _errorEvents.value = "Failed to delete obligation: ${e.message}"
            }
        }
    }

    fun createObligation(
        name: String,
        amount: Double,
        frequency: ObligationFrequency,
        nextDueDate: Long,
        categoryId: String?,
        accountId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                obligationRepository.createObligation(
                    Obligation(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        amount = amount,
                        frequency = frequency,
                        nextDueDate = nextDueDate,
                        categoryId = categoryId,
                        accountId = accountId,
                        isActive = true
                    )
                )
            } catch (e: Exception) {
                _errorEvents.value = "Failed to create obligation: ${e.message}"
            }
        }
    }

    fun createDebt(
        name: String,
        type: DebtType,
        direction: DebtDirection,
        principalAmount: Double,
        counterpartyName: String?,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                debtRepository.createDebt(
                    Debt(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        direction = direction,
                        counterpartyName = counterpartyName,
                        status = DebtStatus.ACTIVE,
                        principalAmount = principalAmount,
                        remainingBalance = principalAmount,
                        notes = notes
                    )
                )
            } catch (e: Exception) {
                _errorEvents.value = "Failed to create debt: ${e.message}"
            }
        }
    }
    
    fun deleteDebt(debtId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                debtRepository.deleteDebt(debtId)
            } catch (e: Exception) {
                _errorEvents.value = "Failed to delete debt: ${e.message}"
            }
        }
    }

    fun updateDebt(
        debtId: String,
        name: String,
        principalAmount: Double,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentDebts = debtTasks.value
                val existing = currentDebts.find { it.id == debtId }
                if (existing != null) {
                    val adjustedRemaining = minOf(existing.debt.remainingBalance, principalAmount)
                    debtRepository.updateDebt(
                        Debt(
                            id = debtId,
                            name = name,
                            type = existing.type,
                            direction = existing.direction,
                            counterpartyName = existing.counterpartyName,
                            status = existing.debt.status,
                            principalAmount = principalAmount,
                            remainingBalance = adjustedRemaining,
                            notes = notes
                        )
                    )
                }
            } catch (e: Exception) {
                _errorEvents.value = "Failed to update debt: ${e.message}"
            }
        }
    }
}
