package com.sciuro.core.transfer.engine

import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.ledger.model.Account
import com.sciuro.core.ledger.model.Transaction
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransactionDedupTest {

    @Test
    fun `findLikelyDuplicate returns transaction when same amount direction and within window`() = runBlocking {
        val database = TestDatabase.create()
        val fakeAuditRepo = FakeAuditRepository()
        val accountRepo = com.sciuro.core.ledger.repository.AccountRepository(fakeAuditRepo, database)
        val txRepo = com.sciuro.core.ledger.repository.TransactionRepository(fakeAuditRepo, database, accountRepo)

        accountRepo.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val tx = Transaction(
            id = "tx_1", accountId = "acc_a", categoryId = null,
            amount = 5.40, direction = "INFLOW", merchant = null,
            timestamp = 1000L, isReviewed = false
        )
        txRepo.bookTransaction(tx, source = AuditSource.SYSTEM_AUTO)

        val duplicate = txRepo.findLikelyDuplicate(
            amount = 5.40, direction = "INFLOW", timestamp = 2000L, windowMs = 90_000
        )
        assertNotNull(duplicate, "Should find duplicate within 90s window")
    }

    @Test
    fun `findLikelyDuplicate returns null outside window`() = runBlocking {
        val database = TestDatabase.create()
        val fakeAuditRepo = FakeAuditRepository()
        val accountRepo = com.sciuro.core.ledger.repository.AccountRepository(fakeAuditRepo, database)
        val txRepo = com.sciuro.core.ledger.repository.TransactionRepository(fakeAuditRepo, database, accountRepo)

        accountRepo.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val tx = Transaction(
            id = "tx_1", accountId = "acc_a", categoryId = null,
            amount = 5.40, direction = "INFLOW", merchant = null,
            timestamp = 1000L, isReviewed = false
        )
        txRepo.bookTransaction(tx, source = AuditSource.SYSTEM_AUTO)

        val duplicate = txRepo.findLikelyDuplicate(
            amount = 5.40, direction = "INFLOW", timestamp = 200_000L, windowMs = 90_000
        )
        assertNull(duplicate, "Should not find duplicate outside 90s window")
    }

    @Test
    fun `findLikelyDuplicate returns null when amount differs`() = runBlocking {
        val database = TestDatabase.create()
        val fakeAuditRepo = FakeAuditRepository()
        val accountRepo = com.sciuro.core.ledger.repository.AccountRepository(fakeAuditRepo, database)
        val txRepo = com.sciuro.core.ledger.repository.TransactionRepository(fakeAuditRepo, database, accountRepo)

        accountRepo.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val tx = Transaction(
            id = "tx_1", accountId = "acc_a", categoryId = null,
            amount = 5.40, direction = "INFLOW", merchant = null,
            timestamp = 1000L, isReviewed = false
        )
        txRepo.bookTransaction(tx, source = AuditSource.SYSTEM_AUTO)

        val duplicate = txRepo.findLikelyDuplicate(
            amount = 10.00, direction = "INFLOW", timestamp = 2000L, windowMs = 90_000
        )
        assertNull(duplicate, "Should not find duplicate when amount differs")
    }

    @Test
    fun `findLikelyDuplicate returns null when direction differs`() = runBlocking {
        val database = TestDatabase.create()
        val fakeAuditRepo = FakeAuditRepository()
        val accountRepo = com.sciuro.core.ledger.repository.AccountRepository(fakeAuditRepo, database)
        val txRepo = com.sciuro.core.ledger.repository.TransactionRepository(fakeAuditRepo, database, accountRepo)

        accountRepo.createAccount(Account(id = "acc_a", name = "A", type = "Bank"))

        val tx = Transaction(
            id = "tx_1", accountId = "acc_a", categoryId = null,
            amount = 5.40, direction = "INFLOW", merchant = null,
            timestamp = 1000L, isReviewed = false
        )
        txRepo.bookTransaction(tx, source = AuditSource.SYSTEM_AUTO)

        val duplicate = txRepo.findLikelyDuplicate(
            amount = 5.40, direction = "OUTFLOW", timestamp = 2000L, windowMs = 90_000
        )
        assertNull(duplicate, "Should not find duplicate when direction differs")
    }
}
