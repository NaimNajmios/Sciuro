package com.sciuro.feature.kanban.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.feature.kanban.model.BillTask
import com.sciuro.feature.kanban.model.DebtTask
import com.sciuro.feature.kanban.model.KanbanTask
import com.sciuro.feature.kanban.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val obligationRepository: ObligationRepository,
    private val debtRepository: DebtRepository,
    eventBus: DomainEventBus
) : ViewModel() {

    private val _animationTriggers = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val animationTriggers: SharedFlow<String> = _animationTriggers

    init {
        viewModelScope.launch(Dispatchers.Default) {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.ObligationCycleSettled -> _animationTriggers.emit(event.obligationId)
                    is DomainEvent.DebtBalanceUpdated -> _animationTriggers.emit(event.debtId)
                    is DomainEvent.DebtFullyPaidOff -> _animationTriggers.emit(event.debtId)
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

    val debtTasks: StateFlow<List<DebtTask>> = debtRepository.observeDebts()
        .map { debts -> debts.filter { it.status == DebtStatus.ACTIVE }.map { DebtTask.fromDebt(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus, newAccountId: String? = null, newDirection: String? = null) {
        if (newStatus == TaskStatus.DONE) {
            viewModelScope.launch {
                transactionRepository.reviewTransaction(taskId, null, newAccountId, newDirection)
            }
        } else if (newStatus == TaskStatus.REJECTED) {
            viewModelScope.launch {
                transactionRepository.rejectTransaction(taskId)
            }
        }
    }

    fun markBillAsPaid(obligation: Obligation) {
        viewModelScope.launch(Dispatchers.IO) {
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
        }
    }

    fun recordDebtPayment(debtId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.applyPayment(debtId, amount)
        }
    }
}
