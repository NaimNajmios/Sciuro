package com.sciuro.core.investment.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.investment.model.Investment
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map

class InvestmentRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createInvestment(investment: Investment): Investment {
        val now = currentTimeMillis()
        return database.transactionWithResult {
            database.investmentQueries.insertInvestment(
                id = investment.id,
                asset_symbol = investment.assetSymbol,
                asset_name = investment.assetName,
                asset_type = investment.assetType,
                units_held = investment.unitsHeld,
                unit_type = investment.unitType,
                average_buy_price = investment.averageBuyPrice,
                associated_account_id = investment.associatedAccountId,
                created_at = now,
                updated_at = now,
                status = investment.status
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.INVESTMENT_ACCOUNT,
                    entityId = investment.id,
                    action = AuditAction.CREATE,
                    beforeState = null,
                    afterState = investment.toString(),
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
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
        val now = currentTimeMillis()

        database.transaction {
            database.investmentQueries.updateInvestment(
                asset_symbol = investment.asset_symbol,
                asset_name = investment.asset_name,
                asset_type = investment.asset_type,
                units_held = newUnitsHeld,
                unit_type = investment.unit_type,
                average_buy_price = newAvgPrice,
                associated_account_id = investment.associated_account_id,
                updated_at = now,
                id = investmentId
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.INVESTMENT_ACCOUNT,
                    entityId = investmentId,
                    action = AuditAction.UPDATE,
                    beforeState = "units: ${investment.units_held}, avg_price: ${investment.average_buy_price}",
                    afterState = "units: $newUnitsHeld, avg_price: $newAvgPrice",
                    source = AuditSource.SYSTEM_AUTO,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }
    
    suspend fun updateInvestment(investment: Investment) {
        val now = currentTimeMillis()
        database.transaction {
            database.investmentQueries.updateInvestment(
                asset_symbol = investment.assetSymbol,
                asset_name = investment.assetName,
                asset_type = investment.assetType,
                units_held = investment.unitsHeld,
                unit_type = investment.unitType,
                average_buy_price = investment.averageBuyPrice,
                associated_account_id = investment.associatedAccountId,
                updated_at = now,
                id = investment.id
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.INVESTMENT_ACCOUNT,
                    entityId = investment.id,
                    action = AuditAction.UPDATE,
                    beforeState = "Update Investment",
                    afterState = investment.toString(),
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }
    
    suspend fun deleteInvestment(investmentId: String) {
        val now = currentTimeMillis()
        database.transaction {
            database.investmentQueries.updateInvestmentStatus(
                status = "DELETED",
                updated_at = now,
                id = investmentId
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.INVESTMENT_ACCOUNT,
                    entityId = investmentId,
                    action = AuditAction.DELETE,
                    beforeState = "Delete Investment",
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }
    
    fun observeInvestments(): Flow<List<Investment>> {
        return database.investmentQueries.selectAllInvestments()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    Investment(
                        id = it.id,
                        assetSymbol = it.asset_symbol,
                        assetName = it.asset_name,
                        assetType = it.asset_type,
                        unitType = it.unit_type,
                        unitsHeld = it.units_held,
                        averageBuyPrice = it.average_buy_price,
                        associatedAccountId = it.associated_account_id,
                        status = it.status
                    )
                }
            }
    }
}
