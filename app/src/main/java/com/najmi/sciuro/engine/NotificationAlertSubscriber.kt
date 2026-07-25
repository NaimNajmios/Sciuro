package com.najmi.sciuro.engine

import android.content.Context
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.worker.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationAlertSubscriber(
    private val eventBus: DomainEventBus,
    private val context: Context
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.BudgetThresholdCrossed -> {
                        NotificationHelper.showBudgetAlert(
                            context = context,
                            categoryId = event.categoryId,
                            percentUsed = event.percentUsed
                        )
                    }
                    is DomainEvent.DebtFullyPaidOff -> {
                        NotificationHelper.showDebtAlert(
                            context = context,
                            debtId = event.debtId,
                            debtName = "Debt",
                            message = "Congratulations! This debt has been fully paid off."
                        )
                    }
                    is DomainEvent.DebtBalanceUpdated -> {
                        if (event.newBalance <= 0.0) {
                            NotificationHelper.showDebtAlert(
                                context = context,
                                debtId = event.debtId,
                                debtName = "Debt",
                                message = "This debt has been paid in full."
                            )
                        }
                    }
                    is DomainEvent.ObligationCreated -> {
                        NotificationHelper.showObligationAlert(
                            context = context,
                            obligationId = event.obligationId,
                            name = "New Obligation",
                            message = "A new recurring obligation has been detected."
                        )
                    }
                    is DomainEvent.RecurringObligationConfirmed -> {
                        NotificationHelper.showObligationAlert(
                            context = context,
                            obligationId = event.obligationId,
                            name = "Obligation Confirmed",
                            message = "A recurring obligation has been auto-confirmed."
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
