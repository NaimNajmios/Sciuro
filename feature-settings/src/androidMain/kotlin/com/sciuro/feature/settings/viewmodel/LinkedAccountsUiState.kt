package com.sciuro.feature.settings.viewmodel

import com.sciuro.core.ledger.db.Account

sealed interface LinkedAccountsUiState {
    data object Loading : LinkedAccountsUiState
    data object Empty : LinkedAccountsUiState
    data class Ready(
        val accounts: List<Account>,
        val selectedIds: Set<String>,
        val canLink: Boolean
    ) : LinkedAccountsUiState
    data class Linked(val message: String) : LinkedAccountsUiState
}
