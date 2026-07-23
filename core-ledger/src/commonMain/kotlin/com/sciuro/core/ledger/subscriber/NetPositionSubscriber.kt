package com.sciuro.core.ledger.subscriber

import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.model.NetPosition
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetPositionSubscriber(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    private val _netPosition = MutableStateFlow(NetPosition(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val netPosition: StateFlow<NetPosition> = _netPosition.asStateFlow()

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            recompute()
        }
        scope.launch(Dispatchers.Default) {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.TransactionCategorized,
                    is DomainEvent.TransferMatched,
                    is DomainEvent.CashCredited,
                    is DomainEvent.CashDebited,
                    is DomainEvent.DebtBalanceUpdated,
                    is DomainEvent.InvestmentPriceRefreshed,
                    is DomainEvent.InvestmentTransactionRecorded -> recompute()
                    else -> {}
                }
            }
        }
    }

    private suspend fun recompute() {
        val accounts = database.accountQueries.selectAllAccounts().executeAsList()
        val accountBalances = accounts.filter { it.status != "DELETED" }.sumOf { it.balance }

        val investments = database.investmentQueries.selectAllInvestments().executeAsList()
            .filter { it.status == "ACTIVE" }
        val investmentValue = investments.sumOf { it.units_held * it.average_buy_price }

        val debts = database.debtQueries.selectAllDebts().executeAsList()
            .filter { it.status == "ACTIVE" }
        val debtsOwed = debts.filter { it.direction == "I_OWE" || it.direction == null }
            .sumOf { it.remaining_balance }
        val debtsReceivable = debts.filter { it.direction == "OWED_TO_ME" }
            .sumOf { it.remaining_balance }

        val cashAccounts = accounts.filter { it.type == "CASH" && it.status != "DELETED" }
        val cashBalance = cashAccounts.sumOf { it.balance }

        val netWorth = accountBalances + cashBalance + investmentValue - debtsOwed + debtsReceivable

        _netPosition.value = NetPosition(
            totalAccounts = accountBalances,
            totalCash = cashBalance,
            totalInvestments = investmentValue,
            totalDebtsOwed = debtsOwed,
            totalDebtsReceivable = debtsReceivable,
            netWorth = netWorth
        )
    }
}
