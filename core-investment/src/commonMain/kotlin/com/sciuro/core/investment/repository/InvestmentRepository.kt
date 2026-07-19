package com.sciuro.core.investment.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.investment.model.Investment

class InvestmentRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createInvestment(investment: Investment): Investment {
        return withAudit(
            entityType = EntityType.INVESTMENT_ACCOUNT,
            entityId = investment.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = investment.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.investmentQueries.insertInvestment(
                id = investment.id,
                asset_symbol = investment.assetSymbol,
                asset_name = investment.assetName,
                units_held = investment.unitsHeld,
                average_buy_price = investment.averageBuyPrice,
                associated_account_id = investment.associatedAccountId,
                created_at = now,
                updated_at = now
            )
            investment
        }
    }
    
    suspend fun recordPurchase(investmentId: String, unitsBought: Double, purchasePrice: Double) {
        val investment = database.investmentQueries.selectAllInvestments().executeAsList().find { it.id == investmentId } ?: return
        
        val totalCostOld = investment.units_held * investment.average_buy_price
        val totalCostNew = unitsBought * purchasePrice
        val newUnitsHeld = investment.units_held + unitsBought
        val newAvgPrice = if (newUnitsHeld > 0) (totalCostOld + totalCostNew) / newUnitsHeld else 0.0
        
        withAudit(
            entityType = EntityType.INVESTMENT_ACCOUNT,
            entityId = investmentId,
            action = AuditAction.UPDATE,
            beforeState = "units: ${investment.units_held}, avg_price: ${investment.average_buy_price}",
            afterState = "units: $newUnitsHeld, avg_price: $newAvgPrice",
            source = AuditSource.SYSTEM_AUTO
        ) {
            database.investmentQueries.updateInvestment(newUnitsHeld, newAvgPrice, currentTimeMillis(), investmentId)
        }
    }
}
