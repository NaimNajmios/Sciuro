package com.sciuro.core.audit.repository

import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType

interface AuditRepository {
    fun logMutation(log: AuditLog)
    suspend fun getLogsForEntity(entityId: String, entityType: EntityType): List<AuditLog>
    suspend fun getAllLogs(): List<AuditLog>
    fun getAuditIntegrityGaps(): Long
}
