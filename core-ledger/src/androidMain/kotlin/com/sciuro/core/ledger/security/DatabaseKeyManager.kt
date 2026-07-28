package com.sciuro.core.ledger.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

object DatabaseKeyManager {
    private const val PREFS_NAME = "sciuro_db_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    fun getOrGeneratePassphrase(context: Context): ByteArray {
        val masterKey = masterKey(context)
        val sharedPreferences = encryptedPrefs(context, masterKey)

        var passphraseStr = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        if (passphraseStr == null) {
            passphraseStr = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, passphraseStr).apply()
        }

        return passphraseStr.toByteArray(Charsets.UTF_8)
    }

    fun passphraseExists(context: Context): Boolean {
        return try {
            val masterKey = masterKey(context)
            val sharedPreferences = encryptedPrefs(context, masterKey)
            sharedPreferences.contains(KEY_DB_PASSPHRASE)
        } catch (_: Exception) {
            false
        }
    }

    fun getStoredPassphrase(context: Context): ByteArray? {
        return try {
            val masterKey = masterKey(context)
            val sharedPreferences = encryptedPrefs(context, masterKey)
            sharedPreferences.getString(KEY_DB_PASSPHRASE, null)?.toByteArray(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun storePassphrase(context: Context, passphrase: ByteArray) {
        val masterKey = masterKey(context)
        val sharedPreferences = encryptedPrefs(context, masterKey)
        sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, String(passphrase, Charsets.UTF_8)).apply()
    }

    private fun masterKey(context: Context) = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private fun encryptedPrefs(context: Context, masterKey: MasterKey) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
