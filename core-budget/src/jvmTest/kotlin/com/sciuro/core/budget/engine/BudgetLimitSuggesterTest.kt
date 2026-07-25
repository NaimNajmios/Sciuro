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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

class BudgetLimitSuggesterTest {

    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var suggester: BudgetLimitSuggester
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        eventBus = DomainEventBus()
        suggester = BudgetLimitSuggester(database, eventBus)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun insertTransaction(id: String, categoryId: String, amount: Long, timestamp: Long) {
        database.transactionRecordQueries.insertTransaction(
            id = id,
            account_id = "acc_1",
            category_id = categoryId,
            amount = amount.toDouble(),
            direction = "OUTFLOW",
            merchant = null,
            timestamp = timestamp,
            reference_id = null,
            is_reviewed = 1L,
            extraction_method = null,
            confidence = null,
            raw_event_id = null,
            review_tier = "MANUAL",
            auto_confirmed_at = null,
            created_at = timestamp,
            updated_at = timestamp
        )
    }

    @Test
    fun `suggestLimit returns null when fewer than 3 transactions`() = runBlocking {
        val now = System.currentTimeMillis()
        insertTransaction("tx_1", "cat_food", 100, now - 1000L)
        insertTransaction("tx_2", "cat_food", 200, now - 2000L)

        val result = suggester.suggestLimit("cat_food")
        assertNull(result)
    }

    @Test
    fun `suggestLimit returns null when exactly 3 transactions`() = runBlocking {
        val now = System.currentTimeMillis()
        insertTransaction("tx_1", "cat_food", 100, now - 1000L)
        insertTransaction("tx_2", "cat_food", 200, now - 2000L)
        insertTransaction("tx_3", "cat_food", 300, now - 3000L)

        val result = suggester.suggestLimit("cat_food")
        assertNull(result)
    }

    @Test
    fun `suggestLimit returns trimmed mean for normal distribution`() = runBlocking {
        val now = System.currentTimeMillis()
        val amounts = listOf(100L, 150L, 200L, 250L, 300L, 350L, 400L)
        amounts.forEachIndexed { index, amount ->
            insertTransaction("tx_$index", "cat_food", amount, now - (index * 1000L))
        }

        val result = suggester.suggestLimit("cat_food")
        assertTrue(result != null && result > 0)
        assertTrue(result in 150.0..350.0)
    }

    @Test
    fun `suggestLimit handles all identical amounts`() = runBlocking {
        val now = System.currentTimeMillis()
        repeat(5) { index ->
            insertTransaction("tx_$index", "cat_food", 200, now - (index * 1000L))
        }

        val result = suggester.suggestLimit("cat_food")
        assertEquals(200.0, result!!, 0.001)
    }

    @Test
    fun `suggestLimit trims outliers correctly`() = runBlocking {
        val now = System.currentTimeMillis()
        val amounts = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 1000L)
        amounts.forEachIndexed { index, amount ->
            insertTransaction("tx_$index", "cat_food", amount, now - (index * 1000L))
        }

        val result = suggester.suggestLimit("cat_food")
        assertTrue(result != null && result > 0)
        assertTrue(result < 200.0, "Outlier (1000) should be trimmed from average")
    }

    @Test
    fun `suggestLimit filters old transactions outside lookback window`() = runBlocking {
        val now = System.currentTimeMillis()
        val ninetyOneDaysAgo = now - (91L * 24 * 60 * 60 * 1000)

        insertTransaction("tx_old", "cat_food", 1000, ninetyOneDaysAgo)
        repeat(5) { index ->
            insertTransaction("tx_$index", "cat_food", 50, now - (index * 1000L))
        }

        val result = suggester.suggestLimit("cat_food")
        assertTrue(result != null && result < 200.0, "Old transaction should not influence suggestion")
    }

    @Test
    fun `suggestAndPublish publishes BudgetLimitSuggested event`() = runBlocking {
        val now = System.currentTimeMillis()
        repeat(5) { index ->
            insertTransaction("tx_$index", "cat_food", 100, now - (index * 1000L))
        }

        val result = suggester.suggestAndPublish("cat_food")
        assertTrue(result != null && result > 0)

        val event = eventBus.events.first() as DomainEvent.BudgetLimitSuggested
        assertEquals("cat_food", event.categoryId)
        assertEquals(result, event.suggestedAmount, 0.001)
    }

    @Test
    fun `suggestAndPublish returns null when insufficient data`() = runBlocking {
        val now = System.currentTimeMillis()
        insertTransaction("tx_1", "cat_food", 100, now - 1000L)

        val result = suggester.suggestAndPublish("cat_food")
        assertNull(result)
    }
}
