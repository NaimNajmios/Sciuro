package com.sciuro.core.budget.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.budget.model.Budget
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

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

    fun observeBudgets(): Flow<List<com.sciuro.core.ledger.db.Budget_record>> {
        return database.budgetQueries.selectAllBudgets()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
}
