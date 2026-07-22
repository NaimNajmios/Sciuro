package com.sciuro.core.transfer.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.sciuro.core.ledger.db.SciuroDatabase

object TestDatabase {
    fun create(): SciuroDatabase {
        val driver: SqlDriver = JdbcDriver("jdbc:sqlite::memory:")
        SciuroDatabase.Schema.create(driver)
        return SciuroDatabase(driver)
    }
}
