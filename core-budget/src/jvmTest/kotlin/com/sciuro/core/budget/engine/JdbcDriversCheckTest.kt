package com.sciuro.core.budget.engine

import app.cash.sqldelight.driver.jdbc.JdbcDrivers
import org.sqlite.SQLiteDataSource

class JdbcDriversCheckTest {
    @kotlin.test.Test
    fun check() {
        val ds = SQLiteDataSource()
        ds.setUrl("jdbc:sqlite::memory:")
        val driver = JdbcDrivers.fromDataSource(ds)
        driver.close()
    }
}
