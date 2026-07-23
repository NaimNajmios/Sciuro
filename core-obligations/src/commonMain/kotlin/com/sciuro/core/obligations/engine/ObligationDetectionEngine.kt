package com.sciuro.core.obligations.engine

import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.model.Obligation
import com.sciuro.core.obligations.model.ObligationFrequency
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent

class ObligationDetectionEngine(
    private val database: SciuroDatabase,
    private val obligationRepository: ObligationRepository,
    private val eventBus: DomainEventBus,
    private val settingsProvider: SettingsProvider
) {
    suspend fun runDetection() {
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        val confidenceTracker = ConfidenceTracker(database)
        val autoConfirmEnabled = settingsProvider.isAutoConfirmEnabled()
        val autoConfirmThreshold = settingsProvider.getAutoConfirmThreshold()

        val byMerchant = allTransactions.filter { it.merchant != null }.groupBy { it.merchant!! }

        for ((merchant, txs) in byMerchant) {
            if (txs.size < 3) continue
            val outflows = txs.filter { it.direction == "OUTFLOW" }
            if (outflows.size < 3) continue

            val firstAmount = outflows.first().amount
            val allSimilar = outflows.all { kotlin.math.abs(it.amount - firstAmount) < 2.0 }
            if (!allSimilar) continue

            val existing = database.obligationQueries.selectAllActiveObligations().executeAsList()
                .any { it.name.contains(merchant, ignoreCase = true) }
            if (existing) continue

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

            val isTrusted = autoConfirmEnabled && confidenceTracker.isTrusted(merchant, autoConfirmThreshold)

            if (isTrusted) {
                obligationRepository.createObligation(newObligation)
                eventBus.publish(DomainEvent.ObligationCreated(newObligation.id))
                eventBus.publish(DomainEvent.RecurringObligationConfirmed(newObligation.id))
            } else {
                obligationRepository.createObligation(newObligation)
                eventBus.publish(DomainEvent.RecurringObligationProposed(newObligation.id, 0.5))
            }
        }
    }
}
