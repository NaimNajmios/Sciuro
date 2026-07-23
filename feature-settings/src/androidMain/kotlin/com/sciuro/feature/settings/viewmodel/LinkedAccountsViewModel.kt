package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LinkedAccountsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LinkedAccountsUiState>(LinkedAccountsUiState.Loading)
    val state: StateFlow<LinkedAccountsUiState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first()
            if (accounts.isEmpty()) {
                _state.value = LinkedAccountsUiState.Empty
            } else {
                _state.value = LinkedAccountsUiState.Ready(
                    accounts = accounts,
                    selectedIds = emptySet(),
                    canLink = false
                )
            }
        }
    }

    fun toggleSelection(accountId: String) {
        val current = _state.value
        if (current !is LinkedAccountsUiState.Ready) return
        val newSelected = if (accountId in current.selectedIds) {
            current.selectedIds - accountId
        } else {
            current.selectedIds + accountId
        }
        _state.value = current.copy(
            selectedIds = newSelected,
            canLink = newSelected.size == 2
        )
    }

    fun linkSelectedPair() {
        val current = _state.value
        if (current !is LinkedAccountsUiState.Ready) return
        val ids = current.selectedIds.toList()
        if (ids.size != 2) return
        viewModelScope.launch {
            accountRepository.linkAccountPair(ids[0], ids[1])
            _state.value = LinkedAccountsUiState.Linked("Accounts linked successfully.")
        }
    }
}
