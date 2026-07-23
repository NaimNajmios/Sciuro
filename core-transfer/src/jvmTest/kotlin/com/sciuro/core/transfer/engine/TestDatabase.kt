package com.sciuro.core.transfer.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase

class FakeAuditRepository : AuditRepository {
    override suspend fun logMutation(log: AuditLog) {}
    override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
    override suspend fun getAllLogs() = emptyList<AuditLog>()
}

object TestDatabase {
    fun create(): SciuroDatabase {
        val driver: SqlDriver = JdbcDriver("jdbc:sqlite::memory:")
        SciuroDatabase.Schema.create(driver)
        return SciuroDatabase(driver)
    }
}
