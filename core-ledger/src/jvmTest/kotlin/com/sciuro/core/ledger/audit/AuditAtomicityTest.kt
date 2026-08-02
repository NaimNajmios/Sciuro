package com.sciuro.core.ledger.audit

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuditAtomicityTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SciuroDatabase
    private lateinit var realAuditRepository: SqlDelightAuditRepository
    private lateinit var accountRepository: AccountRepository

    private val throwingAuditRepository = object : AuditRepository {
        override fun logMutation(log: AuditLog) {
            throw IllegalStateException("audit write failed")
        }

        override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
        override suspend fun getAllLogs() = emptyList<AuditLog>()
        override fun getAuditIntegrityGaps(): Long = 0L
    }

    private fun createDatabase(): SciuroDatabase {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        return SciuroDatabase(driver)
    }

    private fun transactionRepository(auditRepository: AuditRepository): TransactionRepository {
        return TransactionRepository(auditRepository, database, accountRepository, com.sciuro.core.audit.events.DomainEventBus())
    }

    private fun sampleTransaction(id: String, accountId: String): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            categoryId = null,
            amount = 100.0,
            direction = "INFLOW",
            merchant = null,
            timestamp = 1000L,
            referenceId = null,
            isReviewed = false
        )
    }

    @Test
    fun `bookTransaction commits mutation and audit in the same transaction`() = runBlocking {
        database = createDatabase()
        realAuditRepository = SqlDelightAuditRepository(database)
        accountRepository = AccountRepository(realAuditRepository, database)
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank", balance = 500.0))

        val txRepository = transactionRepository(realAuditRepository)
        txRepository.bookTransaction(sampleTransaction("tx_1", "acc_a"), source = AuditSource.SYSTEM_AUTO)

        assertNotNull(
            database.transactionRecordQueries.selectTransactionById("tx_1").executeAsOneOrNull(),
            "transaction must be persisted"
        )
        assertEquals(
            600.0,
            database.accountQueries.selectAccountById("acc_a").executeAsOne().balance,
            "balance must reflect the booked transaction"
        )
        val audits = realAuditRepository.getAllLogs().filter { it.entityType == EntityType.TRANSACTION }
        assertEquals(1, audits.size, "exactly one TRANSACTION audit entry expected")
        assertEquals("tx_1", audits.first().entityId)
        assertEquals(0L, realAuditRepository.getAuditIntegrityGaps())
    }

    @Test
    fun `audit write failure rolls back the mutation and balance`() = runBlocking {
        database = createDatabase()
        realAuditRepository = SqlDelightAuditRepository(database)
        accountRepository = AccountRepository(realAuditRepository, database)
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank", balance = 500.0))

        val txRepository = transactionRepository(throwingAuditRepository)

        assertFailsWith<IllegalStateException> {
            txRepository.bookTransaction(sampleTransaction("tx_1", "acc_a"), source = AuditSource.SYSTEM_AUTO)
        }

        assertNull(
            database.transactionRecordQueries.selectTransactionById("tx_1").executeAsOneOrNull(),
            "transaction must be rolled back when audit write fails"
        )
        assertEquals(
            500.0,
            database.accountQueries.selectAccountById("acc_a").executeAsOne().balance,
            "balance must be rolled back when audit write fails"
        )
        val txAudits = realAuditRepository.getAllLogs().filter { it.entityType == EntityType.TRANSACTION }
        assertEquals(0, txAudits.size, "no transaction audit entries should remain after rollback")
    }

    @Test
    fun `auditIntegrityCheck reports transactions missing an audit record`() = runBlocking {
        database = createDatabase()
        realAuditRepository = SqlDelightAuditRepository(database)
        accountRepository = AccountRepository(realAuditRepository, database)
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val txRepository = transactionRepository(realAuditRepository)
        txRepository.bookTransaction(sampleTransaction("tx_1", "acc_a"), source = AuditSource.SYSTEM_AUTO)
        assertEquals(0L, realAuditRepository.getAuditIntegrityGaps())

        database.transactionRecordQueries.insertTransaction(
            id = "tx_orphan",
            account_id = "acc_a",
            category_id = null,
            amount = 50.0,
            direction = "OUTFLOW",
            merchant = null,
            timestamp = 2000L,
            reference_id = null,
            is_reviewed = 1L,
            extraction_method = null,
            confidence = null,
            raw_event_id = null,
            review_tier = "MANUAL",
            auto_confirmed_at = null,
            created_at = 2000L,
            updated_at = 2000L
        )

        assertEquals(1L, realAuditRepository.getAuditIntegrityGaps(), "orphan transaction must be counted")
    }

    @Test
    fun `auditIntegrityCheck returns zero when every transaction is audited`() = runBlocking {
        database = createDatabase()
        realAuditRepository = SqlDelightAuditRepository(database)
        accountRepository = AccountRepository(realAuditRepository, database)
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val txRepository = transactionRepository(realAuditRepository)
        txRepository.bookTransaction(sampleTransaction("tx_1", "acc_a"), source = AuditSource.SYSTEM_AUTO)
        txRepository.bookTransaction(sampleTransaction("tx_2", "acc_a").copy(amount = 200.0), source = AuditSource.SYSTEM_AUTO)
        txRepository.deleteTransaction("tx_2")

        assertEquals(0L, realAuditRepository.getAuditIntegrityGaps())
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }
}
