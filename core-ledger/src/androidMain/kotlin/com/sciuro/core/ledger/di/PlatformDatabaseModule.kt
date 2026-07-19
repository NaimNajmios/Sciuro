package com.sciuro.core.ledger.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.dsl.module

val platformDatabaseModule = module {
    single<SqlDriver> { AndroidSqliteDriver(SciuroDatabase.Schema, get<Context>(), "sciuro.db") }
}
