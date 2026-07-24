package com.najmi.sciuro.engine

import android.content.Context
import com.najmi.sciuro.worker.NotificationHelper
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
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
    private val suppressionEngine: NotificationSuppressionEngine
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
        NotificationHelper.showBudgetAlert(context, event.transactionId, 0.0)
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
        NotificationHelper.showBudgetAlert(
            context, "bnpl_risk",
            event.activeBnplCount.toDouble() / 10.0
        )
    }

    private suspend fun handleCashRecounted(event: DomainEvent.CashRecounted) {
        if (suppressionEngine.shouldSuppress(event)) return
        val varianceStr = "RM %.2f".format(event.variance)
        NotificationHelper.showBudgetAlert(
            context, event.adjustmentId,
            event.variance / 1000.0
        )
    }
}
