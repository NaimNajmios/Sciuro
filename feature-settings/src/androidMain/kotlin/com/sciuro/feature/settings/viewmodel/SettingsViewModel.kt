package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.feature.settings.config.NotificationPreferencesStore
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
    val connectionTestState: ConnectionTestState = ConnectionTestState.Idle,
    // Notification preferences
    val notifBackupReminder: Boolean = true,
    val notifBackupInterval: Int = 7,
    val notifRunwayAlert: Boolean = true,
    val notifLargeTxn: Boolean = true,
    val notifLargeTxnThreshold: Double = 500.0,
    val notifDebtDue: Boolean = true,
    val notifDebtDueDaysBefore: Int = 7,
    val notifIncomeNotArrived: Boolean = true,
    val notifReviewReminder: Boolean = true,
    val notifBillAutopay: Boolean = true,
    val notifWeeklyDigest: Boolean = true,
    val notifMilestone: Boolean = true,
    val notifBnplRisk: Boolean = true,
    val notifCashAnomaly: Boolean = true,
    val notifTransferReview: Boolean = true
)

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Success : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

class SettingsViewModel(
    private val settingsProvider: SettingsProvider,
    private val prefsStore: NotificationPreferencesStore
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
            isDeveloperOptionsVisible = settingsProvider.isDeveloperOptionsVisible(),
            notifBackupReminder = prefsStore.isEnabled(NotificationPreferencesStore.BACKUP_REMINDER),
            notifBackupInterval = prefsStore.getInt(NotificationPreferencesStore.BACKUP_REMINDER, "interval", 7),
            notifRunwayAlert = prefsStore.isEnabled(NotificationPreferencesStore.RUNWAY_ALERT),
            notifLargeTxn = prefsStore.isEnabled(NotificationPreferencesStore.LARGE_TXN),
            notifLargeTxnThreshold = prefsStore.getDouble(NotificationPreferencesStore.LARGE_TXN, "threshold", 500.0),
            notifDebtDue = prefsStore.isEnabled(NotificationPreferencesStore.DEBT_DUE),
            notifDebtDueDaysBefore = prefsStore.getInt(NotificationPreferencesStore.DEBT_DUE, "days_before", 7),
            notifIncomeNotArrived = prefsStore.isEnabled(NotificationPreferencesStore.INCOME_NOT_ARRIVED),
            notifReviewReminder = prefsStore.isEnabled(NotificationPreferencesStore.REVIEW_REMINDER),
            notifBillAutopay = prefsStore.isEnabled(NotificationPreferencesStore.BILL_AUTOPAY),
            notifWeeklyDigest = prefsStore.isEnabled(NotificationPreferencesStore.WEEKLY_DIGEST),
            notifMilestone = prefsStore.isEnabled(NotificationPreferencesStore.MILESTONE),
            notifBnplRisk = prefsStore.isEnabled(NotificationPreferencesStore.BNPL_RISK),
            notifCashAnomaly = prefsStore.isEnabled(NotificationPreferencesStore.CASH_ANOMALY),
            notifTransferReview = prefsStore.isEnabled(NotificationPreferencesStore.TRANSFER_REVIEW)
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

    // -- Notification preference toggles --

    fun setNotifBackupReminder(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.BACKUP_REMINDER, enabled)
        _uiState.value = _uiState.value.copy(notifBackupReminder = enabled)
    }

    fun setNotifBackupInterval(days: Int) {
        prefsStore.setInt(NotificationPreferencesStore.BACKUP_REMINDER, "interval", days)
        _uiState.value = _uiState.value.copy(notifBackupInterval = days)
    }

    fun setNotifRunwayAlert(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.RUNWAY_ALERT, enabled)
        _uiState.value = _uiState.value.copy(notifRunwayAlert = enabled)
    }

    fun setNotifLargeTxn(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.LARGE_TXN, enabled)
        _uiState.value = _uiState.value.copy(notifLargeTxn = enabled)
    }

    fun setNotifLargeTxnThreshold(threshold: Double) {
        prefsStore.setDouble(NotificationPreferencesStore.LARGE_TXN, "threshold", threshold)
        _uiState.value = _uiState.value.copy(notifLargeTxnThreshold = threshold)
    }

    fun setNotifDebtDue(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.DEBT_DUE, enabled)
        _uiState.value = _uiState.value.copy(notifDebtDue = enabled)
    }

    fun setNotifDebtDueDaysBefore(days: Int) {
        prefsStore.setInt(NotificationPreferencesStore.DEBT_DUE, "days_before", days)
        _uiState.value = _uiState.value.copy(notifDebtDueDaysBefore = days)
    }

    fun setNotifIncomeNotArrived(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.INCOME_NOT_ARRIVED, enabled)
        _uiState.value = _uiState.value.copy(notifIncomeNotArrived = enabled)
    }

    fun setNotifReviewReminder(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.REVIEW_REMINDER, enabled)
        _uiState.value = _uiState.value.copy(notifReviewReminder = enabled)
    }

    fun setNotifBillAutopay(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.BILL_AUTOPAY, enabled)
        _uiState.value = _uiState.value.copy(notifBillAutopay = enabled)
    }

    fun setNotifWeeklyDigest(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.WEEKLY_DIGEST, enabled)
        _uiState.value = _uiState.value.copy(notifWeeklyDigest = enabled)
    }

    fun setNotifMilestone(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.MILESTONE, enabled)
        _uiState.value = _uiState.value.copy(notifMilestone = enabled)
    }

    fun setNotifBnplRisk(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.BNPL_RISK, enabled)
        _uiState.value = _uiState.value.copy(notifBnplRisk = enabled)
    }

    fun setNotifCashAnomaly(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.CASH_ANOMALY, enabled)
        _uiState.value = _uiState.value.copy(notifCashAnomaly = enabled)
    }

    fun setNotifTransferReview(enabled: Boolean) {
        prefsStore.setEnabled(NotificationPreferencesStore.TRANSFER_REVIEW, enabled)
        _uiState.value = _uiState.value.copy(notifTransferReview = enabled)
    }
}
