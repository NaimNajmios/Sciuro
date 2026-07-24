package com.sciuro.core.investment.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
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

class InvestmentEngineTest {

    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var engine: InvestmentEngine
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcDriver("jdbc:sqlite::memory:")
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        eventBus = DomainEventBus()
        engine = InvestmentEngine(database, eventBus)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    @Test
    fun `processInvestments updates units_held from matching purchase transactions`() = runBlocking {
        insertInvestment(id = "inv_1", symbol = "GOLD", name = "Gold", type = "PRECIOUS_METAL",
            units = 10.0, unitType = "GRAMS")
        insertTransaction(id = "tx_1", category = "cat_investment", amount = 5.0, merchant = null)

        engine.processInvestments()

        val updated = database.investmentQueries.selectAllInvestments().executeAsList().first { it.id == "inv_1" }
        assertEquals(5.0, updated.units_held, 0.001)
    }

    @Test
    fun `processInvestments matches by merchant name when category_id does not match`() = runBlocking {
        insertInvestment(id = "inv_2", symbol = "VOO", name = "S&P 500 ETF", type = "ETF",
            units = 0.0, unitType = "UNITS")
        insertTransaction(id = "tx_2", category = "cat_misc", amount = 3.0, merchant = "Buy S&P 500 ETF")

        engine.processInvestments()

        val updated = database.investmentQueries.selectAllInvestments().executeAsList().first { it.id == "inv_2" }
        assertEquals(3.0, updated.units_held, 0.001)
    }

    @Test
    fun `processInvestments excludes transfer transactions`() = runBlocking {
        insertInvestment(id = "inv_3", symbol = "SILV", name = "Silver", type = "PRECIOUS_METAL",
            units = 0.0, unitType = "GRAMS")
        insertTransaction(id = "tx_3_out", category = "cat_investment", amount = 100.0, merchant = null)
        insertTransaction(id = "tx_3_in", category = "cat_income", amount = 100.0, merchant = null)

        database.transferLinkQueries.insertTransferLink(
            id = "link_3", outflow_transaction_id = "tx_3_out",
            inflow_transaction_id = "tx_3_in", amount = 100.0,
            created_at = 1000L
        )

        engine.processInvestments()

        val updated = database.investmentQueries.selectAllInvestments().executeAsList().first { it.id == "inv_3" }
        assertEquals(0.0, updated.units_held, 0.001)
    }

    @Test
    fun `processInvestments publishes InvestmentTransactionRecorded event`() = runBlocking {
        insertInvestment(id = "inv_4", symbol = "BTC", name = "Bitcoin", type = "CRYPTO",
            units = 0.0, unitType = "UNITS")
        insertTransaction(id = "tx_4", category = "cat_investment", amount = 2.5, merchant = null)

        engine.processInvestments()

        val event = eventBus.events.first() as DomainEvent.InvestmentTransactionRecorded
        assertEquals("inv_4", event.accountId)
        assertEquals("BUY", event.action)
        assertEquals(2.5, event.unitAmount, 0.001)
    }

    @Test
    fun `processInvestments no-ops when no investments exist`() = runBlocking {
        engine.processInvestments()
    }

    @Test
    fun `processInvestments no-ops when no transactions match`() = runBlocking {
        insertInvestment(id = "inv_5", symbol = "AAPL", name = "Apple", type = "STOCK",
            units = 10.0, unitType = "UNITS")
        insertTransaction(id = "tx_5", category = "cat_food", amount = 50.0, merchant = "Restaurant")

        engine.processInvestments()

        val updated = database.investmentQueries.selectAllInvestments().executeAsList().first { it.id == "inv_5" }
        assertEquals(10.0, updated.units_held, 0.001)
    }

    private fun insertInvestment(id: String, symbol: String, name: String, type: String,
                                  units: Double, unitType: String) {
        database.investmentQueries.insertInvestment(
            id = id, asset_symbol = symbol, asset_name = name,
            asset_type = type, units_held = units, unit_type = unitType,
            average_buy_price = 0.0, associated_account_id = null,
            created_at = 0L, updated_at = 0L, status = "ACTIVE"
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
