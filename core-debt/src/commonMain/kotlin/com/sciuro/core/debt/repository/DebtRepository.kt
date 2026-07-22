package com.sciuro.core.debt.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtType
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createDebt(debt: Debt): Debt {
        return withAudit(
            entityType = EntityType.DEBT,
            entityId = debt.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = debt.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.debtQueries.insertDebt(
                id = debt.id,
                name = debt.name,
                debt_type = debt.type.name,
                principal_amount = debt.principalAmount,
                remaining_balance = debt.remainingBalance,
                interest_rate = debt.interestRate,
                due_date = debt.dueDate,
                associated_account_id = debt.associatedAccountId,
                created_at = now,
                updated_at = now
            )
            debt
        }
    }
    
    suspend fun applyPayment(debtId: String, paymentAmount: Double) {
        val debt = database.debtQueries.selectAllDebts().executeAsList().find { it.id == debtId } ?: return
        val newBalance = debt.remaining_balance - paymentAmount
        
        withAudit(
            entityType = EntityType.DEBT,
            entityId = debtId,
            action = AuditAction.UPDATE,
            beforeState = "balance: ${debt.remaining_balance}",
            afterState = "balance: $newBalance",
            source = AuditSource.SYSTEM_AUTO
        ) {
            database.debtQueries.updateDebtBalance(newBalance, currentTimeMillis(), debtId)
        }
    }
    
    fun observeDebts(): Flow<List<Debt>> {
        return database.debtQueries.selectAllDebts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    Debt(
                        id = it.id,
                        name = it.name,
                        type = DebtType.valueOf(it.debt_type),
                        principalAmount = it.principal_amount,
                        remainingBalance = it.remaining_balance,
                        interestRate = it.interest_rate,
                        dueDate = it.due_date,
                        associatedAccountId = it.associated_account_id
                    )
                }
            }
    }
}
