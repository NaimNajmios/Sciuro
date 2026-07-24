package com.sciuro.core.debt.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.repository.DebtRepository
import kotlinx.coroutines.flow.first

class BnplRiskDetector(
    private val debtRepository: DebtRepository,
    private val eventBus: DomainEventBus
) {
    suspend fun evaluate() {
        val debts = debtRepository.observeDebts().first()
        val activeCreditDebts = debts.filter { it.status == DebtStatus.ACTIVE }.count { debt ->
            val name = debt.name.lowercase()
            debt.type.name == "CREDIT_CARD" ||
            name.contains("bnpl") ||
            name.contains("paylater") ||
            name.contains("pay later") ||
            name.contains("atome") ||
            name.contains("hoolah") ||
            name.contains("split")
        }

        if (activeCreditDebts >= 2) {
            eventBus.publish(DomainEvent.BnplRiskThresholdCrossed(activeCreditDebts))
        }
    }
}
