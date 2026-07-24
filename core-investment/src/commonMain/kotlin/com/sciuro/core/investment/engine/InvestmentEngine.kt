package com.sciuro.core.investment.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ledger.db.SciuroDatabase

class InvestmentEngine(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    suspend fun processInvestments() {
        val allInvestments = database.investmentQueries.selectAllInvestments().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        val transferTxIds = database.transferLinkQueries.selectAllTransferLinks().executeAsList()
            .flatMap { listOf(it.outflow_transaction_id, it.inflow_transaction_id) }
            .toSet()

        for (investment in allInvestments) {
            val purchases = allTransactions.filter {
                it.direction == "OUTFLOW" &&
                (it.category_id == "cat_investment" || it.merchant?.contains(investment.asset_name, ignoreCase = true) == true) &&
                it.id !in transferTxIds
            }

            val totalInvested = purchases.sumOf { it.amount }

            if (kotlin.math.abs(totalInvested - investment.units_held) > 0.01) {
                val unitType = investment.unit_type.ifBlank { "UNITS" }
                val newUnits = if (unitType == "GRAMS" || unitType == "UNITS") {
                    totalInvested
                } else {
                    totalInvested
                }

                database.investmentQueries.updateInvestment(
                    asset_symbol = investment.asset_symbol,
                    asset_name = investment.asset_name,
                    asset_type = investment.asset_type,
                    units_held = newUnits,
                    unit_type = unitType,
                    average_buy_price = investment.average_buy_price,
                    associated_account_id = investment.associated_account_id,
                    updated_at = com.sciuro.core.audit.util.currentTimeMillis(),
                    id = investment.id
                )

                eventBus.publish(
                    DomainEvent.InvestmentTransactionRecorded(
                        accountId = investment.id,
                        action = "BUY",
                        unitAmount = newUnits
                    )
                )
            }
        }
    }
}
