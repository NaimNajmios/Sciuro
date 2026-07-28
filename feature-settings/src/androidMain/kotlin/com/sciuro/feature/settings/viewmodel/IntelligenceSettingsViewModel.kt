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

data class IntelligenceSettingsUiState(
    val isLlmEnabled: Boolean = false,
    val apiKey: String = "",
    val llmModelName: String = "llama-3.1-8b-instant",
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
    val isObligationAutoConfirmEnabled: Boolean = false
)

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Success : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

class IntelligenceSettingsViewModel(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntelligenceSettingsUiState())
    val uiState: StateFlow<IntelligenceSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = IntelligenceSettingsUiState(
            isLlmEnabled = settingsProvider.isLlmEnabled(),
            apiKey = settingsProvider.getApiKey() ?: "",
            llmModelName = settingsProvider.getLlmModelName(),
            isObligationAutoConfirmEnabled = settingsProvider.isObligationAutoConfirmEnabled()
        )
    }

    fun setLlmEnabled(enabled: Boolean) {
        settingsProvider.setLlmEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLlmEnabled = enabled)
    }

    fun setApiKey(apiKey: String) {
        settingsProvider.setApiKey(apiKey)
        _uiState.value = _uiState.value.copy(apiKey = apiKey, connectionTestState = ConnectionTestState.Idle)
    }

    fun setLlmModelName(name: String) {
        settingsProvider.setLlmModelName(name)
        _uiState.value = _uiState.value.copy(llmModelName = name)
    }

    fun setObligationAutoConfirmEnabled(enabled: Boolean) {
        settingsProvider.setObligationAutoConfirmEnabled(enabled)
        _uiState.value = _uiState.value.copy(isObligationAutoConfirmEnabled = enabled)
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
}
