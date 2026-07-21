package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountDetailState(
    val account: com.sciuro.core.ledger.db.Account? = null,
    val transactions: List<com.sciuro.core.ledger.db.Transaction_record> = emptyList()
)

class AccountDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"]) { "accountId must be provided" }

    val state: StateFlow<AccountDetailState> = combine(
        accountRepository.observeAccountById(accountId),
        transactionRepository.observeTransactionsForAccount(accountId)
    ) { account, transactions ->
        AccountDetailState(
            account = account,
            transactions = transactions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailState()
    )

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
}
