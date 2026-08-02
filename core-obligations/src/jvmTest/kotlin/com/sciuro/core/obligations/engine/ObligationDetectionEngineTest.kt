package com.sciuro.core.obligations.engine

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.obligations.repository.ObligationRepository
import kotlinx.coroutines.flow.first
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

class ObligationDetectionEngineTest {

    private lateinit var database: SciuroDatabase
    private lateinit var eventBus: DomainEventBus
    private lateinit var obligationRepository: ObligationRepository
    private lateinit var engine: ObligationDetectionEngine
    private lateinit var driver: SqlDriver
    private lateinit var settingsProvider: TestSettingsProvider

    private class TestSettingsProvider(
        autoConfirmEnabled: Boolean = false,
        autoConfirmThreshold: Int = 3
    ) : SettingsProvider {
        var autoConfirmEnabled: Boolean = autoConfirmEnabled
        private var _autoConfirmThreshold: Int = autoConfirmThreshold
        override fun isObligationAutoConfirmEnabled(): Boolean = autoConfirmEnabled
        override fun setObligationAutoConfirmEnabled(enabled: Boolean) { autoConfirmEnabled = enabled }
        override fun getAutoConfirmThreshold(): Int = _autoConfirmThreshold
        override fun setAutoConfirmThreshold(threshold: Int) { _autoConfirmThreshold = threshold }
        override fun getSilentAutoConfirmThreshold(): Float = 0.8f
        override fun setSilentAutoConfirmThreshold(threshold: Float) {}
        override fun isTransactionAutoConfirmEnabled(): Boolean = false
        override fun setTransactionAutoConfirmEnabled(enabled: Boolean) {}
        override fun isTrustValidatedLlmEnabled(): Boolean = false
        override fun setTrustValidatedLlmEnabled(enabled: Boolean) {}
        override fun isLockEnabled(): Boolean = false
        override fun setLockEnabled(enabled: Boolean) {}
        override fun isLlmEnabled(): Boolean = true
        override fun setLlmEnabled(enabled: Boolean) {}
        override fun getLlmModelName(): String = ""
        override fun setLlmModelName(name: String) {}
        override fun getApiKey(): String? = null
        override fun setApiKey(apiKey: String) {}
        override fun getQuickLabels(): List<String> = emptyList()
        override fun setQuickLabels(labels: List<String>) {}
        override fun getBudgetWarningThreshold(): Float = 0.8f
        override fun setBudgetWarningThreshold(threshold: Float) {}
        override fun getIngestionAllowlistAdditions(): Set<String> = emptySet()
        override fun setIngestionAllowlistAdditions(packages: Set<String>) {}
        override fun getIngestionAllowlistRemovals(): Set<String> = emptySet()
        override fun setIngestionAllowlistRemovals(packages: Set<String>) {}
        override fun getManualPrice(key: String): Double? = null
        override fun setManualPrice(key: String, price: Double) {}
        override fun isQuietHoursEnabled(): Boolean = false
        override fun setQuietHoursEnabled(enabled: Boolean) {}
        override fun getQuietHoursStart(): Int = 22
        override fun setQuietHoursStart(hour: Int) {}
        override fun getQuietHoursEnd(): Int = 7
        override fun setQuietHoursEnd(hour: Int) {}
        override fun hasSeenBatteryPrompt(): Boolean = false
        override fun setHasSeenBatteryPrompt(hasSeen: Boolean) {}
        override fun isDeveloperOptionsVisible(): Boolean = false
        override fun setDeveloperOptionsVisible(visible: Boolean) {}
        override fun getEngineLastRunMs(engineName: String): Long = 0L
        override fun setEngineLastRunMs(engineName: String, timestampMs: Long) {}
        override fun getLlmConfig(): LlmParsingConfig = LlmParsingConfig()
        override fun getLastBackupTimestamp(): Long = 0L
        override fun setLastBackupTimestamp(timestamp: Long) {}
        override fun getLastMilestoneReached(): Double = 0.0
        override fun setLastMilestoneReached(milestone: Double) {}
    }

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        eventBus = DomainEventBus()
        settingsProvider = TestSettingsProvider()

        val fakeAuditRepository = object : AuditRepository {
            override fun logMutation(log: AuditLog) {}
            override suspend fun getLogsForEntity(entityId: String, entityType: EntityType) = emptyList<AuditLog>()
            override suspend fun getAllLogs() = emptyList<AuditLog>()
            override fun getAuditIntegrityGaps(): Long = 0L
        }

        obligationRepository = ObligationRepository(fakeAuditRepository, database)
        engine = ObligationDetectionEngine(database, obligationRepository, eventBus, settingsProvider)
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }

    @Test
    fun `runDetection creates obligation for merchant with 3 plus same-amount outflows`() = runBlocking {
        val baseTime = 1000L
        val monthMs = 30L * 24L * 60L * 60L * 1000L
        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_$i", merchant = "netflix", amount = 15.0, timestamp = baseTime + i * monthMs)
        }

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertEquals(1, obligations.size)
        assertEquals("Netflix Subscription", obligations[0].name)
        assertEquals(15.0, obligations[0].amount, 0.001)
        assertEquals("MONTHLY", obligations[0].frequency)
    }

    @Test
    fun `runDetection skips merchants with fewer than 3 outflow transactions`() = runBlocking {
        insertMerchantTransaction(id = "tx_s1", merchant = "spotify", amount = 10.0, timestamp = 1000L)
        insertMerchantTransaction(id = "tx_s2", merchant = "spotify", amount = 10.0, timestamp = 2000L)

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertTrue(obligations.isEmpty())
    }

    @Test
    fun `runDetection skips merchants with fewer than 3 outflows even if 3 total transactions`() = runBlocking {
        insertMerchantTransaction(id = "tx_m1", merchant = "mix", amount = 10.0, timestamp = 1000L, direction = "OUTFLOW")
        insertMerchantTransaction(id = "tx_m2", merchant = "mix", amount = 10.0, timestamp = 2000L, direction = "OUTFLOW")
        insertMerchantTransaction(id = "tx_m3", merchant = "mix", amount = 10.0, timestamp = 3000L, direction = "INFLOW")

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertTrue(obligations.isEmpty())
    }

    @Test
    fun `runDetection skips merchants with varying amounts exceeding tolerance`() = runBlocking {
        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_v$i", merchant = "cloud", amount = 10.0 + i * 5.0, timestamp = 1000L + i * 1000L)
        }

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertTrue(obligations.isEmpty())
    }

    @Test
    fun `runDetection skips merchants with existing active obligations`() = runBlocking {
        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_e$i", merchant = "gym", amount = 50.0, timestamp = 1000L + i * 1000L)
        }

        database.obligationQueries.insertObligation(
            id = "oblig_existing", name = "Gym Membership", amount = 50.0,
            frequency = "MONTHLY", next_due_date = 100000L,
            category_id = null, account_id = null, is_active = 1L,
            created_at = 0L, updated_at = 0L, last_paid_date = null
        )

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertEquals(1, obligations.size, "Should not create duplicate")
    }

    @Test
    fun `runDetection publishes RecurringObligationConfirmed when auto-confirm enabled and trusted`() = runBlocking {
        settingsProvider.autoConfirmEnabled = true
        settingsProvider.setAutoConfirmThreshold(1)

        database.merchantCategoryRuleQueries.upsertMerchantRule(
            merchant_key = "netflix", category_id = "cat_entertainment",
            confirmation_count = 5L, first_seen_at = 0L, last_confirmed_at = 0L
        )

        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_ac$i", merchant = "netflix", amount = 15.0, timestamp = 1000L + i * 1000L)
        }

        val events = mutableListOf<DomainEvent>()
        val job = launch {
            eventBus.events.collect { events.add(it) }
        }

        yield()

        engine.runDetection()

        job.cancel()

        assertTrue(events.any { it is DomainEvent.ObligationCreated }, "Should publish ObligationCreated")
        assertTrue(events.any { it is DomainEvent.RecurringObligationConfirmed }, "Should publish RecurringObligationConfirmed")
    }

    @Test
    fun `runDetection publishes RecurringObligationProposed when auto-confirm disabled`() = runBlocking {
        settingsProvider.autoConfirmEnabled = false

        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_pr$i", merchant = "dropbox", amount = 12.0, timestamp = 1000L + i * 1000L)
        }

        val events = mutableListOf<DomainEvent>()
        val job = launch {
            eventBus.events.collect { events.add(it) }
        }

        yield()

        engine.runDetection()

        job.cancel()

        assertTrue(events.any { it is DomainEvent.RecurringObligationProposed }, "Should publish RecurringObligationProposed")
    }

    @Test
    fun `runDetection supports multiple merchants independently`() = runBlocking {
        repeat(3) { i ->
            insertMerchantTransaction(id = "tx_mul_a$i", merchant = "merchant_a", amount = 20.0, timestamp = 1000L + i * 1000L)
            insertMerchantTransaction(id = "tx_mul_b$i", merchant = "merchant_b", amount = 30.0, timestamp = 1000L + i * 1000L)
        }

        engine.runDetection()

        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertEquals(2, obligations.size)
    }

    @Test
    fun `runDetection no-ops when no transactions exist`() = runBlocking {
        engine.runDetection()
        val obligations = database.obligationQueries.selectAllActiveObligations().executeAsList()
        assertTrue(obligations.isEmpty())
    }

    private fun insertMerchantTransaction(id: String, merchant: String, amount: Double,
                                          timestamp: Long, direction: String = "OUTFLOW") {
        database.transactionRecordQueries.insertTransaction(
            id = id, account_id = "acc_1", category_id = "cat_entertainment",
            amount = amount, direction = direction, merchant = merchant,
            timestamp = timestamp, reference_id = null,
            is_reviewed = 1L, extraction_method = null, confidence = null,
            raw_event_id = null, review_tier = "MANUAL", auto_confirmed_at = null,
            created_at = timestamp, updated_at = timestamp
        )
    }
}
