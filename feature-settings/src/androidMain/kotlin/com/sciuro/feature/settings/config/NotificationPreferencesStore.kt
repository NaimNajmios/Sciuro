package com.sciuro.feature.settings.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class NotificationPreferencesStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sciuro_notification_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isEnabled(type: String): Boolean =
        prefs.getBoolean("notif_${type}_enabled", true)

    fun setEnabled(type: String, enabled: Boolean) {
        prefs.edit().putBoolean("notif_${type}_enabled", enabled).apply()
    }

    fun getInt(type: String, key: String, default: Int): Int =
        prefs.getInt("notif_${type}_$key", default)

    fun setInt(type: String, key: String, value: Int) {
        prefs.edit().putInt("notif_${type}_$key", value).apply()
    }

    fun getDouble(type: String, key: String, default: Double): Double {
        val raw = prefs.getString("notif_${type}_$key", null) ?: return default
        return raw.toDoubleOrNull() ?: default
    }

    fun setDouble(type: String, key: String, value: Double) {
        prefs.edit().putString("notif_${type}_$key", value.toString()).apply()
    }

    companion object {
        const val BACKUP_REMINDER = "backup_reminder"
        const val RUNWAY_ALERT = "runway_alert"
        const val LARGE_TXN = "large_txn"
        const val UNUSUAL_SPENDING = "unusual_spending"
        const val DEBT_DUE = "debt_due"
        const val INCOME_NOT_ARRIVED = "income_not_arrived"
        const val WEEKLY_DIGEST = "weekly_digest"
        const val BILL_AUTOPAY = "bill_autopay"
        const val MILESTONE = "milestone"
        const val BNPL_RISK = "bnpl_risk"
        const val CASH_ANOMALY = "cash_anomaly"
        const val TRANSFER_REVIEW = "transfer_review"
        const val REVIEW_REMINDER = "review_reminder"
    }
}
