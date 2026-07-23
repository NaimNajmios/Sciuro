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
    fun getLlmConfig(): LlmParsingConfig = LlmParsingConfig()
}
