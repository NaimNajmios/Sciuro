package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LinkedAccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<com.sciuro.core.ledger.db.Account> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val linkedPairs: List<Pair<String, String>> = emptyList(),
    val canLink: Boolean = false,
    val message: String? = null
) {
    companion object {
        val Loading = LinkedAccountsUiState(isLoading = true)
        val Empty = LinkedAccountsUiState(isLoading = false)
    }
}

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
            val linkedPairs = accountRepository.observeLinkedPairs().first()
            if (accounts.isEmpty()) {
                _state.value = LinkedAccountsUiState.Empty
            } else {
                _state.value = LinkedAccountsUiState(
                    isLoading = false,
                    accounts = accounts,
                    linkedPairs = linkedPairs
                )
            }
        }
    }

    fun toggleSelection(accountId: String) {
        val current = _state.value
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
        val ids = current.selectedIds.toList()
        if (ids.size != 2) return
        viewModelScope.launch {
            accountRepository.linkAccountPair(ids[0], ids[1])
            _state.value = current.copy(
                message = "Accounts linked successfully.",
                selectedIds = emptySet(),
                canLink = false
            )
            loadAccounts()
        }
    }

    fun unlinkPair(accountIdA: String, accountIdB: String) {
        viewModelScope.launch {
            accountRepository.unlinkAccountPair(accountIdA, accountIdB)
            _state.value = _state.value.copy(message = "Accounts unlinked.")
            loadAccounts()
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
