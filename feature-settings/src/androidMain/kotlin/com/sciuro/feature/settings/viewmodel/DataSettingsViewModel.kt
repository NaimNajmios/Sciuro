package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DataSettingsUiState(
    val budgetWarningThreshold: Float = 0.8f
)

class DataSettingsViewModel(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = DataSettingsUiState(
            budgetWarningThreshold = settingsProvider.getBudgetWarningThreshold()
        )
    }

    fun setBudgetWarningThreshold(threshold: Float) {
        settingsProvider.setBudgetWarningThreshold(threshold)
        _uiState.value = _uiState.value.copy(budgetWarningThreshold = threshold)
    }
}
