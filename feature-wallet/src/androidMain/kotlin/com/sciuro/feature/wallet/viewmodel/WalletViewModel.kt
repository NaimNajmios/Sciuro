package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.feature.wallet.model.WalletAccount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WalletViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    val accounts: StateFlow<List<WalletAccount>> = accountRepository.observeAccounts()
        .map { accounts ->
            accounts.map {
                WalletAccount(
                    id = it.id,
                    name = it.name,
                    balance = it.balance,
                    isEWallet = it.type.lowercase().contains("ewallet") || it.type.lowercase().contains("e-wallet")
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
