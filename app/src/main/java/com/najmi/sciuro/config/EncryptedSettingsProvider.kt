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
    }

    override fun setLlmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("is_llm_enabled", enabled).apply()
    }

    override fun getApiKey(): String? {
        val key = sharedPreferences.getString("api_key", null)
        return if (key.isNullOrBlank()) null else key
    }

    override fun setApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }
}
