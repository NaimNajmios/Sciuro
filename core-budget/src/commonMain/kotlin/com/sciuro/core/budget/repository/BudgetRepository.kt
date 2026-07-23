package com.sciuro.core.budget.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.budget.model.Budget
import com.sciuro.core.budget.model.BudgetPeriod
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
                rollover = if (budget.rollover) 1L else 0L,
                alert_threshold_percent = budget.alertThresholdPercent,
                created_at = now,
                updated_at = now
            )
            budget
        }
    }

    suspend fun updateBudget(id: String, allocatedAmount: Double, period: String): Budget {
        val oldBudget = database.budgetQueries.selectBudgetById(id).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Budget not found: $id")

        return withAudit(
            entityType = EntityType.BUDGET,
            entityId = id,
            action = AuditAction.UPDATE,
            beforeState = oldBudget.toString(),
            afterState = "allocatedAmount=$allocatedAmount, period=$period",
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.budgetQueries.updateBudget(
                allocated_amount = allocatedAmount,
                period = period,
                updated_at = now,
                id = id
            )
            Budget(
                id = id,
                categoryId = oldBudget.category_id,
                allocatedAmount = allocatedAmount,
                currentSpent = oldBudget.current_spent,
                period = BudgetPeriod.valueOf(period),
                rollover = oldBudget.rollover == 1L,
                alertThresholdPercent = oldBudget.alert_threshold_percent
            )
        }
    }

    suspend fun deleteBudget(id: String) {
        val oldBudget = database.budgetQueries.selectBudgetById(id).executeAsOneOrNull() ?: return

        withAudit(
            entityType = EntityType.BUDGET,
            entityId = id,
            action = AuditAction.DELETE,
            beforeState = oldBudget.toString(),
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            database.budgetQueries.deleteBudget(id)
        }
    }

    fun observeBudgets(): Flow<List<com.sciuro.core.ledger.db.Budget_record>> {
        return database.budgetQueries.selectAllBudgets()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
}
