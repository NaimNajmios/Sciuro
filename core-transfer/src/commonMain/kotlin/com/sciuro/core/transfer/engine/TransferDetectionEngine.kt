package com.sciuro.core.transfer.engine

import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.transfer.model.TransferLink
import com.sciuro.core.transfer.repository.TransferRepository

class TransferDetectionEngine(
    private val database: SciuroDatabase,
    private val transferRepository: TransferRepository
) {
    suspend fun runDetection() {
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        
        val outflows = allTransactions.filter { it.direction == "OUTFLOW" }
        val inflows = allTransactions.filter { it.direction == "INFLOW" }
        
        // Ensure we don't re-link already linked transactions
        val existingLinks = database.transferLinkQueries.selectTransferLinkByTransactionId("dummy").executeAsList() // we'll fetch all to avoid DB hits in loop for now
        // A better approach for SQLite is just checking if id exists in link table, but in-memory is fast enough for a local client
        // Let's assume we maintain a list of all already linked IDs
        
        // We'll skip existing ones by checking in a naive way:
        // (In a real app we'd query all links, but let's just do an in-memory loop for B2)
        val linkedIds = mutableSetOf<String>()
        
        for (outflow in outflows) {
            if (linkedIds.contains(outflow.id)) continue
            
            // Look for matching inflow: same amount, within 2 minutes (120,000 ms)
            val match = inflows.find { inflow ->
                !linkedIds.contains(inflow.id) &&
                kotlin.math.abs(inflow.amount - outflow.amount) < 0.01 &&
                kotlin.math.abs(inflow.timestamp - outflow.timestamp) < 120_000
            }
            
            if (match != null) {
                // Check if they are already linked in the DB
                val alreadyLinkedOutflow = database.transferLinkQueries.selectTransferLinkByTransactionId(outflow.id).executeAsOneOrNull()
                val alreadyLinkedInflow = database.transferLinkQueries.selectTransferLinkByTransactionId(match.id).executeAsOneOrNull()
                
                if (alreadyLinkedOutflow == null && alreadyLinkedInflow == null) {
                    val link = TransferLink(
                        id = generateUuid(),
                        outflowTransactionId = outflow.id,
                        inflowTransactionId = match.id,
                        amount = outflow.amount,
                        createdAt = currentTimeMillis()
                    )
                    
                    transferRepository.linkTransactions(link)
                }
                
                linkedIds.add(outflow.id)
                linkedIds.add(match.id)
            }
        }
    }
}
