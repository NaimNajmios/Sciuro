package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.sciuro.feature.settings.config.NotificationPreferencesStore
import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSettingsUiState(
    val isQuietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
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

class NotificationSettingsViewModel(
    private val settingsProvider: SettingsProvider,
    private val prefsStore: NotificationPreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = NotificationSettingsUiState(
            isQuietHoursEnabled = settingsProvider.isQuietHoursEnabled(),
            quietHoursStart = settingsProvider.getQuietHoursStart(),
            quietHoursEnd = settingsProvider.getQuietHoursEnd(),
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
