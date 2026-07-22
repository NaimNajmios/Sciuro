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
        
        try {
            AndroidSqliteDriver(
                schema = SciuroDatabase.Schema,
                context = context,
                name = "sciuro.db",
                factory = factory
            )
        } catch (e: Exception) {
            // If it fails to open, it's likely the old unencrypted database or corrupted. 
            // We drop it and recreate it (safe for pre-release development migration).
            context.deleteDatabase("sciuro.db")
            AndroidSqliteDriver(
                schema = SciuroDatabase.Schema,
                context = context,
                name = "sciuro.db",
                factory = factory
            )
        }
    }
}
