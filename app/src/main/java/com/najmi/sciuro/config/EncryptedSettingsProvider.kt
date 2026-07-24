package com.najmi.sciuro.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.ledger.config.SettingsProvider

class EncryptedSettingsProvider(context: Context) : SettingsProvider {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sciuro_secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
    }

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
    }

    override fun getLlmConfig(): LlmParsingConfig {
        val base = LlmParsingConfig()
        return base.copy(
            modelName = getLlmModelName(),
            trustValidatedLlm = isTrustValidatedLlmEnabled()
        )
    }

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
    }

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
    }

    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }

    override fun isLlmEnabled(): Boolean {
        return sharedPreferences.getBoolean("is_llm_enabled", true)
    }

    override fun setLlmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("is_llm_enabled", enabled).apply()
    }

    override fun isLockEnabled(): Boolean {
        return sharedPreferences.getBoolean("is_lock_enabled", false)
    }

    override fun setLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("is_lock_enabled", enabled).apply()
    }

    override fun getApiKey(): String? {
        val key = sharedPreferences.getString("api_key", null)
        return if (key.isNullOrBlank()) null else key
    }

    override fun setApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }

    override fun getIngestionAllowlistAdditions(): Set<String> {
        val raw = sharedPreferences.getString("ingestion_allowlist_additions", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    override fun setIngestionAllowlistAdditions(packages: Set<String>) {
        sharedPreferences.edit().putString("ingestion_allowlist_additions", packages.joinToString(",")).apply()
    }

    override fun getIngestionAllowlistRemovals(): Set<String> {
        val raw = sharedPreferences.getString("ingestion_allowlist_removals", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    override fun setIngestionAllowlistRemovals(packages: Set<String>) {
        sharedPreferences.edit().putString("ingestion_allowlist_removals", packages.joinToString(",")).apply()
    }

    override fun isAutoConfirmEnabled(): Boolean {
        return sharedPreferences.getBoolean("auto_confirm_enabled", false)
    }

    override fun setAutoConfirmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("auto_confirm_enabled", enabled).apply()
    }

    override fun getAutoConfirmThreshold(): Int {
        return sharedPreferences.getInt("auto_confirm_threshold", 3)
    }

    override fun setAutoConfirmThreshold(threshold: Int) {
        sharedPreferences.edit().putInt("auto_confirm_threshold", threshold).apply()
    }

    override fun getManualPrice(key: String): Double? {
        val raw = sharedPreferences.getString("manual_price_$key", null) ?: return null
        return raw.toDoubleOrNull()
    }

    override fun setManualPrice(key: String, price: Double) {
        sharedPreferences.edit().putString("manual_price_$key", price.toString()).apply()
    }

    override fun isQuietHoursEnabled(): Boolean {
        return sharedPreferences.getBoolean("quiet_hours_enabled", false)
    }

    override fun setQuietHoursEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("quiet_hours_enabled", enabled).apply()
    }

    override fun getQuietHoursStart(): Int {
        return sharedPreferences.getInt("quiet_hours_start", 22)
    }

    override fun setQuietHoursStart(hour: Int) {
        sharedPreferences.edit().putInt("quiet_hours_start", hour).apply()
    }

    override fun getQuietHoursEnd(): Int {
        return sharedPreferences.getInt("quiet_hours_end", 7)
    }

    override fun setQuietHoursEnd(hour: Int) {
        sharedPreferences.edit().putInt("quiet_hours_end", hour).apply()
    }

    override fun isTrustValidatedLlmEnabled(): Boolean {
        return sharedPreferences.getBoolean("trust_validated_llm", false)
    }

    override fun setTrustValidatedLlmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("trust_validated_llm", enabled).apply()
    }

    override fun isTransactionAutoConfirmEnabled(): Boolean {
        return sharedPreferences.getBoolean("transaction_auto_confirm", false)
    }

    override fun setTransactionAutoConfirmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("transaction_auto_confirm", enabled).apply()
    }

    override fun getSilentAutoConfirmThreshold(): Float {
        return sharedPreferences.getFloat("silent_auto_confirm_threshold", 0.95f)
    }

    override fun setSilentAutoConfirmThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("silent_auto_confirm_threshold", threshold).apply()
    }
}
