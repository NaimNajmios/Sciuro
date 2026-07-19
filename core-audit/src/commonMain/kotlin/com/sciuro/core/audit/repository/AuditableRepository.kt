package com.sciuro.core.audit.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid

abstract class AuditableRepository(
    private val auditRepository: AuditRepository
) {
    
    protected suspend fun <T> withAudit(
        entityType: EntityType,
        entityId: String,
        action: AuditAction,
        beforeState: String?,
        afterState: String?,
        source: AuditSource,
        confidence: Float? = null,
        mutation: suspend () -> T
    ): T {
        // 1. Execute the mutation
        val result = mutation()
        
        // 2. Construct the audit log
        val log = AuditLog(
            id = generateUuid(),
            entityType = entityType,
            entityId = entityId,
            action = action,
            beforeState = beforeState,
            afterState = afterState,
            source = source,
            confidence = confidence,
            timestamp = currentTimeMillis()
        )
        
        // 3. Write the log
        auditRepository.logMutation(log)
        
        return result
    }
}
