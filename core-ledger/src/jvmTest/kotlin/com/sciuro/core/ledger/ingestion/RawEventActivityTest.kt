package com.sciuro.core.ledger.ingestion

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.model.IngestionActivityStatus
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RawEventActivityTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SciuroDatabase
    private lateinit var repository: RawEventRepository

    private fun createDatabase() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        database = SciuroDatabase(driver)
        repository = RawEventRepository(database)
    }

    private suspend fun persist(id: String, financial: Boolean = true, capturedAt: Long) {
        repository.persistRawEvent(
            id = id,
            sourceType = "NOTIFICATION",
            sourcePackageOrAddress = "com.test.bank",
            title = "Bank Alert",
            text = "RM 10.00 spent",
            timestamp = capturedAt,
            capturedAt = capturedAt,
            financialSignal = financial
        )
    }

    @Test
    fun `last captured at is observable and null when empty`() = runBlocking {
        createDatabase()
        assertNull(repository.observeLastCapturedAt().first())

        repository.persistRawEvent(
            id = "evt_1", sourceType = "NOTIFICATION", sourcePackageOrAddress = "com.test.bank",
            title = "t", text = "b", timestamp = 100L, capturedAt = 5_000L
        )
        assertEquals(5_000L, repository.observeLastCapturedAt().first())
    }

    @Test
    fun `activity status transitions are persisted and observable`() = runBlocking {
        createDatabase()
        persist("evt_1", capturedAt = 1_000L)
        repository.markActivityStatus("evt_1", IngestionActivityStatus.PARSED, "review_tier=AUTO_UNDO")

        val row = repository.getRawEventById("evt_1")!!
        assertEquals(IngestionActivityStatus.PARSED, row.activity_status)
        assertEquals("review_tier=AUTO_UNDO", row.activity_reason)
    }

    @Test
    fun `recent activity excludes ignored events and orders by capture time desc`() = runBlocking {
        createDatabase()
        persist("evt_1", capturedAt = 1_000L)
        persist("evt_2", capturedAt = 2_000L)
        persist("evt_3", capturedAt = 3_000L)
        repository.markActivityStatus("evt_1", IngestionActivityStatus.PARSED)
        repository.markActivityStatus("evt_2", IngestionActivityStatus.NEEDS_REVIEW)
        repository.markActivityStatus("evt_3", IngestionActivityStatus.IGNORED)

        val recent = repository.observeRecentActivity(10).first()
        assertEquals(listOf("evt_2", "evt_1"), recent.map { it.id }, "IGNORED events must be excluded")
    }

    @Test
    fun `unacknowledged parse failures surface once until acknowledged`() = runBlocking {
        createDatabase()
        persist("evt_1", capturedAt = 1_000L)
        persist("evt_2", capturedAt = 2_000L)
        repository.markActivityStatus("evt_1", IngestionActivityStatus.DROPPED, "timeout")
        repository.markActivityStatus("evt_2", IngestionActivityStatus.DROPPED, "network_error")

        val failures = repository.observeUnacknowledgedParseFailures().first()
        assertEquals(listOf("evt_2", "evt_1"), failures.map { it.id })

        repository.acknowledgeActivityAlert("evt_2")
        val remaining = repository.observeUnacknowledgedParseFailures().first()
        assertEquals(listOf("evt_1"), remaining.map { it.id }, "acknowledged failure must not resurface")

        assertTrue(
            database.rawEventStagingQueries.selectRawEventById("evt_2").executeAsOneOrNull()?.user_alerted_at != null,
            "acknowledgment must record user_alerted_at"
        )
    }

    @AfterTest
    fun tearDown() {
        (driver as? java.io.Closeable)?.close()
    }
}
