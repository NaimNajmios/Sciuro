package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
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
    private val eventBus: DomainEventBus
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
        remark: String? = null,
        source: AuditSource = AuditSource.USER_MANUAL
    ): com.sciuro.core.ledger.db.Cash_adjustment {
        val adjustmentId = generateUuid()
        val now = currentTimeMillis()

        val created = database.transactionWithResult {
            database.cashAdjustmentQueries.insertAdjustment(
                id = adjustmentId,
                account_id = accountId,
                amount = amount,
                reason = reason,
                remark = remark,
                timestamp = now,
                created_at = now
            )

            database.accountQueries.updateBalance(
                balance = amount,
                updated_at = now,
                id = accountId
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.CASH_ADJUSTMENT,
                    entityId = adjustmentId,
                    action = AuditAction.CREATE,
                    beforeState = null,
                    afterState = "account=$accountId, amount=$amount, reason=$reason${remark?.let { ", remark=$it" } ?: ""}",
                    source = source,
                    confidence = null,
                    timestamp = now
                )
            )

            database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered(accountId).executeAsList().firstOrNull()
                ?: throw IllegalStateException("Adjustment not found after insert")
        }

        eventBus.publish(DomainEvent.CashRecounted(
            adjustmentId = adjustmentId,
            variance = amount,
            adjustmentType = reason
        ))

        return created
    }

    suspend fun deleteAdjustment(adjustmentId: String) {
        val adjustment = database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered("").executeAsList()
            .find { it.id == adjustmentId } ?: return

        database.transaction {
            database.accountQueries.updateBalance(
                balance = -adjustment.amount,
                updated_at = currentTimeMillis(),
                id = adjustment.account_id
            )
            database.cashAdjustmentQueries.deleteAdjustment(adjustmentId)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.CASH_ADJUSTMENT,
                    entityId = adjustmentId,
                    action = AuditAction.DELETE,
                    beforeState = "account=${adjustment.account_id}, amount=${adjustment.amount}, reason=${adjustment.reason}",
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = currentTimeMillis()
                )
            )
        }
    }

    suspend fun getAdjustmentCountForAccount(accountId: String): Long {
        return database.cashAdjustmentQueries.selectAdjustmentCountByAccount(accountId).executeAsOne()
    }

    fun getAdjustmentsForAccountSync(accountId: String): List<com.sciuro.core.ledger.db.Cash_adjustment> {
        return database.cashAdjustmentQueries.selectAdjustmentsByAccountOrdered(accountId).executeAsList()
    }
}
