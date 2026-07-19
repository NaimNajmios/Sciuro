package com.sciuro.core.obligations.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.model.ObligationFrequency

class ObligationRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createObligation(obligation: Obligation): Obligation {
        return withAudit(
            entityType = EntityType.RECURRING_OBLIGATION,
            entityId = obligation.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = obligation.toString(),
            source = AuditSource.SYSTEM_AUTO
        ) {
            val now = currentTimeMillis()
            database.obligationQueries.insertObligation(
                id = obligation.id,
                name = obligation.name,
                amount = obligation.amount,
                frequency = obligation.frequency.name,
                next_due_date = obligation.nextDueDate,
                category_id = obligation.categoryId,
                account_id = obligation.accountId,
                is_active = if (obligation.isActive) 1L else 0L,
                created_at = now,
                updated_at = now
            )
            obligation
        }
    }
}
