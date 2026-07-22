package com.najmi.sciuro.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sciuro.core.parsing.config.SettingsProvider

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

    override fun isLlmEnabled(): Boolean {
        return sharedPreferences.getBoolean("is_llm_enabled", true) // Default true
        override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("is_llm_enabled", enabled).apply()
        override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getApiKey(): String? {
        val key = sharedPreferences.getString("api_key", null)
        return if (key.isNullOrBlank()) null else key
        override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
        override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getLlmModelName(): String {
        return sharedPreferences.getString("llm_model_name", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setLlmModelName(name: String) {
        sharedPreferences.edit().putString("llm_model_name", name).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun getQuickLabels(): List<String> {
        val labelsString = sharedPreferences.getString("quick_labels", "Breakfast,Lunch,Dinner,Coffee,Groceries,Transport,Shopping,Salary,Others") ?: ""
        if (labelsString.isBlank()) return emptyList()
        return labelsString.split(",")
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}

    override fun setQuickLabels(labels: List<String>) {
        sharedPreferences.edit().putString("quick_labels", labels.joinToString(",")).apply()
        override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}
    override fun getBudgetWarningThreshold(): Float {
        return sharedPreferences.getFloat("budget_warning_threshold", 0.8f)
    }

    override fun setBudgetWarningThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat("budget_warning_threshold", threshold).apply()
    }
}


