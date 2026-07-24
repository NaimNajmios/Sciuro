package com.sciuro.core.debt.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.debt.repository.DebtPaymentLinkRepository
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DebtEngineTest {

    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var linkRepository: DebtPaymentLinkRepository
    private lateinit var engine: DebtEngine
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcDriver("jdbc:sqlite::memory:")
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        eventBus = DomainEventBus()
        linkRepository = DebtPaymentLinkRepository(database)
        engine = DebtEngine(database, linkRepository, eventBus)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    @Test
    fun `processDebtPayments links matching outflow transactions to active debts`() = runBlocking {
        insertDebt(id = "debt_1", name = "Car Loan", principal = 50000.0, remaining = 45000.0, status = "ACTIVE")
        insertTransaction(id = "tx_1", category = "cat_debt_payment", amount = 1000.0, merchant = null)

        engine.processDebtPayments()

        val linked = database.debtPaymentLinkQueries.selectPaymentLinkByTransaction("tx_1").executeAsOneOrNull()
        assertNotNull(linked)
        assertEquals("debt_1", linked.debt_id)
        assertEquals(1000.0, linked.amount_applied, 0.001)
    }

    @Test
    fun `processDebtPayments matches by merchant name when category_id not cat_debt_payment`() = runBlocking {
        insertDebt(id = "debt_2", name = "Credit Card", principal = 10000.0, remaining = 5000.0, status = "ACTIVE")
        insertTransaction(id = "tx_2", category = "cat_expense", amount = 500.0, merchant = "Credit Card Payment")

        engine.processDebtPayments()

        val linked = database.debtPaymentLinkQueries.selectPaymentLinkByTransaction("tx_2").executeAsOneOrNull()
        assertNotNull(linked, "Should match by merchant name containing debt name")
        assertEquals("debt_2", linked.debt_id)
    }

    @Test
    fun `processDebtPayments skips already-linked transactions`() = runBlocking {
        insertDebt(id = "debt_3", name = "Personal Loan", principal = 20000.0, remaining = 15000.0, status = "ACTIVE")
        insertTransaction(id = "tx_3", category = "cat_debt_payment", amount = 500.0, merchant = null)

        linkRepository.linkPayment("debt_3", "tx_3", 500.0)

        engine.processDebtPayments()

        val links = database.debtPaymentLinkQueries.selectPaymentLinksByDebt("debt_3").executeAsList()
        assertEquals(1, links.size, "Should not create a duplicate link")
    }

    @Test
    fun `processDebtPayments excludes transfer transactions`() = runBlocking {
        insertDebt(id = "debt_4", name = "Study Loan", principal = 30000.0, remaining = 25000.0, status = "ACTIVE")
        insertTransaction(id = "tx_out", category = "cat_debt_payment", amount = 1000.0, merchant = null)
        insertTransaction(id = "tx_in", category = "cat_income", amount = 1000.0, merchant = null)

        database.transferLinkQueries.insertTransferLink(
            id = "link_1", outflow_transaction_id = "tx_out",
            inflow_transaction_id = "tx_in", amount = 1000.0,
            created_at = 1000L
        )

        engine.processDebtPayments()

        val linked = database.debtPaymentLinkQueries.selectPaymentLinkByTransaction("tx_out").executeAsOneOrNull()
        assertEquals(null, linked, "Transfer transactions should be excluded")
    }

    @Test
    fun `processDebtPayments skips paid-off debts`() = runBlocking {
        insertDebt(id = "debt_po", name = "Paid Loan", principal = 5000.0, remaining = 0.0, status = "PAID_OFF")
        insertTransaction(id = "tx_po", category = "cat_debt_payment", amount = 500.0, merchant = null)

        engine.processDebtPayments()

        val linked = database.debtPaymentLinkQueries.selectPaymentLinkByTransaction("tx_po").executeAsOneOrNull()
        assertEquals(null, linked, "Should skip PAID_OFF debts")
    }

    @Test
    fun `processDebtPayments skips archived debts`() = runBlocking {
        insertDebt(id = "debt_ar", name = "Archived Debt", principal = 1000.0, remaining = 500.0, status = "ARCHIVED")
        insertTransaction(id = "tx_ar", category = "cat_debt_payment", amount = 100.0, merchant = null)

        engine.processDebtPayments()

        val linked = database.debtPaymentLinkQueries.selectPaymentLinkByTransaction("tx_ar").executeAsOneOrNull()
        assertEquals(null, linked, "Should skip ARCHIVED debts")
    }

    @Test
    fun `processDebtPayments calculates remaining balance and publishes event`() = runBlocking {
        insertDebt(id = "debt_bal", name = "Balance Loan", principal = 10000.0, remaining = 8000.0, status = "ACTIVE")
        insertTransaction(id = "tx_bal1", category = "cat_debt_payment", amount = 2000.0, merchant = null)

        engine.processDebtPayments()

        val updated = database.debtQueries.selectAllDebts().executeAsList().first { it.id == "debt_bal" }
        assertEquals(6000.0, updated.remaining_balance, 0.001)

        val event = eventBus.events.first() as DomainEvent.DebtBalanceUpdated
        assertEquals("debt_bal", event.debtId)
        assertEquals(6000.0, event.newBalance, 0.001)
        assertEquals("AUTO_MATCH", event.method)
    }

    @Test
    fun `processDebtPayments publishes DebtFullyPaidOff when balance reaches zero`() = runBlocking {
        insertDebt(id = "debt_full", name = "Full Pay Debt", principal = 5000.0, remaining = 5000.0, status = "ACTIVE")
        insertTransaction(id = "tx_full1", category = "cat_debt_payment", amount = 5000.0, merchant = null)

        engine.processDebtPayments()

        val updated = database.debtQueries.selectAllDebts().executeAsList().first { it.id == "debt_full" }
        assertEquals(0.0, updated.remaining_balance, 0.001)

        val events = mutableListOf<DomainEvent>()
        eventBus.events.collect { events.add(it); if (events.size >= 2) return@collect }
        assertTrue(events.any { it is DomainEvent.DebtBalanceUpdated })
        assertTrue(events.any { it is DomainEvent.DebtFullyPaidOff })
    }

    @Test
    fun `processDebtPayments no-ops when no debts exist`() = runBlocking {
        engine.processDebtPayments()
    }

    @Test
    fun `processDebtPayments no-ops when no transactions match any debt`() = runBlocking {
        insertDebt(id = "debt_no", name = "No Match", principal = 1000.0, remaining = 800.0, status = "ACTIVE")
        insertTransaction(id = "tx_no", category = "cat_income", amount = 500.0, merchant = "Salary")

        engine.processDebtPayments()

        val updated = database.debtQueries.selectAllDebts().executeAsList().first { it.id == "debt_no" }
        assertEquals(800.0, updated.remaining_balance, 0.001)
    }

    private fun insertDebt(id: String, name: String, principal: Double, remaining: Double, status: String) {
        database.debtQueries.insertDebt(
            id = id, name = name, debt_type = "LOAN",
            direction = "I_OWE", counterparty_name = null,
            status = status, principal_amount = principal,
            remaining_balance = remaining, interest_rate = null,
            due_date = null, associated_account_id = null,
            notes = null, created_at = 0L, updated_at = 0L
        )
    }

    private fun insertTransaction(id: String, category: String, amount: Double, merchant: String?) {
        database.transactionRecordQueries.insertTransaction(
            id = id, account_id = "acc_1", category_id = category,
            amount = amount, direction = "OUTFLOW", merchant = merchant,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )
    }
}
