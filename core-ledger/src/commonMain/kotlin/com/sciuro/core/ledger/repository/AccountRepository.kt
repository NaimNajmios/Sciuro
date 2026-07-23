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
import app.cash.sqldelight.coroutines.mapToOneOrNull
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
                associated_package = account.associatedPackage,
                created_at = now,
                updated_at = now,
                is_system = if (account.isSystem) 1L else 0L,
                status = account.status,
                color = account.color,
                account_number = account.accountNumber,
                account_holder_name = account.accountHolderName,
                bank_institution_code = account.bankInstitutionCode,
                qr_image_path = account.qrImagePath,
                qr_payload_text = account.qrPayloadText
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
    
    suspend fun updateAccount(account: Account) {
        withAudit(
            entityType = EntityType.ACCOUNT,
            entityId = account.id,
            action = AuditAction.UPDATE,
            beforeState = "Update Account",
            afterState = account.toString(),
            source = AuditSource.USER_MANUAL
        ) {
            database.accountQueries.updateAccount(
                name = account.name,
                type = account.type,
                associated_package = account.associatedPackage,
                color = account.color,
                updated_at = currentTimeMillis(),
                id = account.id,
                account_number = account.accountNumber,
                account_holder_name = account.accountHolderName,
                bank_institution_code = account.bankInstitutionCode,
                qr_image_path = account.qrImagePath,
                qr_payload_text = account.qrPayloadText
            )
        }
    }
    
    suspend fun deleteAccount(accountId: String) {
        val account = database.accountQueries.selectAccountById(accountId).executeAsOneOrNull()
        if (account?.is_system == 1L) {
            throw IllegalStateException("Cannot delete a system account")
        }
        
        withAudit(
            entityType = EntityType.ACCOUNT,
            entityId = accountId,
            action = AuditAction.DELETE,
            beforeState = "Delete Account",
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            database.accountQueries.updateAccountStatus(
                status = "DELETED",
                updated_at = currentTimeMillis(),
                id = accountId
            )
        }
    }
    
    suspend fun archiveAccount(accountId: String) {
        val account = database.accountQueries.selectAccountById(accountId).executeAsOneOrNull()
        if (account?.is_system == 1L) {
            throw IllegalStateException("Cannot archive a system account")
        }
        withAudit(
            entityType = EntityType.ACCOUNT,
            entityId = accountId,
            action = AuditAction.UPDATE,
            beforeState = "Archive Account",
            afterState = null,
            source = AuditSource.USER_MANUAL
        ) {
            database.accountQueries.updateAccountStatus(
                status = "ARCHIVED",
                updated_at = currentTimeMillis(),
                id = accountId
            )
        }
    }

    suspend fun linkAccountPair(accountIdA: String, accountIdB: String) {
        val sorted = listOf(accountIdA, accountIdB).sorted()
        if (sorted.size != 2) return
        database.accountQueries.insertAccountPairConfirmation(sorted[0], sorted[1], currentTimeMillis())
    }
    
    fun observeAccounts(): Flow<List<com.sciuro.core.ledger.db.Account>> {
        return database.accountQueries.selectAllAccounts()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    suspend fun getAccountByPackageName(packageName: String): com.sciuro.core.ledger.db.Account? {
        return database.accountQueries.selectAccountByPackage(packageName).executeAsOneOrNull()
    }

    suspend fun ensureDefaultAccountExists() {
        val accounts = database.accountQueries.selectAllAccounts().executeAsList()
        if (accounts.isEmpty()) {
            createAccount(
                Account(
                    id = "default-${currentTimeMillis()}",
                    name = "Personal Wallet",
                    type = "Cash",
                    currency = "MYR",
                    balance = 0.0,
                    associatedPackage = null,
                    isSystem = true
                )
            )
        }
    }

    fun observeAccountById(accountId: String): Flow<com.sciuro.core.ledger.db.Account?> {
        return database.accountQueries.selectAccountById(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
    }
}
