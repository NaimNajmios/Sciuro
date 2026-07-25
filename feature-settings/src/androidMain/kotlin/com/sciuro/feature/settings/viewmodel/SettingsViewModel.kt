package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class SettingsUiState(
    val isLlmEnabled: Boolean = false,
    val isLockEnabled: Boolean = false,
    val isObligationAutoConfirmEnabled: Boolean = false,
    val apiKey: String = "",
    val llmModelName: String = "llama-3.1-8b-instant",
    val budgetWarningThreshold: Float = 0.8f,
    val isQuietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val isDeveloperOptionsVisible: Boolean = false,
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle
)

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Success : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

class SettingsViewModel(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            isLlmEnabled = settingsProvider.isLlmEnabled(),
            isLockEnabled = settingsProvider.isLockEnabled(),
            isObligationAutoConfirmEnabled = settingsProvider.isObligationAutoConfirmEnabled(),
            apiKey = settingsProvider.getApiKey() ?: "",
            llmModelName = settingsProvider.getLlmModelName(),
            budgetWarningThreshold = settingsProvider.getBudgetWarningThreshold(),
            isQuietHoursEnabled = settingsProvider.isQuietHoursEnabled(),
            quietHoursStart = settingsProvider.getQuietHoursStart(),
            quietHoursEnd = settingsProvider.getQuietHoursEnd(),
            isDeveloperOptionsVisible = settingsProvider.isDeveloperOptionsVisible()
        )
    }

    fun setLlmEnabled(enabled: Boolean) {
        settingsProvider.setLlmEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLlmEnabled = enabled)
    }

    fun setLockEnabled(enabled: Boolean) {
        settingsProvider.setLockEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLockEnabled = enabled)
    }

    fun setObligationAutoConfirmEnabled(enabled: Boolean) {
        settingsProvider.setObligationAutoConfirmEnabled(enabled)
        _uiState.value = _uiState.value.copy(isObligationAutoConfirmEnabled = enabled)
    }

    fun setApiKey(apiKey: String) {
        settingsProvider.setApiKey(apiKey)
        _uiState.value = _uiState.value.copy(apiKey = apiKey, connectionTestState = ConnectionTestState.Idle)
    }

    fun setLlmModelName(name: String) {
        settingsProvider.setLlmModelName(name)
        _uiState.value = _uiState.value.copy(llmModelName = name)
    }

    fun setBudgetWarningThreshold(threshold: Float) {
        settingsProvider.setBudgetWarningThreshold(threshold)
        _uiState.value = _uiState.value.copy(budgetWarningThreshold = threshold)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        settingsProvider.setQuietHoursEnabled(enabled)
        _uiState.value = _uiState.value.copy(isQuietHoursEnabled = enabled)
    }

    fun setQuietHoursStart(hour: Int) {
        settingsProvider.setQuietHoursStart(hour)
        _uiState.value = _uiState.value.copy(quietHoursStart = hour)
    }

    fun setQuietHoursEnd(hour: Int) {
        settingsProvider.setQuietHoursEnd(hour)
        _uiState.value = _uiState.value.copy(quietHoursEnd = hour)
    }

    fun testConnection() {
        val apiKey = _uiState.value.apiKey
        if (apiKey.isBlank()) return

        _uiState.value = _uiState.value.copy(connectionTestState = ConnectionTestState.Testing)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.groq.com/openai/v1/models")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    when (connection.responseCode) {
                        200 -> ConnectionTestState.Success
                        401 -> ConnectionTestState.Error("Invalid API Key")
                        else -> ConnectionTestState.Error("HTTP ${connection.responseCode}")
                    }
                } catch (e: Exception) {
                    ConnectionTestState.Error(e.message ?: "Connection failed")
                }
            }
            _uiState.value = _uiState.value.copy(connectionTestState = result)
        }
    }

    fun clearConnectionTestState() {
        _uiState.value = _uiState.value.copy(connectionTestState = ConnectionTestState.Idle)
    }

    fun setDeveloperOptionsVisible(visible: Boolean) {
        settingsProvider.setDeveloperOptionsVisible(visible)
        _uiState.value = _uiState.value.copy(isDeveloperOptionsVisible = visible)
    }
}
