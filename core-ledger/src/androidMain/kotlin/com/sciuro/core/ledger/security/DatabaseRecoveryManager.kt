package com.sciuro.core.ledger.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import java.io.File

/**
 * Owns all database quarantine bookkeeping: quarantine metadata persisted in the encrypted
 * DB preferences, discovery of preserved (quarantined) database files, and the startup
 * validation that decides whether the previous database can still be opened.
 */
class DatabaseRecoveryManager(private val context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val dbFile: File get() = context.getDatabasePath(DB_FILE_NAME)

    private fun dbDir(): File? = dbFile.parentFile

    /**
     * Runs the two-phase startup validation: key-loss detection before passphrase
     * generation, then corruption detection via a read-only open. Any database that
     * cannot be opened is quarantined and recorded. Returns true when a quarantine
     * happened during this call.
     */
    fun validateDatabaseOnStartup(): Boolean {
        var quarantined = false

        if (dbFile.exists() && !DatabaseKeyManager.passphraseExists(context)) {
            quarantined = quarantineDatabase(dbFile, "key-loss") || quarantined
        }

        if (dbFile.exists() && DatabaseKeyManager.passphraseExists(context)) {
            val passphrase = DatabaseKeyManager.getStoredPassphrase(context)
            if (passphrase == null) {
                quarantined = quarantineDatabase(dbFile, "key-loss") || quarantined
            } else {
                try {
                    SQLiteDatabase.loadLibs(context)
                    val db = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        String(passphrase),
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )
                    db.close()
                } catch (e: Exception) {
                    quarantined = quarantineDatabase(dbFile, "corruption", e) || quarantined
                }
            }
        }

        return quarantined
    }

    fun hasQuarantinedFiles(): Boolean = listQuarantinedFiles().isNotEmpty()

    fun listQuarantinedFiles(): List<File> {
        val dir = dbDir() ?: return emptyList()
        return dir.listFiles { file ->
            QuarantineFiles.isQuarantineFileName(file.name)
        }?.sortedBy { it.name } ?: emptyList()
    }

    fun quarantineCount(): Int = prefs.getInt(KEY_QUARANTINE_COUNT, 0)

    fun lastQuarantineTimestamp(): Long = prefs.getLong(KEY_LAST_QUARANTINE_TS, 0L)

    fun isRecoveryAcknowledged(): Boolean = prefs.getBoolean(KEY_RECOVERY_ACKNOWLEDGED, false)

    fun isRecoveryPending(): Boolean = hasQuarantinedFiles() && !isRecoveryAcknowledged()

    fun markRecoveryAcknowledged() {
        prefs.edit().putBoolean(KEY_RECOVERY_ACKNOWLEDGED, true).apply()
    }

    /**
     * Renames [dbFile] to a timestamped quarantine name, cleans WAL/SHM/journal sidecars,
     * and records the event in encrypted preferences. Returns true when the rename succeeded.
     */
    fun quarantineDatabase(dbFile: File, reason: String, cause: Throwable? = null): Boolean {
        val quarantined = File(
            dbFile.parentFile,
            QuarantineFiles.quarantineFileName(System.currentTimeMillis())
        )
        val renamed = dbFile.renameTo(quarantined)

        File("${dbFile.absolutePath}-wal").delete()
        File("${dbFile.absolutePath}-shm").delete()
        File("${dbFile.absolutePath}-journal").delete()

        if (renamed) {
            prefs.edit()
                .putInt(KEY_QUARANTINE_COUNT, quarantineCount() + 1)
                .putLong(KEY_LAST_QUARANTINE_TS, System.currentTimeMillis())
                .putBoolean(KEY_RECOVERY_ACKNOWLEDGED, false)
                .apply()
            if (cause != null) {
                Log.w(TAG, "Database quarantined ($reason). Preserved to $quarantined", cause)
            } else {
                Log.w(TAG, "Database quarantined ($reason). Preserved to $quarantined")
            }
        } else {
            Log.e(TAG, "Database quarantine failed ($reason): could not rename $dbFile to $quarantined")
        }
        return renamed
    }

    fun getLastIntegrityCheckMs(): Long = prefs.getLong(KEY_LAST_INTEGRITY_CHECK, 0L)

    fun setLastIntegrityCheckMs(timestampMs: Long) {
        prefs.edit().putLong(KEY_LAST_INTEGRITY_CHECK, timestampMs).apply()
    }

    fun getLastIntegrityResult(): String? = prefs.getString(KEY_LAST_INTEGRITY_RESULT, null)

    fun setLastIntegrityResult(result: String) {
        prefs.edit().putString(KEY_LAST_INTEGRITY_RESULT, result).apply()
    }

    companion object {
        private const val TAG = "SciuroDB"
        private const val PREFS_NAME = "sciuro_db_secure_prefs"
        private const val DB_FILE_NAME = "sciuro.db"
        private const val KEY_QUARANTINE_COUNT = "quarantine_count"
        private const val KEY_LAST_QUARANTINE_TS = "last_quarantine_timestamp"
        private const val KEY_RECOVERY_ACKNOWLEDGED = "recovery_acknowledged"
        private const val KEY_LAST_INTEGRITY_CHECK = "last_integrity_check_ms"
        private const val KEY_LAST_INTEGRITY_RESULT = "last_integrity_result"
    }
}
