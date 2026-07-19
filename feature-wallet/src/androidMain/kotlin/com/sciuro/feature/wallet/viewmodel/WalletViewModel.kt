package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.feature.wallet.model.WalletAccount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
                    isEWallet = it.type.lowercase().contains("ewallet") || it.type.lowercase().contains("e-wallet"),
                    associatedPackage = it.associated_package
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    fun addAccount(name: String, type: String, associatedPackage: String, initialBalance: Double) {
        viewModelScope.launch {
            accountRepository.createAccount(
                com.sciuro.core.ledger.model.Account(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    balance = initialBalance,
                    associatedPackage = associatedPackage.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun updateAccount(id: String, name: String, type: String, associatedPackage: String, balance: Double) {
        viewModelScope.launch {
            accountRepository.updateAccount(
                com.sciuro.core.ledger.model.Account(
                    id = id,
                    name = name,
                    type = type,
                    balance = balance,
                    associatedPackage = associatedPackage.takeIf { it.isNotBlank() }
                )
            )
        }
    }
    
    fun deleteAccount(id: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(id)
        }
    }
}
