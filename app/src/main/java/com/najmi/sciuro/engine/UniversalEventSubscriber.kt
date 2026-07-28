package com.najmi.sciuro.engine

import android.content.Context
import com.sciuro.feature.settings.config.NotificationPreferencesStore
import com.najmi.sciuro.worker.NotificationHelper
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.obligations.repository.ObligationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UniversalEventSubscriber(
    private val context: Context,
    private val eventBus: DomainEventBus,
    private val obligationRepository: ObligationRepository,
    private val suppressionEngine: NotificationSuppressionEngine,
    private val prefsStore: NotificationPreferencesStore,
    private val transactionRepository: TransactionRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.RecurringObligationProposed -> handleProposed(event)
                    is DomainEvent.RecurringObligationConfirmed -> handleConfirmed(event)
                    is DomainEvent.IncomeRecurrencePatternDetected -> handleIncomeDetected(event)
                    is DomainEvent.BudgetLimitSuggested -> handleBudgetSuggested(event)
                    is DomainEvent.TransferUnmatchedFlagged -> handleTransferFlagged(event)
                    is DomainEvent.ObligationCreated -> handleObligationCreated(event)
                    is DomainEvent.ObligationAmountDrifted -> handleAmountDrifted(event)
                    is DomainEvent.BnplRiskThresholdCrossed -> handleBnplRisk(event)
                    is DomainEvent.CashRecounted -> handleCashRecounted(event)
                    is DomainEvent.TransactionCategorized -> handleTransactionCategorized(event)
                    is DomainEvent.ObligationCycleSettled -> handleObligationCycleSettled(event)
                    is DomainEvent.NetPositionMilestoneReached -> handleMilestoneReached(event)
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleProposed(event: DomainEvent.RecurringObligationProposed) {
        if (suppressionEngine.shouldSuppress(event)) return
        val obligations = obligationRepository.observeActiveObligations().first()
        val obligation = obligations.find { it.id == event.obligationId } ?: return
        NotificationHelper.showBillReminder(
            context, obligation.id, obligation.name, obligation.nextDueDate
        )
    }

    private suspend fun handleConfirmed(event: DomainEvent.RecurringObligationConfirmed) {
        if (suppressionEngine.shouldSuppress(event)) return
        val obligations = obligationRepository.observeActiveObligations().first()
        val obligation = obligations.find { it.id == event.obligationId } ?: return
        NotificationHelper.showBillReminder(
            context, obligation.id,
            "${obligation.name} (auto-confirmed)",
            obligation.nextDueDate
        )
    }

    private suspend fun handleIncomeDetected(event: DomainEvent.IncomeRecurrencePatternDetected) {
        if (suppressionEngine.shouldSuppress(event)) return
        val amountStr = "RM %.0f".format(event.amount)
        NotificationHelper.showBillReminder(
            context, event.incomeStreamId,
            "Income pattern detected: $amountStr",
            event.expectedNextDate
        )
    }

    private suspend fun handleBudgetSuggested(event: DomainEvent.BudgetLimitSuggested) {
        if (suppressionEngine.shouldSuppress(event)) return
        NotificationHelper.showBudgetAlert(context, event.categoryId, 0.0)
    }

    private suspend fun handleTransferFlagged(event: DomainEvent.TransferUnmatchedFlagged) {
        if (suppressionEngine.shouldSuppress(event)) return
        if (!prefsStore.isEnabled(NotificationPreferencesStore.TRANSFER_REVIEW)) return
        NotificationHelper.showTransferReviewAlert(context, event.candidateRecipient)
    }

    private suspend fun handleObligationCreated(event: DomainEvent.ObligationCreated) {
        if (suppressionEngine.shouldSuppress(event)) return
        val obligations = obligationRepository.observeActiveObligations().first()
        val obligation = obligations.find { it.id == event.obligationId } ?: return
        NotificationHelper.showBillReminder(
            context, obligation.id, "New bill: ${obligation.name}", obligation.nextDueDate
        )
    }

    private suspend fun handleAmountDrifted(event: DomainEvent.ObligationAmountDrifted) {
        if (suppressionEngine.shouldSuppress(event)) return
        val obligations = obligationRepository.observeActiveObligations().first()
        val obligation = obligations.find { it.id == event.obligationId } ?: return
        val oldStr = "RM %.2f".format(event.oldAmount)
        val newStr = "RM %.2f".format(event.newAmount)
        NotificationHelper.showBillReminder(
            context, obligation.id,
            "${obligation.name} amount changed: $oldStr \u2192 $newStr",
            obligation.nextDueDate
        )
    }

    private suspend fun handleBnplRisk(event: DomainEvent.BnplRiskThresholdCrossed) {
        if (suppressionEngine.shouldSuppress(event)) return
        if (!prefsStore.isEnabled(NotificationPreferencesStore.BNPL_RISK)) return
        NotificationHelper.showBnplAlert(context, event.activeBnplCount)
    }

    private suspend fun handleCashRecounted(event: DomainEvent.CashRecounted) {
        if (suppressionEngine.shouldSuppress(event)) return
        if (!prefsStore.isEnabled(NotificationPreferencesStore.CASH_ANOMALY)) return
        NotificationHelper.showCashAnomalyAlert(context, event.variance, event.adjustmentType)
    }

    private suspend fun handleTransactionCategorized(event: DomainEvent.TransactionCategorized) {
        if (!prefsStore.isEnabled(NotificationPreferencesStore.LARGE_TXN)) return
        val threshold = prefsStore.getDouble(
            NotificationPreferencesStore.LARGE_TXN, "threshold", 500.0
        )
        val transactions = transactionRepository.observeAllTransactions().first()
        val tx = transactions.find { it.id == event.transactionId } ?: return
        if (tx.direction == "OUTFLOW" && kotlin.math.abs(tx.amount) >= threshold) {
            NotificationHelper.showLargeTransactionAlert(context, event.merchant, tx.amount)
        }
    }

    private suspend fun handleObligationCycleSettled(event: DomainEvent.ObligationCycleSettled) {
        if (!prefsStore.isEnabled(NotificationPreferencesStore.BILL_AUTOPAY)) return
        if (suppressionEngine.shouldSuppress(event)) return
        val obligations = obligationRepository.observeActiveObligations().first()
        val obligation = obligations.find { it.id == event.obligationId } ?: return
        NotificationHelper.showBillAutopayConfirmed(
            context, obligation.name, obligation.amount, obligation.nextDueDate
        )
    }

    private suspend fun handleMilestoneReached(event: DomainEvent.NetPositionMilestoneReached) {
        if (!prefsStore.isEnabled(NotificationPreferencesStore.MILESTONE)) return
        NotificationHelper.showNetPositionMilestone(context, event.milestone, event.netWorth)
    }
}
