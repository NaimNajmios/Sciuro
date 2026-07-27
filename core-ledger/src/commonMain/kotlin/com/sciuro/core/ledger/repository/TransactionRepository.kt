package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val accountRepository: AccountRepository,
    private val eventBus: DomainEventBus
) : AuditableRepository(auditRepository) {

    suspend fun bookTransaction(
        transaction: Transaction,
        source: AuditSource = AuditSource.SYSTEM_AUTO,
        confidence: Float? = null
    ): Transaction {
        val now = currentTimeMillis()

        database.transaction {
            database.transactionRecordQueries.insertTransaction(
                id = transaction.id,
                account_id = transaction.accountId,
                category_id = transaction.categoryId,
                amount = transaction.amount,
                direction = transaction.direction,
                merchant = transaction.merchant,
                timestamp = transaction.timestamp,
                reference_id = transaction.referenceId,
                is_reviewed = if (transaction.isReviewed) 1L else 0L,
                extraction_method = transaction.extractionMethod,
                confidence = transaction.confidence?.toDouble(),
                raw_event_id = transaction.rawEventId,
                review_tier = transaction.reviewTier,
                auto_confirmed_at = transaction.autoConfirmedAt,
                created_at = now,
                updated_at = now
            )

            if (transaction.accountId != null) {
                val balanceDelta = if (transaction.direction == "INFLOW") transaction.amount else -transaction.amount
                database.accountQueries.updateBalance(
                    balance = balanceDelta,
                    updated_at = now,
                    id = transaction.accountId
                )
            }
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transaction.id,
                action = AuditAction.CREATE,
                beforeState = null,
                afterState = transaction.toString(),
                source = source,
                confidence = confidence,
                timestamp = now
            )
        )

        eventBus.publish(DomainEvent.TransactionModified(transaction.id))
        return transaction
    }

    suspend fun reviewTransaction(
        transactionId: String,
        newCategoryId: String?,
        newAccountId: String? = null,
        newDirection: String? = null
    ) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()
        val targetAccountId = newAccountId ?: oldTx.account_id
        val finalDirection = newDirection ?: oldTx.direction

        database.transaction {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                database.accountQueries.updateBalance(balance = oldBalanceDelta, updated_at = now, id = oldTx.account_id)
            }

            if (targetAccountId != null) {
                val newBalanceDelta = if (finalDirection == "INFLOW") oldTx.amount else -oldTx.amount
                database.accountQueries.updateBalance(balance = newBalanceDelta, updated_at = now, id = targetAccountId)
            }

            database.transactionRecordQueries.updateTransactionDetails(
                amount = oldTx.amount,
                direction = finalDirection,
                merchant = oldTx.merchant,
                category_id = newCategoryId ?: oldTx.category_id,
                account_id = targetAccountId,
                updated_at = now,
                id = transactionId
            )

            database.transactionRecordQueries.markAsReviewed(now, transactionId)
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.RECLASSIFY,
                beforeState = "is_reviewed=0, category_id=${oldTx.category_id}, account=${oldTx.account_id}",
                afterState = "is_reviewed=1, category_id=$newCategoryId, account=$newAccountId",
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        val learnedCategoryId = newCategoryId ?: oldTx.category_id
        if (learnedCategoryId != null && oldTx.category_id != learnedCategoryId) {
            eventBus.publish(
                DomainEvent.TransactionRecategorized(
                    transactionId = transactionId,
                    oldCategoryId = oldTx.category_id ?: "",
                    newCategoryId = learnedCategoryId,
                    merchant = oldTx.merchant,
                    accountId = targetAccountId
                )
            )
        }
        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    suspend fun approveTransaction(transactionId: String) {
        val tx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        database.transactionRecordQueries.markAsReviewed(now, transactionId)

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.UPDATE,
                beforeState = "is_reviewed=0",
                afterState = "is_reviewed=1",
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        if (tx.category_id != null) {
            eventBus.publish(
                DomainEvent.TransactionCategorized(
                    transactionId = transactionId,
                    categoryId = tx.category_id,
                    confidence = tx.confidence ?: 0.0,
                    source = "review",
                    merchant = tx.merchant
                )
            )
        }
        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    suspend fun rejectTransaction(transactionId: String) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        database.transaction {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                database.accountQueries.updateBalance(balance = oldBalanceDelta, updated_at = now, id = oldTx.account_id)
            }
            database.transactionRecordQueries.deleteTransaction(transactionId)
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.DELETE,
                beforeState = "Reject Transaction",
                afterState = null,
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    suspend fun deleteTransaction(transactionId: String) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        database.transaction {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                database.accountQueries.updateBalance(balance = oldBalanceDelta, updated_at = now, id = oldTx.account_id)
            }
            database.transactionRecordQueries.deleteTransaction(transactionId)
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.DELETE,
                beforeState = oldTx.toString(),
                afterState = null,
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    suspend fun editTransaction(
        transactionId: String,
        newAmount: Double,
        newDirection: String,
        newMerchant: String,
        newCategoryId: String?,
        newAccountId: String?
    ) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        database.transaction {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                database.accountQueries.updateBalance(balance = oldBalanceDelta, updated_at = now, id = oldTx.account_id)
            }

            if (newAccountId != null) {
                val newBalanceDelta = if (newDirection == "INFLOW") newAmount else -newAmount
                database.accountQueries.updateBalance(balance = newBalanceDelta, updated_at = now, id = newAccountId)
            }

            database.transactionRecordQueries.updateTransactionDetails(
                amount = newAmount,
                direction = newDirection,
                merchant = newMerchant,
                category_id = newCategoryId,
                account_id = newAccountId,
                updated_at = now,
                id = transactionId
            )
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.UPDATE,
                beforeState = oldTx.toString(),
                afterState = "amount=$newAmount, merchant=$newMerchant, category=$newCategoryId, account=$newAccountId",
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        if (newCategoryId != null && oldTx.category_id != newCategoryId) {
            eventBus.publish(
                DomainEvent.TransactionRecategorized(
                    transactionId = transactionId,
                    oldCategoryId = oldTx.category_id ?: "",
                    newCategoryId = newCategoryId,
                    merchant = newMerchant.ifEmpty { oldTx.merchant }
                )
            )
        }
        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    fun observeUnreviewedTransactions(): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        // We use an arbitrary dispatcher since we might not have IO in commonMain
        return database.transactionRecordQueries.selectUnreviewedTransactions()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun observeAllTransactions(): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        return database.transactionRecordQueries.selectAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun observeTransactionsFilteredPaginated(start: Long?, end: Long?, direction: String?, limit: Long, offset: Long): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        return database.transactionRecordQueries.selectTransactionsFilteredPaginated(
            start = start,
            end = end,
            direction = direction,
            limit = limit,
            offset = offset
        ).asFlow().mapToList(Dispatchers.Default)
    }


    fun observeTransactionsForAccount(accountId: String): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        return database.transactionRecordQueries.selectTransactionsByAccount(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun findLikelyDuplicate(
        amount: Double,
        direction: String,
        timestamp: Long,
        windowMs: Long = 90_000
    ): com.sciuro.core.ledger.db.Transaction_record? {
        return database.transactionRecordQueries.findLikelyDuplicate(
            direction = direction,
            amount = amount,
            timestamp = timestamp,
            value_ = windowMs
        ).executeAsOneOrNull()
    }

    suspend fun attachCorroboratingSource(transactionId: String, rawEventId: String) {
        database.transactionCorroborationQueries.insertCorroboration(
            transaction_id = transactionId,
            raw_event_id = rawEventId,
            captured_at = currentTimeMillis()
        )
    }

    fun observeRecentlyAutoConfirmed(sinceMs: Long): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        return database.transactionRecordQueries.selectRecentlyAutoConfirmed(sinceMs)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun undoAutoConfirm(transactionId: String) {
        val tx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        database.transaction {
            if (tx.account_id != null) {
                val balanceDelta = if (tx.direction == "INFLOW") -tx.amount else tx.amount
                database.accountQueries.updateBalance(balance = balanceDelta, updated_at = now, id = tx.account_id)
            }

            database.transactionRecordQueries.undoAutoConfirm(now, transactionId)
        }

        auditRepository.logMutation(
            AuditLog(
                id = generateUuid(),
                entityType = EntityType.TRANSACTION,
                entityId = transactionId,
                action = AuditAction.UPDATE,
                beforeState = "review_tier=${tx.review_tier}, is_reviewed=${tx.is_reviewed}",
                afterState = "review_tier=MANUAL, is_reviewed=0",
                source = AuditSource.USER_MANUAL,
                confidence = null,
                timestamp = now
            )
        )

        eventBus.publish(DomainEvent.TransactionModified(transactionId))
    }

    suspend fun getAllTransactionsOnce(): List<com.sciuro.core.ledger.db.Transaction_record> {
        return database.transactionRecordQueries.selectAllTransactions().executeAsList()
    }

    suspend fun getTransactionById(transactionId: String): com.sciuro.core.ledger.db.Transaction_record? {
        return database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull()
    }

    suspend fun getTransactionsByAccount(accountId: String): List<com.sciuro.core.ledger.db.Transaction_record> {
        return database.transactionRecordQueries.selectTransactionsByAccount(accountId).executeAsList()
    }
}
