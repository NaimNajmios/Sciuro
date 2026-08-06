package com.sciuro.core.ledger.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.dsl.module

import com.sciuro.core.ledger.security.DatabaseKeyManager
import com.sciuro.core.ledger.security.DatabaseRecoveryManager
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

val platformDatabaseModule = module {
    single { DatabaseRecoveryManager(get()) }

    single<SqlDriver> {
        val context = get<Context>()

        SQLiteDatabase.loadLibs(context)

        val recoveryManager = get<DatabaseRecoveryManager>()

        // Two-phase startup validation: key-loss detection before passphrase generation,
        // then corruption detection. Both paths quarantine instead of deleting.
        recoveryManager.validateDatabaseOnStartup()

        val passphrase = DatabaseKeyManager.getOrGeneratePassphrase(context)
        val factory = SupportFactory(passphrase)

        AndroidSqliteDriver(
            schema = SciuroDatabase.Schema,
            context = context,
            name = "sciuro.db",
            factory = factory
        )
    }
}
