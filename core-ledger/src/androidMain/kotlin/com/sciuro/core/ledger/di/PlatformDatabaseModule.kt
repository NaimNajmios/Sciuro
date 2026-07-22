package com.sciuro.core.ledger.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.dsl.module

import com.sciuro.core.ledger.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

val platformDatabaseModule = module {
    single<SqlDriver> { 
        val context = get<Context>()
        
        // Initialize SQLiteDatabase for SQLCipher (required for Android 9+)
        SQLiteDatabase.loadLibs(context)
        
        val passphrase = DatabaseKeyManager.getOrGeneratePassphrase(context)
        val factory = SupportFactory(passphrase)
        
        val dbFile = context.getDatabasePath("sciuro.db")
        if (dbFile.exists()) {
            try {
                // Attempt to open it with SQLCipher. If it's an unencrypted DB, 
                // this will throw "file is not a database".
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, 
                    String(passphrase), 
                    null, 
                    SQLiteDatabase.OPEN_READONLY
                )
                db.close()
            } catch (e: Exception) {
                // It failed to open with the key, so drop the existing unencrypted database.
                context.deleteDatabase("sciuro.db")
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
