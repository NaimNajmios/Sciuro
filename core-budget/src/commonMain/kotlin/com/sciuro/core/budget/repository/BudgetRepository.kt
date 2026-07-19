package com.sciuro.core.budget.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.budget.model.Budget

class BudgetRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createBudget(budget: Budget): Budget {
        return withAudit(
            entityType = EntityType.BUDGET,
            entityId = budget.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = budget.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.budgetQueries.insertBudget(
                id = budget.id,
                category_id = budget.categoryId,
                allocated_amount = budget.allocatedAmount,
                current_spent = budget.currentSpent,
                period = budget.period.name,
                created_at = now,
                updated_at = now
            )
            budget
        }
    }
}
