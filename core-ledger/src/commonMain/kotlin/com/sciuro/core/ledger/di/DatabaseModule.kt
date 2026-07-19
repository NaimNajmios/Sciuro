package com.sciuro.core.ledger.di

import app.cash.sqldelight.db.SqlDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { SciuroDatabase(get<SqlDriver>()) }
}
