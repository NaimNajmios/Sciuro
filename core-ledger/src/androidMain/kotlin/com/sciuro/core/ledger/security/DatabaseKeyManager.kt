package com.sciuro.core.ledger.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

object DatabaseKeyManager {
    private const val PREFS_NAME = "sciuro_db_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    fun getOrGeneratePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphraseStr = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
        if (passphraseStr == null) {
            passphraseStr = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, passphraseStr).apply()
        }
        
        return passphraseStr.toByteArray()
    }
}
