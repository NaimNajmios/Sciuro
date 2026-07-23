package com.sciuro.core.investment.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.investment.price.PriceProvider
import com.sciuro.core.ledger.db.SciuroDatabase

class InvestmentValuationEngine(
    private val database: SciuroDatabase,
    private val priceProvider: PriceProvider,
    private val eventBus: DomainEventBus
) {
    suspend fun refreshValuation() {
        priceProvider.refresh()
        val investments = database.investmentQueries.selectAllInvestments().executeAsList()

        for (inv in investments) {
            val currentPrice = priceProvider.getCurrentPricePerUnit(inv.asset_type, inv.asset_symbol)
            if (currentPrice != null && currentPrice > 0.0) {
                eventBus.publish(
                    DomainEvent.InvestmentPriceRefreshed(
                        accountId = inv.id,
                        newPricePerUnit = currentPrice
                    )
                )
            }
        }
    }

    suspend fun getTotalCurrentValue(): Double {
        val investments = database.investmentQueries.selectAllInvestments().executeAsList()
        var total = 0.0

        for (inv in investments) {
            val currentPrice = priceProvider.getCurrentPricePerUnit(inv.asset_type, inv.asset_symbol)
            val price = currentPrice ?: inv.average_buy_price
            total += inv.units_held * price
        }

        return total
    }

    suspend fun getCurrentValue(investmentId: String): Double {
        val inv = database.investmentQueries.selectAllInvestments().executeAsList()
            .find { it.id == investmentId } ?: return 0.0

        val currentPrice = priceProvider.getCurrentPricePerUnit(inv.asset_type, inv.asset_symbol)
        val price = currentPrice ?: inv.average_buy_price
        return inv.units_held * price
    }
}
