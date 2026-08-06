package com.sciuro.core.transfer.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.events.DomainEventBus
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransferRepositoryManualLinkTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SciuroDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transferRepository: TransferRepository

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
        transactionRepository = TransactionRepository(fakeAuditRepository, database, accountRepository, DomainEventBus())
        transferRepository = TransferRepository(fakeAuditRepository, database, transactionRepository, accountRepository)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    private suspend fun book(accountId: String, direction: String, amount: Double, timestamp: Long): String {
        val txId = "tx_${accountId}_${direction}_$timestamp"
        transactionRepository.bookTransaction(
            Transaction(
                id = txId,
                accountId = accountId,
                categoryId = null,
                amount = amount,
                direction = direction,
                merchant = null,
                timestamp = timestamp,
                referenceId = null,
                isReviewed = false
            ),
            source = AuditSource.SYSTEM_AUTO
        )
        return txId
    }

    @Test
    fun `linkCandidatePair links and records one manual confirmation`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        val outTxId = book("acc_a", "OUTFLOW", 50.0, 1000L)
        val inTxId = book("acc_b", "INFLOW", 50.0, 2000L)

        val link = transferRepository.linkCandidatePair(outTxId, inTxId)
        assertNotNull(link, "candidate pair must link")
        assertEquals(outTxId, link.outflowTransactionId)
        assertEquals(inTxId, link.inflowTransactionId)

        assertEquals(
            1,
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOne().toInt(),
            "one manual confirmation expected after a manual link"
        )
        assertNull(
            database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(),
            "manual link must not immediately confirm the pair"
        )
    }

    @Test
    fun `linkCandidatePair resolves inflow-outflow ordering regardless of argument order`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        val outTxId = book("acc_a", "OUTFLOW", 50.0, 1000L)
        val inTxId = book("acc_b", "INFLOW", 50.0, 2000L)

        val link = transferRepository.linkCandidatePair(inTxId, outTxId)
        assertNotNull(link)
        assertEquals(outTxId, link.outflowTransactionId)
        assertEquals(inTxId, link.inflowTransactionId)
    }

    @Test
    fun `three manual links promote the pair to account_pair_confirmation`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        for (i in 1..3) {
            val outTxId = book("acc_a", "OUTFLOW", 50.0, i * 1000L)
            val inTxId = book("acc_b", "INFLOW", 50.0, i * 1000L + 500L)
            val link = transferRepository.linkCandidatePair(outTxId, inTxId)
            assertNotNull(link, "manual link $i should succeed")
        }

        assertNotNull(
            database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(),
            "pair must be promoted to confirmed after the threshold"
        )
        assertEquals(
            AccountRepository.MANUAL_CONFIRMATION_THRESHOLD.toLong(),
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOne(),
            "three manual confirmations expected"
        )
    }

    @Test
    fun `already linked transactions cannot be relinked and do not increment count`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        val outTxId = book("acc_a", "OUTFLOW", 50.0, 1000L)
        val inTxId = book("acc_b", "INFLOW", 50.0, 2000L)

        assertNotNull(transferRepository.linkCandidatePair(outTxId, inTxId))
        assertNull(transferRepository.linkCandidatePair(outTxId, inTxId), "second link attempt must fail")
        assertEquals(
            1,
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOne().toInt(),
            "failed relink must not increment the confirmation count"
        )
    }

    @Test
    fun `automatic link keeps immediate account pair confirmation`() = runBlocking {
        accountRepository.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = "acc_b", name = "B", type = "Bank"))

        val outTxId = book("acc_a", "OUTFLOW", 50.0, 1000L)
        val inTxId = book("acc_b", "INFLOW", 50.0, 2000L)

        transferRepository.linkTransactions(
            com.sciuro.core.transfer.model.TransferLink(
                id = "link_auto",
                outflowTransactionId = outTxId,
                inflowTransactionId = inTxId,
                amount = 50.0,
                createdAt = System.currentTimeMillis()
            )
        )

        assertNotNull(
            database.accountQueries.selectAccountPairConfirmation("acc_a", "acc_b").executeAsOneOrNull(),
            "automatic link must keep confirming the pair immediately"
        )
        assertNull(
            database.accountQueries.selectManualConfirmationCount("acc_a", "acc_b").executeAsOneOrNull(),
            "automatic link must not count as a manual confirmation"
        )
    }
}
