package com.sciuro.core.obligations.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.repository.ObligationRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObligationCycleMatcherTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var obligationRepository: ObligationRepository
    private lateinit var matcher: ObligationCycleMatcher

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
        eventBus = DomainEventBus()
        obligationRepository = ObligationRepository(fakeAuditRepository, database)
        matcher = ObligationCycleMatcher(database, obligationRepository, eventBus)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    @Test
    fun `advances next due date when merchant matches active obligation`() = runBlocking {
        val initialDueDate = 100000000L
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = initialDueDate,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 15.0, direction = "OUTFLOW",
            categoryId = null, merchant = "netflix"
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        val expectedNextDue = initialDueDate + 30L * 24L * 60L * 60L * 1000L
        assertEquals(expectedNextDue, updated.next_due_date,
            "Due date should advance 30 days for MONTHLY frequency")
    }

    @Test
    fun `publishes ObligationCycleSettled event on match`() = runBlocking {
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = 100000000L,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        val events = mutableListOf<DomainEvent>()
        val job = launch {
            eventBus.events.collect { events.add(it) }
        }

        yield()

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 15.0, direction = "OUTFLOW",
            categoryId = null, merchant = "netflix"
        )

        job.cancel()

        assertEquals(1, events.size)
        val settledEvent = events.first()
        assertTrue(settledEvent is DomainEvent.ObligationCycleSettled)
        assertEquals("oblig_1", (settledEvent as DomainEvent.ObligationCycleSettled).obligationId)
        assertEquals("tx_1", settledEvent.transactionId)
    }

    @Test
    fun `publishes ObligationAmountDrifted when amount differs beyond threshold`() = runBlocking {
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = 100000000L,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        val events = mutableListOf<DomainEvent>()
        val job = launch {
            eventBus.events.collect { events.add(it) }
        }

        yield()

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 25.0, direction = "OUTFLOW",
            categoryId = null, merchant = "netflix"
        )

        job.cancel()

        val driftEvent = events.firstOrNull { it is DomainEvent.ObligationAmountDrifted }
        assertNotNull(driftEvent, "Should publish ObligationAmountDrifted when amount changes from 15 to 25")
        val drift = driftEvent as DomainEvent.ObligationAmountDrifted
        assertEquals("oblig_1", drift.obligationId)
        assertEquals(15.0, drift.oldAmount)
        assertEquals(25.0, drift.newAmount)
    }

    @Test
    fun `ignores INFLOW transactions`() = runBlocking {
        val initialDueDate = 100000000L
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = initialDueDate,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 15.0, direction = "INFLOW",
            categoryId = null, merchant = "netflix"
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        assertEquals(initialDueDate, updated.next_due_date,
            "Due date should not change for INFLOW transactions")
    }

    @Test
    fun `skips when no active obligation matches`() = runBlocking {
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = 100000000L,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        val initialDueDate = 100000000L
        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 15.0, direction = "OUTFLOW",
            categoryId = null, merchant = null
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        assertEquals(initialDueDate, updated.next_due_date,
            "Due date should not change when no obligation matches")
    }

    @Test
    fun `matches by category when merchant is null`() = runBlocking {
        val initialDueDate = 100000000L
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Rent", amount = 1200.0,
            frequency = "MONTHLY", next_due_date = initialDueDate,
            category_id = "cat_rent", account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 1200.0, direction = "OUTFLOW",
            categoryId = "cat_rent", merchant = null
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        val expectedNextDue = initialDueDate + 30L * 24L * 60L * 60L * 1000L
        assertEquals(expectedNextDue, updated.next_due_date,
            "Due date should advance when category matches")
    }

    @Test
    fun `advances due date correctly for WEEKLY frequency`() = runBlocking {
        val initialDueDate = 100000000L
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Weekly Cleaner", amount = 50.0,
            frequency = "WEEKLY", next_due_date = initialDueDate,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 50.0, direction = "OUTFLOW",
            categoryId = null, merchant = "weekly cleaner"
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        val expectedNextDue = initialDueDate + 7L * 24L * 60L * 60L * 1000L
        assertEquals(expectedNextDue, updated.next_due_date,
            "Due date should advance 7 days for WEEKLY frequency")
    }

    @Test
    fun `advances due date correctly for YEARLY frequency`() = runBlocking {
        val initialDueDate = 100000000L
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Annual Insurance", amount = 500.0,
            frequency = "YEARLY", next_due_date = initialDueDate,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 500.0, direction = "OUTFLOW",
            categoryId = null, merchant = "insurance"
        )

        val updated = database.obligationQueries.selectAllActiveObligations().executeAsOne()
        val expectedNextDue = initialDueDate + 365L * 24L * 60L * 60L * 1000L
        assertEquals(expectedNextDue, updated.next_due_date,
            "Due date should advance 365 days for YEARLY frequency")
    }

    @Test
    fun `does not publish drift when amount difference is within threshold`() = runBlocking {
        database.obligationQueries.insertObligation(
            id = "oblig_1", name = "Netflix Subscription", amount = 15.0,
            frequency = "MONTHLY", next_due_date = 100000000L,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        val events = mutableListOf<DomainEvent>()
        val job = launch {
            eventBus.events.collect { events.add(it) }
        }

        yield()

        matcher.onTransactionBooked(
            transactionId = "tx_1", amount = 16.0, direction = "OUTFLOW",
            categoryId = null, merchant = "netflix"
        )

        job.cancel()

        val driftEvent = events.firstOrNull { it is DomainEvent.ObligationAmountDrifted }
        assertEquals(null, driftEvent,
            "Should not publish ObligationAmountDrifted for 1.0 difference within threshold")
    }
}
