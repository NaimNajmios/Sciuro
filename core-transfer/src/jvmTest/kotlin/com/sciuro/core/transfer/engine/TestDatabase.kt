package com.sciuro.core.transfer.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase

class FakeAuditRepository : AuditRepository {
    override fun logMutation(log: AuditLog) {}
    override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
    override suspend fun getAllLogs() = emptyList<AuditLog>()
    override fun getAuditIntegrityGaps(): Long = 0L
}

object TestDatabase {
    fun create(): SciuroDatabase {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        return SciuroDatabase(driver)
    }
}
