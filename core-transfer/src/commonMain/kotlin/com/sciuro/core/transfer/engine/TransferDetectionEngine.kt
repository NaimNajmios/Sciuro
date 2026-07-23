package com.sciuro.core.transfer.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.transfer.model.TransferLink
import com.sciuro.core.transfer.repository.TransferRepository

class TransferDetectionEngine(
    private val database: SciuroDatabase,
    private val transferRepository: TransferRepository,
    private val eventBus: DomainEventBus
) {
    companion object {
        const val TIGHT_MATCH_WINDOW_MS = 15_000L
        const val HEURISTIC_WINDOW_MS = 120_000L
    }

    suspend fun onTransactionBooked(
        newTxId: String,
        newTxAccountId: String?,
        newTxAmount: Double,
        newTxDirection: String,
        newTxTimestamp: Long,
        counterpartyAccountNumber: String?
    ) {
        if (counterpartyAccountNumber != null) {
            val ownAccounts = database.accountQueries.selectAllAccounts().executeAsList()
            val matchingOwnAccount = ownAccounts.firstOrNull { ownAccount ->
                val num = ownAccount.account_number ?: return@firstOrNull false
                matchesAccountSuffix(counterpartyAccountNumber, num)
            }

            if (matchingOwnAccount != null) {
                val otherLeg = findUnlinkedMatchingLeg(
                    newTxId = newTxId,
                    otherAccountId = matchingOwnAccount.id,
                    amount = newTxAmount,
                    direction = newTxDirection,
                    timestamp = newTxTimestamp
                )
                if (otherLeg != null) {
                    linkAsTransfer(newTxId, otherLeg.id, newTxAmount)
                }
                return
            }
            return
        }

        val tightMatch = findTightUnconfirmedMatch(
            newTxId = newTxId,
            amount = newTxAmount,
            direction = newTxDirection,
            timestamp = newTxTimestamp
        )
        if (tightMatch != null) {
            linkAsTransfer(newTxId, tightMatch.id, newTxAmount)
            return
        }

        val match = findHeuristicMatch(
            newTxId = newTxId,
            amount = newTxAmount,
            direction = newTxDirection,
            timestamp = newTxTimestamp
        )
        if (match != null) {
            val matchAccountId = match.account_id
            val pairConfirmed = newTxAccountId != null && matchAccountId != null &&
                isPairConfirmed(newTxAccountId, matchAccountId)
            if (pairConfirmed) {
                linkAsTransfer(newTxId, match.id, newTxAmount)
            } else {
                eventBus.publish(
                    DomainEvent.TransferUnmatchedFlagged(
                        transactionId = newTxId,
                        candidateRecipient = match.merchant ?: match.id
                    )
                )
            }
        }
    }

    private suspend fun findUnlinkedMatchingLeg(
        newTxId: String,
        otherAccountId: String,
        amount: Double,
        direction: String,
        timestamp: Long
    ): com.sciuro.core.ledger.db.Transaction_record? {
        val oppositeDirection = if (direction == "OUTFLOW") "INFLOW" else "OUTFLOW"
        val candidates = database.transactionRecordQueries.selectTransactionsByAccount(otherAccountId)
            .executeAsList()
            .filter { tx ->
                tx.id != newTxId &&
                tx.direction == oppositeDirection &&
                kotlin.math.abs(tx.amount - amount) < 0.01 &&
                !isTransactionLinked(tx.id)
            }
        return candidates.minByOrNull { kotlin.math.abs(it.timestamp - timestamp) }
    }

    private suspend fun findTightUnconfirmedMatch(
        newTxId: String,
        amount: Double,
        direction: String,
        timestamp: Long
    ): com.sciuro.core.ledger.db.Transaction_record? {
        val oppositeDirection = if (direction == "OUTFLOW") "INFLOW" else "OUTFLOW"
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()

        return allTransactions.firstOrNull { tx ->
            tx.id != newTxId &&
            tx.direction == oppositeDirection &&
            !isTransactionLinked(tx.id) &&
            kotlin.math.abs(tx.amount - amount) < 0.01 &&
            kotlin.math.abs(tx.timestamp - timestamp) < TIGHT_MATCH_WINDOW_MS
        }
    }

    private suspend fun findHeuristicMatch(
        newTxId: String,
        amount: Double,
        direction: String,
        timestamp: Long
    ): com.sciuro.core.ledger.db.Transaction_record? {
        val oppositeDirection = if (direction == "OUTFLOW") "INFLOW" else "OUTFLOW"
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()

        return allTransactions.firstOrNull { tx ->
            tx.id != newTxId &&
            tx.direction == oppositeDirection &&
            !isTransactionLinked(tx.id) &&
            kotlin.math.abs(tx.amount - amount) < 0.01 &&
            kotlin.math.abs(tx.timestamp - timestamp) < HEURISTIC_WINDOW_MS
        }
    }

    private suspend fun isTransactionLinked(transactionId: String): Boolean {
        return database.transferLinkQueries.selectTransferLinkByTransactionId(transactionId)
            .executeAsOneOrNull() != null
    }

    private suspend fun isPairConfirmed(accountIdA: String, accountIdB: String): Boolean {
        val sorted = listOf(accountIdA, accountIdB).sorted()
        return database.accountQueries.selectAccountPairConfirmation(sorted[0], sorted[1])
            .executeAsOneOrNull() != null
    }

    private suspend fun linkAsTransfer(txIdA: String, txIdB: String, amount: Double) {
        val outflowTxId: String
        val inflowTxId: String
        val txA = database.transactionRecordQueries.selectTransactionById(txIdA).executeAsOneOrNull()
        val txB = database.transactionRecordQueries.selectTransactionById(txIdB).executeAsOneOrNull()
        if (txA == null || txB == null) return

        if (txA.direction == "OUTFLOW" && txB.direction == "INFLOW") {
            outflowTxId = txIdA; inflowTxId = txIdB
        } else if (txA.direction == "INFLOW" && txB.direction == "OUTFLOW") {
            outflowTxId = txIdB; inflowTxId = txIdA
        } else {
            return
        }

        val alreadyLinkedOutflow = database.transferLinkQueries.selectTransferLinkByTransactionId(outflowTxId).executeAsOneOrNull()
        val alreadyLinkedInflow = database.transferLinkQueries.selectTransferLinkByTransactionId(inflowTxId).executeAsOneOrNull()
        if (alreadyLinkedOutflow != null || alreadyLinkedInflow != null) return

        val link = TransferLink(
            id = generateUuid(),
            outflowTransactionId = outflowTxId,
            inflowTransactionId = inflowTxId,
            amount = amount,
            createdAt = currentTimeMillis()
        )

        transferRepository.linkTransactions(link)
        eventBus.publish(
            DomainEvent.TransferMatched(
                transferLinkId = link.id,
                sourceTxId = outflowTxId,
                destTxId = inflowTxId,
                matchMethod = "IDENTITY"
            )
        )
    }

    private fun matchesAccountSuffix(extracted: String, stored: String): Boolean {
        val normalizedExtracted = extracted.filter { it.isDigit() }
        val normalizedStored = stored.filter { it.isDigit() }
        if (normalizedExtracted.isEmpty() || normalizedStored.isEmpty()) return false
        val len = minOf(normalizedExtracted.length, normalizedStored.length)
        return normalizedExtracted.takeLast(len) == normalizedStored.takeLast(len)
    }
}
