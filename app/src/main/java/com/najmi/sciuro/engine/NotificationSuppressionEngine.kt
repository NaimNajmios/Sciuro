package com.najmi.sciuro.engine

import android.content.Context
import com.najmi.sciuro.worker.NotificationHelper
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.obligations.engine.IncomeRecurrencePatternDetector
import com.sciuro.core.obligations.repository.ObligationRepository
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.audit.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationSuppressionEngine(
    private val context: Context,
    private val eventBus: DomainEventBus,
    private val settingsProvider: SettingsProvider,
    private val accountRepository: AccountRepository,
    private val obligationRepository: ObligationRepository,
    private val debtRepository: DebtRepository,
    private val incomeDetector: IncomeRecurrencePatternDetector
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.BudgetThresholdCrossed -> {
                        if (!shouldSuppressBudget(event)) {
                            NotificationHelper.showBudgetAlert(
                                context,
                                event.categoryId,
                                event.percentUsed
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun shouldSuppressBudget(@Suppress("UNUSED_PARAMETER") event: DomainEvent.BudgetThresholdCrossed): Boolean {
        if (isInQuietHours()) return true
        if (isRunwayCritical()) return false
        return false
    }

    private fun isInQuietHours(): Boolean {
        if (!settingsProvider.isQuietHoursEnabled()) return false

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val start = settingsProvider.getQuietHoursStart()
        val end = settingsProvider.getQuietHoursEnd()

        return if (start <= end) {
            currentHour >= start && currentHour < end
        } else {
            currentHour >= start || currentHour < end
        }
    }

    private suspend fun isRunwayCritical(): Boolean {
        val accounts = accountRepository.observeAccounts().first()
        val totalAccounts = accounts.sumOf { it.balance }

        val obligations = obligationRepository.observeActiveObligations().first()
        val incomePattern = incomeDetector.detectNextIncome()
        val nextIncome = incomePattern?.nextExpectedDate ?: (currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        val expectedIncome = incomePattern?.amount ?: 0.0

        val obligationsDue = obligations.filter {
            it.nextDueDate <= nextIncome
        }.sumOf { it.amount }

        val debts = debtRepository.observeDebts().first()
        val debtsDue = debts.filter { debt ->
            debt.direction == DebtDirection.I_OWE && debt.dueDate != null && debt.dueDate!! <= nextIncome
        }.sumOf { it.remainingBalance.toDouble() }

        val runway = totalAccounts + expectedIncome - obligationsDue - debtsDue
        return runway < 0
    }
}
