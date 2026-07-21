package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class CashAdjustmentRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val accountRepository: AccountRepository
) : AuditableRepository(auditRepository) {

    fun observeAdjustmentsForAccount(accountId: String): Flow<List<com.sciuro.core.ledger.db.Cash_adjustment>> {
        return database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun observeAllAdjustments(): Flow<List<com.sciuro.core.ledger.db.Cash_adjustment>> {
        return database.cashAdjustmentQueries.selectAllAdjustments()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun observeRecentAdjustments(sinceTimestamp: Long): Flow<List<com.sciuro.core.ledger.db.Cash_adjustment>> {
        return database.cashAdjustmentQueries.selectRecentAdjustments(sinceTimestamp)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun createAdjustment(
        accountId: String,
        amount: Double,
        reason: String,
        source: AuditSource = AuditSource.USER_MANUAL
    ): com.sciuro.core.ledger.db.Cash_adjustment {
        val adjustmentId = generateUuid()
        val now = currentTimeMillis()

        return withAudit(
            entityType = EntityType.CASH_ADJUSTMENT,
            entityId = adjustmentId,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = "account=$accountId, amount=$amount, reason=$reason",
            source = source
        ) {
            database.cashAdjustmentQueries.insertAdjustment(
                id = adjustmentId,
                account_id = accountId,
                amount = amount,
                reason = reason,
                timestamp = now,
                created_at = now
            )

            accountRepository.updateBalance(accountId, amount)

            database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered(accountId).executeAsList().firstOrNull()
                ?: throw IllegalStateException("Adjustment not found after insert")
        }
    }

    suspend fun deleteAdjustment(adjustmentId: String) {
        val adjustment = database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered("").executeAsList()
            .find { it.id == adjustmentId } ?: return

        withAudit(
            entityType = EntityType.CASH_ADJUSTMENT,
            entityId = adjustmentId,
            action = AuditAction.DELETE,
            beforeState = "account=${adjustment.account_id}, amount=${adjustment.amount}, reason=${adjustment.reason}",
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            accountRepository.updateBalance(adjustment.account_id, -adjustment.amount)
            database.cashAdjustmentQueries.deleteAdjustment(adjustmentId)
        }
    }

    suspend fun getAdjustmentCountForAccount(accountId: String): Long {
        return database.cashAdjustmentQueries.selectAdjustmentCountByAccount(accountId).executeAsOne()
    }

    fun getAdjustmentsForAccountSync(accountId: String): List<com.sciuro.core.ledger.db.Cash_adjustment> {
        return database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered(accountId).executeAsList()
    }
}
