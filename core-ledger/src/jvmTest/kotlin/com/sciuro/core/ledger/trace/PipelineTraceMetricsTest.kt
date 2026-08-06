package com.sciuro.core.ledger.trace

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PipelineTraceMetricsTest {

    private fun createDatabase(): SciuroDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SciuroDatabase.Schema.create(driver)
        return SciuroDatabase(driver)
    }

    private fun insertTrace(
        db: SciuroDatabase,
        id: String,
        detail: Map<String, String?>,
        createdAt: Long,
        stage: TraceStage = TraceStage.PARSE_LLM
    ) {
        db.pipelineTraceQueries.insertTrace(
            id = id,
            raw_event_id = "evt-1",
            transaction_id = null,
            stage = stage.name,
            outcome = TraceOutcome.SUCCESS.name,
            duration_ms = 0,
            confidence = null,
            detail_json = detail.toString(),
            created_at = createdAt,
            package_name = "com.test"
        )
    }

    @Test
    fun `range queries count provider calls and cache hits separately`() = runBlocking {
        val db = createDatabase()
        insertTrace(db, "t1", mapOf("verdict" to "validated_untrusted", "provider_called" to "true"), 1_000L)
        insertTrace(db, "t2", mapOf("verdict" to "cache_hit", "provider_called" to "false"), 2_000L)
        insertTrace(db, "t3", mapOf("verdict" to "daily_cap_exceeded", "provider_called" to "false"), 3_000L)

        val providerCalls = db.pipelineTraceQueries.countParseLlmProviderCallsInRange(500L, 4_000L).executeAsOne()
        val cacheHits = db.pipelineTraceQueries.countParseLlmCacheHitsInRange(500L, 4_000L).executeAsOne()
        val outcomes = db.pipelineTraceQueries.countTraceByOutcomeInRange(500L, 4_000L).executeAsList()

        assertEquals(1L, providerCalls)
        assertEquals(1L, cacheHits)
        assertEquals(1, outcomes.size)
        assertEquals("PARSE_LLM", outcomes[0].stage)
        assertEquals(3L, outcomes[0].cnt)
    }

    @Test
    fun `range query excludes traces outside the window`() = runBlocking {
        val db = createDatabase()
        insertTrace(db, "t1", mapOf("verdict" to "validated_untrusted", "provider_called" to "true"), 1_000L)
        insertTrace(db, "t2", mapOf("verdict" to "validated_untrusted", "provider_called" to "true"), 5_000L)

        val providerCalls = db.pipelineTraceQueries.countParseLlmProviderCallsInRange(500L, 4_000L).executeAsOne()
        assertEquals(1L, providerCalls)
    }
}
