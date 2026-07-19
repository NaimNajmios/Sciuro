package com.sciuro.core.ledger.audit

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase

class SqlDelightAuditRepository(
    private val database: SciuroDatabase
) : AuditRepository {

    override suspend fun logMutation(log: AuditLog) {
        database.auditLogQueries.insertLog(
            id = log.id,
            entityType = log.entityType.name,
            entityId = log.entityId,
            action = log.action.name,
            beforeState = log.beforeState,
            afterState = log.afterState,
            source = log.source.name,
            confidence = log.confidence?.toDouble(),
            timestamp = log.timestamp
        )
    }

    override suspend fun getLogsForEntity(entityId: String, entityType: EntityType): List<AuditLog> {
        return database.auditLogQueries
            .getLogsForEntity(entityId, entityType.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getAllLogs(): List<AuditLog> {
        return database.auditLogQueries
            .getAllLogs()
            .executeAsList()
            .map { it.toDomain() }
    }

    private fun com.sciuro.core.ledger.db.AuditLogEntity.toDomain(): AuditLog {
        return AuditLog(
            id = id,
            entityType = EntityType.valueOf(entityType),
            entityId = entityId,
            action = AuditAction.valueOf(action),
            beforeState = beforeState,
            afterState = afterState,
            source = AuditSource.valueOf(source),
            confidence = confidence?.toFloat(),
            timestamp = timestamp
        )
    }
}
