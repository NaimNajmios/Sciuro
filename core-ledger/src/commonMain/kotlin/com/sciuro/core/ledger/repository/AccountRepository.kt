package com.sciuro.core.ledger.repository

import com.sciuro.core.audit.model.AuditAction
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.repository.AuditableRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Account
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(
    auditRepository: AuditRepository,
    private val database: SciuroDatabase
) : AuditableRepository(auditRepository) {

    suspend fun createAccount(account: Account): Account {
        val now = currentTimeMillis()
        return database.transactionWithResult {
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

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.ACCOUNT,
                    entityId = account.id,
                    action = AuditAction.CREATE,
                    beforeState = null,
                    afterState = account.toString(),
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )

            account
        }
    }

    suspend fun updateAccount(account: Account) {
        val now = currentTimeMillis()
        database.transaction {
            database.accountQueries.updateAccount(
                name = account.name,
                type = account.type,
                associated_package = account.associatedPackage,
                color = account.color,
                updated_at = now,
                id = account.id,
                account_number = account.accountNumber,
                account_holder_name = account.accountHolderName,
                bank_institution_code = account.bankInstitutionCode,
                qr_image_path = account.qrImagePath,
                qr_payload_text = account.qrPayloadText
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.ACCOUNT,
                    entityId = account.id,
                    action = AuditAction.UPDATE,
                    beforeState = "Update Account",
                    afterState = account.toString(),
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val account = database.accountQueries.selectAccountById(accountId).executeAsOneOrNull()
        if (account?.is_system == 1L) {
            throw IllegalStateException("Cannot delete a system account")
        }

        val now = currentTimeMillis()
        database.transaction {
            database.accountQueries.updateAccountStatus(
                status = "DELETED",
                updated_at = now,
                id = accountId
            )
            database.merchantAccountRuleQueries.deleteMerchantAccountRuleByAccount(accountId)

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.ACCOUNT,
                    entityId = accountId,
                    action = AuditAction.DELETE,
                    beforeState = "Delete Account",
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun archiveAccount(accountId: String) {
        val account = database.accountQueries.selectAccountById(accountId).executeAsOneOrNull()
        if (account?.is_system == 1L) {
            throw IllegalStateException("Cannot archive a system account")
        }

        val now = currentTimeMillis()
        database.transaction {
            database.accountQueries.updateAccountStatus(
                status = "ARCHIVED",
                updated_at = now,
                id = accountId
            )

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.ACCOUNT,
                    entityId = accountId,
                    action = AuditAction.UPDATE,
                    beforeState = "Archive Account",
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    suspend fun linkAccountPair(accountIdA: String, accountIdB: String) {
        val sorted = listOf(accountIdA, accountIdB).sorted()
        if (sorted.size != 2) return
        database.accountQueries.insertAccountPairConfirmation(sorted[0], sorted[1], currentTimeMillis())
    }

    suspend fun unlinkAccountPair(accountIdA: String, accountIdB: String) {
        val sorted = listOf(accountIdA, accountIdB).sorted()
        if (sorted.size != 2) return

        val now = currentTimeMillis()
        database.transaction {
            database.accountQueries.deleteAccountPairConfirmation(sorted[0], sorted[1])

            auditRepository.logMutation(
                AuditLog(
                    id = generateUuid(),
                    entityType = EntityType.ACCOUNT,
                    entityId = sorted.joinToString(":"),
                    action = AuditAction.UPDATE,
                    beforeState = "linked pair: ${sorted[0]} <-> ${sorted[1]}",
                    afterState = null,
                    source = AuditSource.USER_MANUAL,
                    confidence = null,
                    timestamp = now
                )
            )
        }
    }

    fun observeLinkedPairs(): Flow<List<Pair<String, String>>> {
        return database.accountQueries.selectAllAccountPairConfirmations()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row -> Pair(row.account_id_a, row.account_id_b) }
            }
    }
    
    fun observeAccounts(): Flow<List<com.sciuro.core.ledger.db.Account>> {
        return database.accountQueries.selectAllAccounts()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    suspend fun getAccountByPackageName(packageName: String): com.sciuro.core.ledger.db.Account? {
        return database.accountQueries.selectAccountByPackage(packageName).executeAsOneOrNull()
    }

    suspend fun getAccountByNumberSuffix(suffix: String): com.sciuro.core.ledger.db.Account? {
        return database.accountQueries.selectAccountByNumberSuffix(suffix).executeAsOneOrNull()
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
