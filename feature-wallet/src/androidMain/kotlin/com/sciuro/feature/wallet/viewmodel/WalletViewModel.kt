package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.feature.wallet.model.WalletAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletViewModel : ViewModel() {
    private val _accounts = MutableStateFlow<List<WalletAccount>>(emptyList())
    val accounts: StateFlow<List<WalletAccount>> = _accounts.asStateFlow()
    
    init {
        // Mock data for Phase C2 scaffolding
        _accounts.value = listOf(
            WalletAccount("1", "Maybank", 12450.00, false),
            WalletAccount("2", "CIMB", 5300.50, false),
            WalletAccount("3", "Touch 'n Go eWallet", 150.00, true),
            WalletAccount("4", "GrabPay", 85.00, true)
        )
    }
}
