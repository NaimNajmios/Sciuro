package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.feature.settings.config.NotificationPreferencesStore
import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val isLockEnabled: Boolean = false,
    val isDeveloperOptionsVisible: Boolean = false,
    val isLlmEnabled: Boolean = false,
    val isObligationAutoConfirmEnabled: Boolean = false,
    val budgetWarningThreshold: Float = 0.8f,
    val isQuietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val notifBackupReminder: Boolean = true,
    val notifRunwayAlert: Boolean = true,
    val notifLargeTxn: Boolean = true,
    val notifDebtDue: Boolean = true,
    val notifIncomeNotArrived: Boolean = true,
    val notifReviewReminder: Boolean = true,
    val notifBillAutopay: Boolean = true,
    val notifWeeklyDigest: Boolean = true,
    val notifMilestone: Boolean = true,
    val notifBnplRisk: Boolean = true,
    val notifCashAnomaly: Boolean = true,
    val notifTransferReview: Boolean = true
)

class SettingsViewModel(
    private val settingsProvider: SettingsProvider,
    private val prefsStore: NotificationPreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun refresh() {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            isLockEnabled = settingsProvider.isLockEnabled(),
            isDeveloperOptionsVisible = settingsProvider.isDeveloperOptionsVisible(),
            isLlmEnabled = settingsProvider.isLlmEnabled(),
            isObligationAutoConfirmEnabled = settingsProvider.isObligationAutoConfirmEnabled(),
            budgetWarningThreshold = settingsProvider.getBudgetWarningThreshold(),
            isQuietHoursEnabled = settingsProvider.isQuietHoursEnabled(),
            quietHoursStart = settingsProvider.getQuietHoursStart(),
            quietHoursEnd = settingsProvider.getQuietHoursEnd(),
            notifBackupReminder = prefsStore.isEnabled(NotificationPreferencesStore.BACKUP_REMINDER),
            notifRunwayAlert = prefsStore.isEnabled(NotificationPreferencesStore.RUNWAY_ALERT),
            notifLargeTxn = prefsStore.isEnabled(NotificationPreferencesStore.LARGE_TXN),
            notifDebtDue = prefsStore.isEnabled(NotificationPreferencesStore.DEBT_DUE),
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

    fun setLockEnabled(enabled: Boolean) {
        settingsProvider.setLockEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLockEnabled = enabled)
    }

    fun setDeveloperOptionsVisible(visible: Boolean) {
        settingsProvider.setDeveloperOptionsVisible(visible)
        _uiState.value = _uiState.value.copy(isDeveloperOptionsVisible = visible)
    }
}
