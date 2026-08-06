package com.sciuro.core.ledger.security

import android.content.Context
import net.sqlcipher.database.SQLiteDatabase

/**
 * Runs a full SQLCipher `PRAGMA integrity_check` against the live database using the
 * stored passphrase. The database is opened read-only so the check never mutates it.
 */
object DatabaseIntegrityChecker {

    sealed interface IntegrityResult {
        val message: String

        data class Success(override val message: String) : IntegrityResult
        data class Failure(override val message: String) : IntegrityResult
    }

    fun check(context: Context, dbName: String = "sciuro.db"): IntegrityResult {
        val passphrase = DatabaseKeyManager.getStoredPassphrase(context)
            ?: return IntegrityResult.Failure("No stored database passphrase available")

        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return IntegrityResult.Failure("Database file not found")

        return try {
            SQLiteDatabase.loadLibs(context)
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                String(passphrase),
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val message = try {
                readIntegrityMessage(db)
            } finally {
                db.close()
            }
            when (message) {
                null -> IntegrityResult.Failure("PRAGMA integrity_check returned no rows")
                "ok" -> IntegrityResult.Success(message)
                else -> IntegrityResult.Failure(message)
            }
        } catch (e: Exception) {
            IntegrityResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun readIntegrityMessage(db: SQLiteDatabase): String? {
        val cursor = db.rawQuery("PRAGMA integrity_check", null)
        return try {
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } finally {
            cursor.close()
        }
    }
}
