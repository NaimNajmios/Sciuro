package com.sciuro.core.obligations.engine

import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.model.ObligationFrequency
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent

class ObligationDetectionEngine(
    private val database: SciuroDatabase,
    private val obligationRepository: ObligationRepository,
    private val eventBus: DomainEventBus
) {
    /**
     * Scans recent transactions to detect recurring patterns.
     * Naive implementation: Groups transactions by merchant and amount.
     * If 3 transactions exist for the same merchant/amount roughly 30 days apart, it's a MONTHLY obligation.
     */
    suspend fun runDetection() {
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        
        // 1. Group by merchant
        val byMerchant = allTransactions.filter { it.merchant != null }.groupBy { it.merchant!! }
        
        for ((merchant, txs) in byMerchant) {
            if (txs.size >= 3) {
                // Check if they are OUTFLOW
                val outflows = txs.filter { it.direction == "OUTFLOW" }
                if (outflows.size >= 3) {
                    // Very basic check: are the amounts similar?
                    val firstAmount = outflows.first().amount
                    val allSimilar = outflows.all { kotlin.math.abs(it.amount - firstAmount) < 2.0 }
                    
                    if (allSimilar) {
                        // Check if an obligation already exists for this merchant
                        val existing = database.obligationQueries.selectAllActiveObligations().executeAsList()
                            .any { it.name.contains(merchant, ignoreCase = true) }
                            
                        if (!existing) {
                            // Assume monthly and next due date is +30 days from the most recent transaction
                            val mostRecent = outflows.maxByOrNull { it.timestamp }!!
                            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                            
                            val newObligation = Obligation(
                                id = generateUuid(),
                                name = "${merchant.replaceFirstChar { it.uppercase() }} Subscription",
                                amount = firstAmount,
                                frequency = ObligationFrequency.MONTHLY,
                                nextDueDate = mostRecent.timestamp + thirtyDaysMs,
                                categoryId = mostRecent.category_id,
                                accountId = mostRecent.account_id,
                                isActive = true
                            )
                            
                            obligationRepository.createObligation(newObligation)
                            eventBus.publish(DomainEvent.ObligationCreated(newObligation.id))
                        }
                    }
                }
            }
        }
    }
}
