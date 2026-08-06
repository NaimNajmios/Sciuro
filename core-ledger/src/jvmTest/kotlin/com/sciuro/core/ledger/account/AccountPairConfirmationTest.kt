package com.sciuro.core.ledger.account

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.repository.AccountRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountPairConfirmationTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SciuroDatabase
    private lateinit var accountRepository: AccountRepository

    private val fakeAuditRepository = object : AuditRepository {
        override fun logMutation(log: AuditLog) {}
        override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
        override suspend fun getAllLogs() = emptyList<AuditLog>()
        override fun getAuditIntegrityGaps(): Long = 0L
    }

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        accountRepository = AccountRepository(fakeAuditRepository, database)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    private fun pairRow(accountIdA: String, accountIdB: String): Boolean {
        val sorted = listOf(accountIdA, accountIdB).sorted()
        return database.accountQueries.selectAccountPairConfirmation(sorted[0], sorted[1]).executeAsOneOrNull() != null
    }

    @Test
    fun `manual confirmations accumulate and insert pair only at threshold`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        assertNull(
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOneOrNull(),
            "no confirmation row before first confirmation"
        )
        assertNull(database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(), "pair must not be confirmed before threshold")

        val first = accountRepository.recordManualConfirmation("acc_a", "acc_b")
        assertEquals(1, first)
        assertNull(database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(), "pair must not be confirmed after 1 confirmation")

        val second = accountRepository.recordManualConfirmation("acc_a", "acc_b")
        assertEquals(2, second)
        assertNull(database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(), "pair must not be confirmed after 2 confirmations")

        val third = accountRepository.recordManualConfirmation("acc_a", "acc_b")
        assertEquals(3, third)
        assertEquals(true, pairRow("acc_a", "acc_b"), "pair must be confirmed after 3 manual confirmations")
    }

    @Test
    fun `reversed account ordering hits the same canonical count`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        accountRepository.recordManualConfirmation("acc_b", "acc_a")
        accountRepository.recordManualConfirmation("acc_a", "acc_b")

        val count = database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOne().toInt()
        assertEquals(2, count, "both orderings must accumulate onto the same canonical row")
        assertNull(
            database.accountQueries.selectManualConfirmationCount("acc_b", "acc_a").executeAsOneOrNull(),
            "row is stored under canonical ordering only"
        )
    }

    @Test
    fun `automatic pair insert does not require manual confirmations`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        database.accountQueries.insertAccountPairConfirmation("acc_a", "acc_b", 0L)

        assertEquals(true, pairRow("acc_a", "acc_b"))
        assertNull(
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOneOrNull(),
            "automatic pair confirmation must not seed the manual confirmation count"
        )
    }

    @Test
    fun `manual confirmations respect AuditSource for threshold audit`() = runBlocking {
        val realAuditRepository = object : AuditRepository {
            val logs = mutableListOf<AuditLog>()
            override fun logMutation(log: AuditLog) { logs.add(log) }
            override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = logs.filter { it.entityId == entityId }
            override suspend fun getAllLogs() = logs.toList()
            override fun getAuditIntegrityGaps(): Long = 0L
        }
        val repo = AccountRepository(realAuditRepository, database)
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        repeat(AccountRepository.MANUAL_CONFIRMATION_THRESHOLD) {
            repo.recordManualConfirmation("acc_a", "acc_b")
        }

        val thresholdAudits = realAuditRepository.logs.filter { it.action.name == "UPDATE" }
        assertEquals(1, thresholdAudits.size, "exactly one threshold audit entry expected")
        assertEquals(AuditSource.USER_MANUAL, thresholdAudits.first().source)
    }
}
