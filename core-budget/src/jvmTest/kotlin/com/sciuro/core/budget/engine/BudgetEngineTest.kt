package com.sciuro.core.budget.engine

import app.cash.sqldelight.db.SqlDriver
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.sqlite.SQLiteDataSource

class BudgetEngineTest {

    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var engine: BudgetEngine
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        val dataSource = SQLiteDataSource()
        dataSource.setUrl("jdbc:sqlite::memory:")
        driver = app.cash.sqldelight.driver.jdbc.JdbcDrivers.fromDataSource(dataSource)
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        eventBus = DomainEventBus()
        engine = BudgetEngine(database, eventBus)
    }

    @AfterTest
    fun tearDown() {
        database.driver.close()
    }

    @Test
    fun `processBudgets updates current_spent for matching monthly transactions`() = runBlocking {
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH)
        now.set(year, month, 1, 0, 0, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        val monthStart = now.timeInMillis

        database.budgetQueries.insertBudget(
            id = "budget_1", category_id = "cat_food",
            allocated_amount = 500.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 0L,
            alert_threshold_percent = null,
            created_at = monthStart, updated_at = monthStart
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_1", account_id = "acc_1", category_id = "cat_food",
            amount = 150.0, direction = "OUTFLOW", merchant = "Restaurant",
            timestamp = monthStart + 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = "PARSER", confidence = 0.95,
            raw_event_id = null, review_tier = "AUTO", auto_confirmed_at = null,
            created_at = monthStart, updated_at = monthStart
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_2", account_id = "acc_1", category_id = "cat_food",
            amount = 50.0, direction = "OUTFLOW", merchant = "Cafe",
            timestamp = monthStart + 2000L, reference_id = null,
            is_reviewed = 1L, extraction_method = "PARSER", confidence = 0.95,
            raw_event_id = null, review_tier = "AUTO", auto_confirmed_at = null,
            created_at = monthStart, updated_at = monthStart
        )

        engine.processBudgets()

        val updated = database.budgetQueries.selectBudgetById("budget_1").executeAsOne()
        assertEquals(200.0, updated.current_spent, 0.001)
    }

    @Test
    fun `processBudgets handles weekly budget period`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_w", category_id = "cat_food",
            allocated_amount = 200.0, current_spent = 0.0,
            period = "WEEKLY", rollover = 0L,
            alert_threshold_percent = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_w1", account_id = "acc_1", category_id = "cat_food",
            amount = 50.0, direction = "OUTFLOW", merchant = null,
            timestamp = java.util.Calendar.getInstance().timeInMillis, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        engine.processBudgets()

        val updated = database.budgetQueries.selectBudgetById("budget_w").executeAsOne()
        assertTrue(updated.current_spent > 0)
    }

    @Test
    fun `processBudgets excludes transfer transactions from spend calculation`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_tr", category_id = "cat_food",
            allocated_amount = 500.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 0L,
            alert_threshold_percent = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_tr1", account_id = "acc_1", category_id = "cat_food",
            amount = 100.0, direction = "OUTFLOW", merchant = null,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_tr_out", account_id = "acc_1", category_id = "cat_food",
            amount = 200.0, direction = "OUTFLOW", merchant = null,
            timestamp = 2000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_tr_in", account_id = "acc_2", category_id = "cat_income",
            amount = 200.0, direction = "INFLOW", merchant = null,
            timestamp = 2000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        database.transferLinkQueries.insertTransferLink(
            id = "link_1", outflow_transaction_id = "tx_tr_out",
            inflow_transaction_id = "tx_tr_in", amount = 200.0,
            created_at = 2000L
        )

        engine.processBudgets()

        val updated = database.budgetQueries.selectBudgetById("budget_tr").executeAsOne()
        assertEquals(100.0, updated.current_spent, 0.001)
    }

    @Test
    fun `processBudgets handles rollover from previous period`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_ro", category_id = "cat_food",
            allocated_amount = 500.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 1L,
            alert_threshold_percent = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_ro1", account_id = "acc_1", category_id = "cat_food",
            amount = 300.0, direction = "OUTFLOW", merchant = null,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        engine.processBudgets()

        val updated = database.budgetQueries.selectBudgetById("budget_ro").executeAsOne()
        assertEquals(300.0, updated.current_spent, 0.001)
    }

    @Test
    fun `processBudgets fires BudgetThresholdCrossed when threshold exceeded`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_th", category_id = "cat_food",
            allocated_amount = 100.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 0L,
            alert_threshold_percent = 0.5,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_th1", account_id = "acc_1", category_id = "cat_food",
            amount = 80.0, direction = "OUTFLOW", merchant = null,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        engine.processBudgets()

        val event = eventBus.events.first() as DomainEvent.BudgetThresholdCrossed
        assertEquals("cat_food", event.categoryId)
        assertTrue(event.percentUsed >= 0.8)
    }

    @Test
    fun `processBudgets does not fire event when spend below threshold`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_nt", category_id = "cat_food",
            allocated_amount = 1000.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 0L,
            alert_threshold_percent = 0.9,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_nt1", account_id = "acc_1", category_id = "cat_food",
            amount = 50.0, direction = "OUTFLOW", merchant = null,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        engine.processBudgets()

        val event = eventBus.events.firstOrNull()
        assertEquals(null, event)
    }

    @Test
    fun `processBudgets no-ops when no budgets exist`() = runBlocking {
        engine.processBudgets()
    }

    @Test
    fun `processBudgets no-ops when no transactions match budget category`() = runBlocking {
        database.budgetQueries.insertBudget(
            id = "budget_em", category_id = "cat_food",
            allocated_amount = 500.0, current_spent = 0.0,
            period = "MONTHLY", rollover = 0L,
            alert_threshold_percent = null,
            created_at = 0L, updated_at = 0L
        )

        database.transactionRecordQueries.insertTransaction(
            id = "tx_em1", account_id = "acc_1", category_id = "cat_transport",
            amount = 100.0, direction = "OUTFLOW", merchant = null,
            timestamp = 1000L, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = 0L, updated_at = 0L
        )

        engine.processBudgets()

        val updated = database.budgetQueries.selectBudgetById("budget_em").executeAsOne()
        assertEquals(0.0, updated.current_spent, 0.001)
    }
}

