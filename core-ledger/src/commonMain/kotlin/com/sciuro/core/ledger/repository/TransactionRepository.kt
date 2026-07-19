package com.sciuro.core.ledger.repository

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
    private val accountRepository: AccountRepository // for atomic balance updates
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
    
    suspend fun reviewTransaction(transactionId: String, newCategoryId: String?, newAccountId: String? = null) {
        val oldTx = database.transactionRecordQueries.selectTransactionById(transactionId).executeAsOneOrNull() ?: return
        
        withAudit(
            entityType = EntityType.TRANSACTION,
            entityId = transactionId,
            action = AuditAction.RECLASSIFY,
            beforeState = "is_reviewed=0, account=${oldTx.account_id}",
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
            
            // Apply new balance if there's a target account
            if (targetAccountId != null) {
                val newBalanceDelta = if (oldTx.direction == "INFLOW") oldTx.amount else -oldTx.amount
                accountRepository.updateBalance(targetAccountId, newBalanceDelta)
            }
            
            if (newCategoryId != null && targetAccountId != null) {
                database.transactionRecordQueries.updateAccountAndCategory(targetAccountId, newCategoryId, now, transactionId)
            } else if (newCategoryId != null) {
                database.transactionRecordQueries.updateCategory(newCategoryId, now, transactionId)
            }
            
            database.transactionRecordQueries.markAsReviewed(now, transactionId)
        }
    }

    fun observeUnreviewedTransactions(): Flow<List<com.sciuro.core.ledger.db.Transaction_record>> {
        // We use an arbitrary dispatcher since we might not have IO in commonMain
        return database.transactionRecordQueries.selectUnreviewedTransactions()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
}
