package com.sciuro.core.transfer.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.transfer.repository.TransferRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransferDetectionEngineTest {

    private lateinit var database: SciuroDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transferRepository: TransferRepository
    private lateinit var engine: TransferDetectionEngine

    private val fakeAuditRepository = object : AuditRepository {
        override suspend fun logMutation(log: AuditLog) {}
        override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
        override suspend fun getAllLogs() = emptyList<AuditLog>()
    }

    @BeforeTest
    fun setUp() {
        val driver: SqlDriver = JdbcDriver("jdbc:sqlite::memory:")
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)

        accountRepository = AccountRepository(fakeAuditRepository, database)
        transactionRepository = TransactionRepository(fakeAuditRepository, database, accountRepository)
        transferRepository = TransferRepository(fakeAuditRepository, database, transactionRepository)
        engine = TransferDetectionEngine(database, transferRepository, DomainEventBus())
    }

    @AfterTest
    fun tearDown() {
        (database.driver as? java.io.Closeable)?.close()
    }

    @Test
    fun `Tier 1 links transfer when counterparty account number matches own account suffix`() = runBlocking {
        val myAccountId = "acc_1"
        accountRepository.createAccount(
            Account(id = myAccountId, name = "CIMB Savings", type = "Bank", accountNumber = "1234567890")
        )

        val inflowTxId = bookInflowTransaction(accountId = myAccountId, amount = 100.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = myAccountId,
            newTxAmount = 100.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = "7890"
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tier 1 should link when counterparty account suffix matches")
        assertEquals(100.0, link.amount)
    }

    @Test
    fun `Tier 1 links regardless of time gap between transactions`() = runBlocking {
        val myAccountId = "acc_1"
        accountRepository.createAccount(
            Account(id = myAccountId, name = "CIMB Savings", type = "Bank", accountNumber = "601234567890")
        )

        val inflowTxId = bookInflowTransaction(accountId = myAccountId, amount = 250.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = myAccountId,
            newTxAmount = 250.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 3_600_000L,
            counterpartyAccountNumber = "7890"
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tier 1 should link with 1-hour gap")
    }

    @Test
    fun `Tier 1 does not link when counterparty account number does not match any own account`() = runBlocking {
        val myAccountId = "acc_1"
        accountRepository.createAccount(
            Account(id = myAccountId, name = "CIMB Savings", type = "Bank", accountNumber = "1234567890")
        )

        bookInflowTransaction(accountId = myAccountId, amount = 100.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = myAccountId,
            newTxAmount = 100.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = "9999"
        )

        val link = transferRepository.getTransferForTransaction("outflow_1")
        assertNull(link, "Should not link when suffix does not match")
    }

    @Test
    fun `Tier 1 handles masked account numbers with asterisks`() = runBlocking {
        val myAccountId = "acc_1"
        accountRepository.createAccount(
            Account(id = myAccountId, name = "CIMB Savings", type = "Bank", accountNumber = "9876543210")
        )

        val inflowTxId = bookInflowTransaction(accountId = myAccountId, amount = 50.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = myAccountId,
            newTxAmount = 50.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = "****3210"
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tier 1 should match masked account number")
    }

    @Test
    fun `Tier 1 does not link when counterparty number matches but amounts differ`() = runBlocking {
        val myAccountId = "acc_1"
        accountRepository.createAccount(
            Account(id = myAccountId, name = "CIMB Savings", type = "Bank", accountNumber = "1234567890")
        )

        val inflowTxId = bookInflowTransaction(accountId = myAccountId, amount = 100.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = myAccountId,
            newTxAmount = 200.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = "7890"
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNull(link, "Should not link when amounts don't match even in Tier 1")
    }

    @Test
    fun `Tier 1 matches cross-account self-transfer`() = runBlocking {
        val accountA = "acc_savings"
        val accountB = "acc_current"
        accountRepository.createAccount(
            Account(id = accountA, name = "CIMB Savings", type = "Bank", accountNumber = "111122223333")
        )
        accountRepository.createAccount(
            Account(id = accountB, name = "CIMB Current", type = "Bank", accountNumber = "444455556666")
        )

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 500.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 500.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = "556666"
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tier 1 should link cross-account self-transfer")
    }

    @Test
    fun `Tier 2 heuristic links when pair was previously confirmed`() = runBlocking {
        val accountA = "acc_a"
        val accountB = "acc_b"
        accountRepository.createAccount(Account(id = accountA, name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = accountB, name = "B", type = "Bank"))

        database.accountQueries.insertAccountPairConfirmation(accountA, accountB, currentTimeMillis())

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 75.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 75.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 1060_000L,
            counterpartyAccountNumber = null
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tier 2 should link for confirmed pairs within time window")
    }

    @Test
    fun `Tier 2 heuristic does not link when pair is unconfirmed`() = runBlocking {
        val accountA = "acc_a"
        val accountB = "acc_b"
        accountRepository.createAccount(Account(id = accountA, name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = accountB, name = "B", type = "Bank"))

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 75.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 75.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 1060_000L,
            counterpartyAccountNumber = null
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNull(link, "Tier 2 should NOT link unconfirmed pairs")
    }

    @Test
    fun `Tier 2 heuristic does not link outside 2-minute window`() = runBlocking {
        val accountA = "acc_a"
        val accountB = "acc_b"
        accountRepository.createAccount(Account(id = accountA, name = "A", type = "Bank"))
        accountRepository.createAccount(Account(id = accountB, name = "B", type = "Bank"))

        database.accountQueries.insertAccountPairConfirmation(accountA, accountB, currentTimeMillis())

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 75.0, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 75.0,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 500_000L,
            counterpartyAccountNumber = null
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNull(link, "Tier 2 should not link outside the 2-minute window")
    }

    @Test
    fun `tight match links DuitNow-style self-transfer without prior pair confirmation`() = runBlocking {
        val accountA = "acc_a"
        val accountB = "acc_b"
        accountRepository.createAccount(Account(id = accountA, name = "BSN", type = "Bank", accountNumber = "111122223333"))
        accountRepository.createAccount(Account(id = accountB, name = "Maybank", type = "Bank", accountNumber = "444455556666"))

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 5.40, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 5.40,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 2000L,
            counterpartyAccountNumber = null
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNotNull(link, "Tight match should link without pair confirmation with 1-second gap")
        assertEquals(5.40, link.amount)
    }

    @Test
    fun `tight match does not link when gap exceeds 15 seconds`() = runBlocking {
        val accountA = "acc_a"
        val accountB = "acc_b"
        accountRepository.createAccount(Account(id = accountA, name = "BSN", type = "Bank"))
        accountRepository.createAccount(Account(id = accountB, name = "Maybank", type = "Bank"))

        val inflowTxId = bookInflowTransaction(accountId = accountB, amount = 5.40, timestamp = 1000L)

        engine.onTransactionBooked(
            newTxId = "outflow_1",
            newTxAccountId = accountA,
            newTxAmount = 5.40,
            newTxDirection = "OUTFLOW",
            newTxTimestamp = 20_000L,
            counterpartyAccountNumber = null
        )

        val link = transferRepository.getTransferForTransaction(inflowTxId)
        assertNull(link, "Tight match should not link with 19-second gap")
    }

    private suspend fun bookInflowTransaction(
        accountId: String,
        amount: Double,
        timestamp: Long
    ): String {
        val txId = "tx_${accountId}_$timestamp"
        val tx = com.sciuro.core.ledger.model.Transaction(
            id = txId,
            accountId = accountId,
            categoryId = null,
            amount = amount,
            direction = "INFLOW",
            merchant = null,
            timestamp = timestamp,
            referenceId = null,
            isReviewed = true
        )
        transactionRepository.bookTransaction(tx, source = AuditSource.SYSTEM_AUTO)
        return txId
    }
}
