package com.sciuro.core.ledger.engine

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.repository.AccountRepository

class ReconciliationEngine(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val accountRepository: AccountRepository
) : AuditableRepository(auditRepository) {

    suspend fun reconcileAccount(accountId: String, declaredBalance: Double) {
        reconcileAccountWithReason(accountId, declaredBalance, "MANUAL_OVERRIDE")
    }

    suspend fun reconcileAccountWithReason(accountId: String, declaredBalance: Double, reason: String) {
        // 1. Calculate sum of all transactions for this account
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
            .filter { it.account_id == accountId }
            
        var calculatedSum = 0.0
        for (tx in allTransactions) {
            if (tx.direction == "INFLOW") {
                calculatedSum += tx.amount
            } else {
                calculatedSum -= tx.amount
            }
        }
        
        // 2. Add all existing cash adjustments
        val existingAdjustments = database.cashAdjustmentQueries.selectAdjustmentsByAccount(accountId).executeAsList()
        val adjustmentsSum = existingAdjustments.sumOf { it.amount }
        
        val actualBalance = calculatedSum + adjustmentsSum
        
        // 3. Compare with declared balance
        val diff = declaredBalance - actualBalance
        
        if (kotlin.math.abs(diff) > 0.01) {
            // Need to insert a cash adjustment
            val adjustmentId = generateUuid()
            
            withAudit(
                entityType = EntityType.CASH_ADJUSTMENT,
                entityId = adjustmentId,
                action = AuditAction.CREATE,
                beforeState = "Calculated: $actualBalance",
                afterState = "Declared: $declaredBalance, Diff: $diff",
                source = AuditSource.USER_MANUAL
            ) {
                val now = currentTimeMillis()
                
                database.cashAdjustmentQueries.insertAdjustment(
                    id = adjustmentId,
                    account_id = accountId,
                    amount = diff,
                    reason = reason,
                    remark = null,
                    timestamp = now,
                    created_at = now
                )
                
                // Update the account balance directly with the diff
                accountRepository.updateBalance(accountId, diff)
            }
        }
    }
}
