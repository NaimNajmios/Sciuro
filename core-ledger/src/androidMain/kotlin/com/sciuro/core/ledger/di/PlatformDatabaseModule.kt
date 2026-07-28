package com.sciuro.core.ledger.di

import android.content.Context
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.dsl.module

import com.sciuro.core.ledger.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import java.io.File

private const val TAG = "SciuroDB"

val platformDatabaseModule = module {
    single<SqlDriver> {
        val context = get<Context>()

        SQLiteDatabase.loadLibs(context)

        val dbFile = context.getDatabasePath("sciuro.db")

        // Phase 1: Key-loss detection before touching passphrase storage
        if (dbFile.exists() && !DatabaseKeyManager.passphraseExists(context)) {
            val quarantined = File(dbFile.parentFile, "sciuro.db.quarantined.${System.currentTimeMillis()}")
            dbFile.renameTo(quarantined)
            Log.w(TAG, "Database exists but no stored passphrase found (key-loss). Quarantined to $quarantined")
        }

        val passphrase = DatabaseKeyManager.getOrGeneratePassphrase(context)
        val factory = SupportFactory(passphrase)

        // Phase 2: Verify existing DB opens with its passphrase
        if (dbFile.exists() && DatabaseKeyManager.passphraseExists(context)) {
            try {
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    String(passphrase),
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                db.close()
            } catch (e: Exception) {
                val quarantined = File(dbFile.parentFile, "sciuro.db.quarantined.${System.currentTimeMillis()}")
                dbFile.renameTo(quarantined)
                Log.w(TAG, "Failed to open database with stored passphrase (corruption? version mismatch?). Quarantined to $quarantined", e)
                File("${dbFile.absolutePath}-wal").delete()
                File("${dbFile.absolutePath}-shm").delete()
                File("${dbFile.absolutePath}-journal").delete()
            }
        }

        AndroidSqliteDriver(
            schema = SciuroDatabase.Schema,
            context = context,
            name = "sciuro.db",
            factory = factory
        )
    }
}
