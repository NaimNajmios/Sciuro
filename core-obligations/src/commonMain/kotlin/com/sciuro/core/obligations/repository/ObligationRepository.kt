package com.sciuro.core.obligations.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.model.ObligationFrequency
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObligationRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    private fun mapObligation(it: com.sciuro.core.ledger.db.Obligation): Obligation {
        return Obligation(
            id = it.id,
            name = it.name,
            amount = it.amount,
            frequency = try { ObligationFrequency.valueOf(it.frequency) } catch (_: Exception) { ObligationFrequency.MONTHLY },
            nextDueDate = it.next_due_date,
            categoryId = it.category_id,
            accountId = it.account_id,
            isActive = it.is_active == 1L,
            lastPaidDate = it.last_paid_date
        )
    }

    suspend fun createObligation(obligation: Obligation): Obligation {
        val now = currentTimeMillis()
        return database.transactionWithResult {
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
                updated_at = now,
                last_paid_date = obligation.lastPaidDate
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = obligation.id,
                    action = AuditAction.CREATE,
                    beforeState = null,
                    afterState = obligation.toString(),
                    source = AuditSource.SYSTEM_AUTO,
                    confidence = null,
                    timestamp = now
                )
            )

            obligation
        }
    }

    suspend fun updateObligation(obligation: Obligation): Obligation {
        val now = currentTimeMillis()
        return database.transactionWithResult {
            database.obligationQueries.updateObligation(
                name = obligation.name,
                amount = obligation.amount,
                frequency = obligation.frequency.name,
                next_due_date = obligation.nextDueDate,
                category_id = obligation.categoryId,
                account_id = obligation.accountId,
                updated_at = now,
                id = obligation.id
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = obligation.id,
                    action = AuditAction.UPDATE,
                    beforeState = null,
                    afterState = obligation.toString(),
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )

            obligation
        }
    }

    suspend fun deleteObligation(id: String) {
        val now = currentTimeMillis()
        database.transaction {
            database.obligationQueries.deleteObligation(id)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = id,
                    action = AuditAction.DELETE,
                    beforeState = null,
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun deactivateObligation(id: String) {
        val now = currentTimeMillis()
        database.transaction {
            database.obligationQueries.deactivateObligation(now, id)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = id,
                    action = AuditAction.UPDATE,
                    beforeState = null,
                    afterState = "is_active: false",
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun advanceNextDueDate(id: String, newDueDate: Long) {
        val now = currentTimeMillis()
        database.transaction {
            database.obligationQueries.updateNextDueDate(newDueDate, now, id)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = id,
                    action = AuditAction.UPDATE,
                    beforeState = null,
                    afterState = "next_due_date: $newDueDate",
                    source = AuditSource.SYSTEM_AUTO,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun recordPayment(id: String, newDueDate: Long) {
        val now = currentTimeMillis()
        database.transaction {
            database.obligationQueries.recordPayment(newDueDate, now, now, id)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.RECURRING_OBLIGATION,
                    entityId = id,
                    action = AuditAction.UPDATE,
                    beforeState = null,
                    afterState = "next_due_date: $newDueDate, last_paid_date: $now",
                    source = AuditSource.SYSTEM_AUTO,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    fun observeActiveObligations(): Flow<List<Obligation>> {
        return database.obligationQueries.selectAllActiveObligations()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { mapObligation(it) } }
    }

    fun observeAllObligations(): Flow<List<Obligation>> {
        return database.obligationQueries.selectAllObligations()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { mapObligation(it) } }
    }
}
