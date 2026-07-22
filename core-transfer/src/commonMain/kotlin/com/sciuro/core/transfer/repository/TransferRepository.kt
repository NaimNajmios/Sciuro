package com.sciuro.core.transfer.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.transfer.model.TransferLink

class TransferRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val transactionRepository: TransactionRepository
) : AuditableRepository(auditRepository) {

    suspend fun getTransferForTransaction(transactionId: String): TransferLink? {
        return database.transferLinkQueries.selectTransferLinkByTransactionId(transactionId)
            .executeAsOneOrNull()
            ?.let { entity ->
                TransferLink(
                    id = entity.id,
                    outflowTransactionId = entity.outflow_transaction_id,
                    inflowTransactionId = entity.inflow_transaction_id,
                    amount = entity.amount,
                    createdAt = entity.created_at
                )
            }
    }

    suspend fun linkTransactions(transferLink: TransferLink): TransferLink {
        return withAudit(
            entityType = EntityType.TRANSFER_LINK,
            entityId = transferLink.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = transferLink.toString(),
            source = AuditSource.SYSTEM_AUTO
        ) {
            val now = currentTimeMillis()
            database.transferLinkQueries.insertTransferLink(
                id = transferLink.id,
                outflow_transaction_id = transferLink.outflowTransactionId,
                inflow_transaction_id = transferLink.inflowTransactionId,
                amount = transferLink.amount,
                created_at = now
            )
            
            // Re-categorize both to "Transfer"
            transactionRepository.reviewTransaction(transferLink.outflowTransactionId, newCategoryId = "cat_transfer")
            transactionRepository.reviewTransaction(transferLink.inflowTransactionId, newCategoryId = "cat_transfer")
            
            // Auto-confirm the account pair so future heuristic matches auto-link
            val outflowTx = database.transactionRecordQueries.selectTransactionById(transferLink.outflowTransactionId).executeAsOneOrNull()
            val inflowTx = database.transactionRecordQueries.selectTransactionById(transferLink.inflowTransactionId).executeAsOneOrNull()
            if (outflowTx?.account_id != null && inflowTx?.account_id != null) {
                val accounts = listOfNotNull(outflowTx.account_id, inflowTx.account_id).sorted()
                if (accounts.size == 2) {
                    database.accountQueries.insertAccountPairConfirmation(accounts[0], accounts[1], now)
                }
            }
            
            transferLink
        }
    }
}
