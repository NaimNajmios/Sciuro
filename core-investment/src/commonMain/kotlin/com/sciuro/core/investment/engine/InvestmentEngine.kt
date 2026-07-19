package com.sciuro.core.investment.engine

import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.investment.repository.InvestmentRepository

class InvestmentEngine(
    private val database: SciuroDatabase,
    private val investmentRepository: InvestmentRepository
) {
    /**
     * Scans transactions and applies them to known investments.
     */
    suspend fun processInvestments() {
        val allInvestments = database.investmentQueries.selectAllInvestments().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        
        for (investment in allInvestments) {
            val purchases = allTransactions.filter {
                it.direction == "OUTFLOW" && 
                (it.category_id == "cat_investment" || it.merchant?.contains(investment.asset_name, ignoreCase = true) == true)
            }
            
            // For B6 heuristic, we track total fiat invested as units (1 RM = 1 Unit) 
            // since notifications don't contain unit price info (e.g. Raiz, ASB)
            val totalInvested = purchases.sumOf { it.amount }
            
            if (kotlin.math.abs(totalInvested - investment.units_held) > 0.01) {
                database.investmentQueries.updateInvestment(
                    units_held = totalInvested,
                    average_buy_price = 1.0, 
                    updated_at = com.sciuro.core.audit.util.currentTimeMillis(),
                    id = investment.id
                )
            }
        }
    }
}
