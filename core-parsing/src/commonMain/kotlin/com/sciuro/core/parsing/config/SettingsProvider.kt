package com.sciuro.core.parsing.config

interface SettingsProvider {
    fun isLlmEnabled(): Boolean
    fun setLlmEnabled(enabled: Boolean)
    fun getApiKey(): String?
    fun setApiKey(apiKey: String)
    fun getLlmConfig(): LlmParsingConfig = LlmParsingConfig()
}
