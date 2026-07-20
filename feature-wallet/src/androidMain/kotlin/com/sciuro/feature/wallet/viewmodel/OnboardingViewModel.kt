package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.model.Account
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.sciuro.core.audit.util.currentTimeMillis

data class OnboardingState(
    val isLoading: Boolean = true,
    val isOnboardingComplete: Boolean = false
)

class OnboardingViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val state: StateFlow<OnboardingState> = accountRepository.observeAccounts()
        .map { accounts ->
            OnboardingState(
                isLoading = false,
                isOnboardingComplete = accounts.isNotEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OnboardingState()
        )

    fun setupPersonalWallet(initialBalance: Double) {
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first()
            if (accounts.isEmpty()) {
                accountRepository.createAccount(
                    Account(
                        id = "default-${currentTimeMillis()}",
                        name = "Personal Wallet",
                        type = "Cash",
                        currency = "MYR",
                        balance = initialBalance,
                        associatedPackage = null,
                        isSystem = true
                    )
                )
            }
        }
    }
}
