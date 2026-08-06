package com.sciuro.core.transfer.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.transfer.model.TransferLink

class TransferRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
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

    suspend fun linkTransactions(transferLink: TransferLink, isManualConfirmation: Boolean = false): TransferLink {
        val now = currentTimeMillis()

        database.transaction {
            database.transferLinkQueries.insertTransferLink(
                id = transferLink.id,
                outflow_transaction_id = transferLink.outflowTransactionId,
                inflow_transaction_id = transferLink.inflowTransactionId,
                amount = transferLink.amount,
                created_at = now
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.TRANSFER_LINK,
                    entityId = transferLink.id,
                    action = AuditAction.CREATE,
                    beforeState = null,
                    afterState = transferLink.toString(),
                    source = AuditSource.SYSTEM_AUTO,
                    confidence = null,
                    timestamp = now
                )
            )
        }

        // Re-categorize both to "Transfer"
        transactionRepository.reviewTransaction(transferLink.outflowTransactionId, newCategoryId = "cat_transfer")
        transactionRepository.reviewTransaction(transferLink.inflowTransactionId, newCategoryId = "cat_transfer")

        // Auto-confirm the account pair so future heuristic matches auto-link.
        // Manual confirmations (user-initiated) instead accumulate a count and
        // only promote the pair to account_pair_confirmation after 3 such
        // confirmations. Automatic links keep the previous immediate behavior.
        val outflowTx = database.transactionRecordQueries.selectTransactionById(transferLink.outflowTransactionId).executeAsOneOrNull()
        val inflowTx = database.transactionRecordQueries.selectTransactionById(transferLink.inflowTransactionId).executeAsOneOrNull()
        if (outflowTx?.account_id != null && inflowTx?.account_id != null) {
            val accounts = listOfNotNull(outflowTx.account_id, inflowTx.account_id).sorted()
            if (accounts.size == 2) {
                if (isManualConfirmation) {
                    accountRepository.recordManualConfirmation(accounts[0], accounts[1])
                } else {
                    database.accountQueries.insertAccountPairConfirmation(accounts[0], accounts[1], now)
                }
            }
        }

        return transferLink
    }

    /**
     * Links an unconfirmed transfer candidate pair, resolving outflow/inflow
     * ordering and rejecting already-linked transactions. Records the link as
     * a manual confirmation, accumulating toward the account-pair threshold.
     * Returns null when the pair cannot be linked.
     */
    suspend fun linkCandidatePair(transactionIdA: String, transactionIdB: String): TransferLink? {
        val txA = database.transactionRecordQueries.selectTransactionById(transactionIdA).executeAsOneOrNull() ?: return null
        val txB = database.transactionRecordQueries.selectTransactionById(transactionIdB).executeAsOneOrNull() ?: return null

        val outflowTxId: String
        val inflowTxId: String
        when {
            txA.direction == "OUTFLOW" && txB.direction == "INFLOW" -> {
                outflowTxId = txA.id
                inflowTxId = txB.id
            }
            txA.direction == "INFLOW" && txB.direction == "OUTFLOW" -> {
                outflowTxId = txB.id
                inflowTxId = txA.id
            }
            else -> return null
        }

        val alreadyLinkedOutflow = database.transferLinkQueries.selectTransferLinkByTransactionId(outflowTxId).executeAsOneOrNull()
        val alreadyLinkedInflow = database.transferLinkQueries.selectTransferLinkByTransactionId(inflowTxId).executeAsOneOrNull()
        if (alreadyLinkedOutflow != null || alreadyLinkedInflow != null) return null

        val link = TransferLink(
            id = generateUuid(),
            outflowTransactionId = outflowTxId,
            inflowTransactionId = inflowTxId,
            amount = txA.amount,
            createdAt = currentTimeMillis()
        )
        return linkTransactions(link, isManualConfirmation = true)
    }
}
