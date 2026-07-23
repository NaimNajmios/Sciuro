package com.sciuro.core.ledger.config

interface SettingsProvider {
    fun getLlmModelName(): String
    fun setLlmModelName(name: String)
    fun getQuickLabels(): List<String>
    fun setQuickLabels(labels: List<String>)
    fun getBudgetWarningThreshold(): Float
    fun setBudgetWarningThreshold(threshold: Float)
    fun isLlmEnabled(): Boolean
    fun setLlmEnabled(enabled: Boolean)
    fun getApiKey(): String?
    fun setApiKey(apiKey: String)
    fun isLockEnabled(): Boolean
    fun setLockEnabled(enabled: Boolean)
    fun getLlmConfig(): LlmParsingConfig = LlmParsingConfig()
    fun getIngestionAllowlistAdditions(): Set<String>
    fun setIngestionAllowlistAdditions(packages: Set<String>)
    fun getIngestionAllowlistRemovals(): Set<String>
    fun setIngestionAllowlistRemovals(packages: Set<String>)
    fun isAutoConfirmEnabled(): Boolean
    fun setAutoConfirmEnabled(enabled: Boolean)
    fun getAutoConfirmThreshold(): Int
    fun setAutoConfirmThreshold(threshold: Int)
    fun getManualPrice(key: String): Double?
    fun setManualPrice(key: String, price: Double)
    fun isQuietHoursEnabled(): Boolean
    fun setQuietHoursEnabled(enabled: Boolean)
    fun getQuietHoursStart(): Int
    fun setQuietHoursStart(hour: Int)
    fun getQuietHoursEnd(): Int
    fun setQuietHoursEnd(hour: Int)
}
