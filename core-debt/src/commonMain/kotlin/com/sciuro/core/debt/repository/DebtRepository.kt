package com.sciuro.core.debt.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
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

    private fun mapDebt(it: com.sciuro.core.ledger.db.Debt_record): Debt {
        return Debt(
            id = it.id,
            name = it.name,
            type = DebtType.valueOf(it.debt_type),
            direction = it.direction?.let { dir -> try { DebtDirection.valueOf(dir) } catch (_: Exception) { DebtDirection.I_OWE } } ?: DebtDirection.I_OWE,
            counterpartyName = it.counterparty_name,
            status = it.status?.let { s -> try { DebtStatus.valueOf(s) } catch (_: Exception) { DebtStatus.ACTIVE } } ?: DebtStatus.ACTIVE,
            principalAmount = it.principal_amount,
            remainingBalance = it.remaining_balance,
            interestRate = it.interest_rate,
            dueDate = it.due_date,
            associatedAccountId = it.associated_account_id,
            notes = it.notes
        )
    }

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
                direction = debt.direction.name,
                counterparty_name = debt.counterpartyName,
                status = debt.status.name,
                principal_amount = debt.principalAmount,
                remaining_balance = debt.remainingBalance,
                interest_rate = debt.interestRate,
                due_date = debt.dueDate,
                associated_account_id = debt.associatedAccountId,
                notes = debt.notes,
                created_at = now,
                updated_at = now
            )
            debt
        }
    }

    suspend fun updateDebt(debt: Debt): Debt {
        return withAudit(
            entityType = EntityType.DEBT,
            entityId = debt.id,
            action = AuditAction.UPDATE,
            beforeState = null,
            afterState = debt.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            database.debtQueries.updateDebt(
                name = debt.name,
                principal_amount = debt.principalAmount,
                remaining_balance = debt.remainingBalance,
                interest_rate = debt.interestRate,
                due_date = debt.dueDate,
                counterparty_name = debt.counterpartyName,
                notes = debt.notes,
                updated_at = currentTimeMillis(),
                id = debt.id
            )
            debt
        }
    }

    suspend fun deleteDebt(id: String) {
        withAudit(
            entityType = EntityType.DEBT,
            entityId = id,
            action = AuditAction.DELETE,
            beforeState = null,
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            database.debtQueries.deleteDebt(id)
        }
    }

    suspend fun markAsPaidOff(id: String) {
        withAudit(
            entityType = EntityType.DEBT,
            entityId = id,
            action = AuditAction.UPDATE,
            beforeState = null,
            afterState = "status: PAID_OFF",
            source = AuditSource.USER_MANUAL
        ) {
            database.debtQueries.markDebtAsPaidOff(currentTimeMillis(), id)
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
            .map { list -> list.map { mapDebt(it) } }
    }

    fun observeDebtsByDirection(direction: DebtDirection): Flow<List<Debt>> {
        return database.debtQueries.selectDebtsByDirection(direction.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { mapDebt(it) } }
    }
}
