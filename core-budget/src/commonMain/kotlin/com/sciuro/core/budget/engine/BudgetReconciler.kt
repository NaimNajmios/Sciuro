package com.sciuro.core.budget.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
class BudgetReconciler(
    private val eventBus: DomainEventBus,
    private val budgetEngine: BudgetEngine
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            eventBus.events
                .filter { it is DomainEvent.TransactionModified }
                .debounce(300)
                .collect { budgetEngine.processBudgets() }
        }
    }
}
