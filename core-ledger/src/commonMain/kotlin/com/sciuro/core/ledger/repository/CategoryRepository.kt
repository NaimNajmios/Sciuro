package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Category

class CategoryRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createCategory(category: Category): Category {
        return withAudit(
            entityType = EntityType.CATEGORY,
            entityId = category.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = category.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.categoryQueries.insertCategory(
                id = category.id,
                name = category.name,
                icon = category.icon,
                color = category.color,
                created_at = now,
                updated_at = now
            )
            category
        }
    }
}
