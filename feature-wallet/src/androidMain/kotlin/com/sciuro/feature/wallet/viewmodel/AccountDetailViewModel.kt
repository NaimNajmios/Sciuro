package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.CashAdjustmentRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.transfer.model.TransferLink
import com.sciuro.core.transfer.repository.TransferRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TimelineItem {
    data class TransactionItem(val tx: com.sciuro.core.ledger.db.Transaction_record) : TimelineItem()
    data class AdjustmentItem(val adjustment: com.sciuro.core.ledger.db.Cash_adjustment) : TimelineItem()
}

data class AccountDetailState(
    val account: com.sciuro.core.ledger.db.Account? = null,
    val transactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList(),
    val adjustments: List<com.sciuro.core.ledger.db.Cash_adjustment> = emptyList(),
    val selectedFilter: String = "All",
    val timeline: List<TimelineItem> = emptyList()
)

data class TransactionDetailData(
    val auditLogs: List<AuditLog> = emptyList(),
    val transferLink: TransferLink? = null,
    val rawEvent: Raw_event_staging? = null
)

class AccountDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val cashAdjustmentRepository: CashAdjustmentRepository,
    private val auditRepository: AuditRepository,
    private val transferRepository: TransferRepository,
    private val rawEventRepository: RawEventRepository
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"]) { "accountId must be provided" }

    private val _selectedFilter = MutableStateFlow("All")

    val state: StateFlow<AccountDetailState> = combine(
        accountRepository.observeAccountById(accountId),
        transactionRepository.observeTransactionsForAccount(accountId),
        cashAdjustmentRepository.observeAdjustmentsForAccount(accountId),
        _selectedFilter
    ) { account, transactions, adjustments, filter ->
        val timeline = buildTimeline(transactions, adjustments, filter)
        AccountDetailState(
            account = account,
            transactions = transactions,
            adjustments = adjustments,
            selectedFilter = filter,
            timeline = timeline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailState()
    )

    private fun buildTimeline(
        transactions: List<com.sciuro.core.ledger.db.Transaction_record>,
        adjustments: List<com.sciuro.core.ledger.db.Cash_adjustment>,
        filter: String
    ): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        when (filter) {
            "Transactions" -> {
                items.addAll(transactions.map { TimelineItem.TransactionItem(it) })
            }
            "Adjustments" -> {
                items.addAll(adjustments.map { TimelineItem.AdjustmentItem(it) })
            }
            "Income" -> {
                items.addAll(transactions.filter { it.direction == "INFLOW" }.map { TimelineItem.TransactionItem(it) })
            }
            "Expense" -> {
                items.addAll(transactions.filter { it.direction == "OUTFLOW" }.map { TimelineItem.TransactionItem(it) })
            }
            else -> { // "All"
                items.addAll(transactions.map { TimelineItem.TransactionItem(it) })
                items.addAll(adjustments.map { TimelineItem.AdjustmentItem(it) })
                items.sortByDescending {
                    when (it) {
                        is TimelineItem.TransactionItem -> it.tx.timestamp
                        is TimelineItem.AdjustmentItem -> it.adjustment.timestamp
                    }
                }
            }
        }
        return items
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun recordCorrection(amount: Double, reason: String) {
        viewModelScope.launch {
            cashAdjustmentRepository.createAdjustment(
                accountId = accountId,
                amount = amount,
                reason = reason
            )
        }
    }

    fun deleteCorrection(adjustmentId: String) {
        viewModelScope.launch {
            cashAdjustmentRepository.deleteAdjustment(adjustmentId)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun archiveAccount() {
        viewModelScope.launch {
            accountRepository.archiveAccount(accountId)
        }
    }

    fun updateAccountColor(color: String?) {
        viewModelScope.launch {
            val account = state.value.account ?: return@launch
            val domainAccount = com.sciuro.core.ledger.model.Account(
                id = account.id,
                name = account.name,
                type = account.type,
                currency = account.currency,
                balance = account.balance,
                associatedPackage = account.associated_package,
                isSystem = account.is_system == 1L,
                status = account.status,
                color = color
            )
            accountRepository.updateAccount(domainAccount)
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
}
