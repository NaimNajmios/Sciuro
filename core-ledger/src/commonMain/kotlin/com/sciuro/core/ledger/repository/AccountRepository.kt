package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Account
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createAccount(account: Account): Account {
        return withAudit(
            entityType = EntityType.ACCOUNT,
            entityId = account.id,
            action = AuditAction.CREATE,
            beforeState = null,
            afterState = account.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            val now = currentTimeMillis()
            database.accountQueries.insertAccount(
                id = account.id,
                name = account.name,
                type = account.type,
                currency = account.currency,
                balance = account.balance,
                created_at = now,
                updated_at = now
            )
            account
        }
    }
    
    suspend fun updateBalance(accountId: String, amount: Double) {
        withAudit(
            entityType = EntityType.ACCOUNT,
            entityId = accountId,
            action = AuditAction.UPDATE,
            beforeState = "balance update delta: $amount",
            afterState = null,
            source = AuditSource.SYSTEM_AUTO
        ) {
            database.accountQueries.updateBalance(
                balance = amount, // delta
                updated_at = currentTimeMillis(),
                id = accountId
            )
        }
    }
    
    fun observeAccounts(): Flow<List<com.sciuro.core.ledger.db.Account>> {
        return database.accountQueries.selectAllAccounts()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
}
