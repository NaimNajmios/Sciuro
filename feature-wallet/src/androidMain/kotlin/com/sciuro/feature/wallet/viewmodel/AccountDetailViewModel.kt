package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.Cash_adjustment
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.db.Transaction_record
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.CashAdjustmentRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.engine.VelocityCalculator
import com.sciuro.core.ledger.engine.SpendingVelocity
import com.sciuro.core.transfer.model.TransferLink
import com.sciuro.core.transfer.repository.TransferRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TimelineItem {
    data class TransactionItem(val tx: Transaction_record) : TimelineItem
    data class AdjustmentItem(val adjustment: Cash_adjustment) : TimelineItem
}

sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState
    data class Loaded(
        val account: com.sciuro.core.ledger.db.Account,
        val transactions: List<Transaction_record>,
        val adjustments: List<Cash_adjustment>,
        val selectedFilter: String,
        val timeline: List<TimelineItem>,
        val spendingVelocity: SpendingVelocity?
    ) : AccountDetailUiState
}

sealed interface AccountDetailDialog {
    data object None : AccountDetailDialog
    data object AdjustBalance : AccountDetailDialog
    data class TransactionDetail(val tx: Transaction_record) : AccountDetailDialog
    data class EditTransaction(val tx: Transaction_record) : AccountDetailDialog
    data object EditAccountDetails : AccountDetailDialog
    data object ArchiveConfirm : AccountDetailDialog
    data object DeleteConfirm : AccountDetailDialog
    data class DeleteTransactionConfirm(val tx: Transaction_record) : AccountDetailDialog
    data object ColorPicker : AccountDetailDialog
    data object QrFullScreen : AccountDetailDialog
    data class AdjustConfirm(val amount: Double, val reason: String, val remark: String?) : AccountDetailDialog
    data class ColorConfirm(val color: String?) : AccountDetailDialog
    data class EditAccountConfirm(val accountNumber: String?, val holderName: String?, val bankCode: String?) : AccountDetailDialog
}

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
    private val rawEventRepository: RawEventRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"]) { "accountId must be provided" }

    private val _selectedFilter = MutableStateFlow("All")

    val expenseCategories: StateFlow<List<com.sciuro.core.ledger.model.Category>> = categoryRepository.observeCategoriesByType("OUTFLOW")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<com.sciuro.core.ledger.model.Category>> = categoryRepository.observeCategoriesByType("INFLOW")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountRepository.observeAccountById(accountId),
        transactionRepository.observeTransactionsForAccount(accountId),
        cashAdjustmentRepository.observeAdjustmentsForAccount(accountId),
        _selectedFilter
    ) { account, transactions, adjustments, filter ->
        if (account == null) return@combine AccountDetailUiState.Loading
        val timeline = buildTimeline(transactions, adjustments, filter)
        val velocity = if (transactions.isNotEmpty() && account.balance > 0) {
            VelocityCalculator().calculate(account.balance, transactions)
        } else null
        AccountDetailUiState.Loaded(
            account = account,
            transactions = transactions,
            adjustments = adjustments,
            selectedFilter = filter,
            timeline = timeline,
            spendingVelocity = velocity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailUiState.Loading
    )

    private fun buildTimeline(
        transactions: List<Transaction_record>,
        adjustments: List<Cash_adjustment>,
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
            else -> {
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

    fun recordCorrection(amount: Double, reason: String, remark: String? = null) {
        viewModelScope.launch {
            cashAdjustmentRepository.createAdjustment(
                accountId = accountId,
                amount = amount,
                reason = reason,
                remark = remark
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
            val s = uiState.value
            if (s !is AccountDetailUiState.Loaded) return@launch
            val a = s.account
            val domainAccount = com.sciuro.core.ledger.model.Account(
                id = a.id,
                name = a.name,
                type = a.type,
                currency = a.currency,
                balance = a.balance,
                associatedPackage = a.associated_package,
                isSystem = a.is_system == 1L,
                status = a.status,
                color = color,
                accountNumber = a.account_number,
                accountHolderName = a.account_holder_name,
                bankInstitutionCode = a.bank_institution_code,
                qrImagePath = a.qr_image_path,
                qrPayloadText = a.qr_payload_text
            )
            accountRepository.updateAccount(domainAccount)
        }
    }

    fun updateAccountDetails(accountNumber: String?, accountHolderName: String?, bankInstitutionCode: String?) {
        viewModelScope.launch {
            val s = uiState.value
            if (s !is AccountDetailUiState.Loaded) return@launch
            val a = s.account
            val domainAccount = com.sciuro.core.ledger.model.Account(
                id = a.id,
                name = a.name,
                type = a.type,
                currency = a.currency,
                balance = a.balance,
                associatedPackage = a.associated_package,
                isSystem = a.is_system == 1L,
                status = a.status,
                color = a.color,
                accountNumber = accountNumber,
                accountHolderName = accountHolderName,
                bankInstitutionCode = bankInstitutionCode,
                qrImagePath = a.qr_image_path,
                qrPayloadText = a.qr_payload_text
            )
            accountRepository.updateAccount(domainAccount)
        }
    }

    fun updateQrImagePath(path: String?) {
        viewModelScope.launch {
            val s = uiState.value
            if (s !is AccountDetailUiState.Loaded) return@launch
            val a = s.account
            val domainAccount = com.sciuro.core.ledger.model.Account(
                id = a.id,
                name = a.name,
                type = a.type,
                currency = a.currency,
                balance = a.balance,
                associatedPackage = a.associated_package,
                isSystem = a.is_system == 1L,
                status = a.status,
                color = a.color,
                accountNumber = a.account_number,
                accountHolderName = a.account_holder_name,
                bankInstitutionCode = a.bank_institution_code,
                qrImagePath = path,
                qrPayloadText = a.qr_payload_text
            )
            accountRepository.updateAccount(domainAccount)
        }
    }

    suspend fun loadTransactionDetail(tx: Transaction_record): TransactionDetailData {
        val auditLogs = auditRepository.getLogsForEntity(tx.id, EntityType.TRANSACTION)
        val transferLink = transferRepository.getTransferForTransaction(tx.id)
        val rawEvent = tx.raw_event_id?.let { rawEventRepository.getRawEventById(it) }
        return TransactionDetailData(
            auditLogs = auditLogs,
            transferLink = transferLink,
            rawEvent = rawEvent
        )
    }

    fun editTransaction(transactionId: String, amount: Double, direction: String, merchant: String, categoryId: String?, accountId: String?) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
        }
    }
}
