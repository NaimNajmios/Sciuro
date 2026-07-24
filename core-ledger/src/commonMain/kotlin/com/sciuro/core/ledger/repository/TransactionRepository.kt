package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val accountRepository: AccountRepository, // for atomic balance updates
    private val eventBus: DomainEventBus
) : AuditableRepository(auditRepository) {

    suspend fun bookTransaction(
        transaction: Transaction,
        source: AuditSource = AuditSource.SYSTEM_AUTO,
        confidence: Float? = null
    ): Transaction {
        return withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transaction.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = transaction.toString(),
            source = source,
            confidence = confidence
        ) {
            val now = currentTimeMillis()
            
            // 1. Insert transaction
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
                created_at = now,
                updated_at = now
            )
            
            // 2. Adjust account balance
            if (transaction.accountId != null) {
                val balanceDelta = if (transaction.direction == "INFLOW") transaction.amount else -transaction.amount
                accountRepository.updateBalance(transaction.accountId, balanceDelta)
            }
            
            transaction
        }
    }
    
    suspend fun reviewTransaction(transactionId: String, newCategoryId: String?, newAccountId: String? = null, newDirection: String? = null) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        
        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.RECLASSIFY,
            beforeState = "is_reviewed=0, category_id=${oldTx.category_id}, account=${oldTx.account_id}",
            afterState = "is_reviewed=1, category_id=$newCategoryId, account=$newAccountId",
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            
            // Reverse old balance if it was assigned
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                accountRepository.updateBalance(oldTx.account_id, oldBalanceDelta)
            }
            
            val targetAccountId = newAccountId ?: oldTx.account_id
            val finalDirection = newDirection ?: oldTx.direction
            
            // Apply new balance if there's a target account
            if (targetAccountId != null) {
                val newBalanceDelta = if (finalDirection == "INFLOW") oldTx.amount else -oldTx.amount
                accountRepository.updateBalance(targetAccountId, newBalanceDelta)
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

            val learnedCategoryId = newCategoryId ?: oldTx.category_id
            if (learnedCategoryId != null && oldTx.category_id != learnedCategoryId) {
                eventBus.publish(
                    DomainEvent.TransactionRecategorized(
                        transactionId = transactionId,
                        oldCategoryId = oldTx.category_id ?: "",
                        newCategoryId = learnedCategoryId,
                        merchant = oldTx.merchant
                    )
                )
            }
        }
    }

    suspend fun approveTransaction(transactionId: String) {
        val tx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return

        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.UPDATE,
            beforeState = "is_reviewed=0",
            afterState = "is_reviewed=1",
            source = AuditSource.USER_MANUAL
        ) {
            database.transactionRecordQueries.markAsReviewed(currentTimeMillis(), transactionId)
        }

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
    }

    suspend fun rejectTransaction(transactionId: String) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        
        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.DELETE,
            beforeState = "Reject Transaction",
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                accountRepository.updateBalance(oldTx.account_id, oldBalanceDelta)
            }
            database.transactionRecordQueries.deleteTransaction(transactionId)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        
        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.DELETE,
            beforeState = oldTx.toString(),
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                accountRepository.updateBalance(oldTx.account_id, oldBalanceDelta)
            }
            database.transactionRecordQueries.deleteTransaction(transactionId)
        }
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
        
        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.UPDATE,
            beforeState = oldTx.toString(),
            afterState = "amount=$newAmount, merchant=$newMerchant, category=$newCategoryId, account=$newAccountId",
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            
            // Revert old balance
            if (oldTx.account_id != null) {
                val oldBalanceDelta = if (oldTx.direction == "INFLOW") -oldTx.amount else oldTx.amount
                accountRepository.updateBalance(oldTx.account_id, oldBalanceDelta)
            }
            
            // Apply new balance
            if (newAccountId != null) {
                val newBalanceDelta = if (newDirection == "INFLOW") newAmount else -newAmount
                accountRepository.updateBalance(newAccountId, newBalanceDelta)
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
        }
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
}
