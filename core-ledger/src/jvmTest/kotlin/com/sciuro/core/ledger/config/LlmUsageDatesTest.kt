package com.sciuro.core.ledger.config

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmUsageDatesTest {

    private val kualaLumpur = TimeZone.of("Asia/Kuala_Lumpur")

    @Test
    fun `epoch in utc maps to 1970-01-01`() {
        assertEquals("1970-01-01", localDateKeyFor(0L, TimeZone.UTC))
    }

    @Test
    fun `instant at local midnight maps to the next local date`() {
        val instant = Instant.parse("2026-01-01T16:00:00Z") // 2026-01-02 00:00 in KL (UTC+8)
        assertEquals("2026-01-02", localDateKeyFor(instant.toEpochMilliseconds(), kualaLumpur))
    }

    @Test
    fun `instant just before local midnight keeps the same date`() {
        val instant = Instant.parse("2026-01-01T15:59:59Z") // 2026-01-01 23:59:59 in KL
        assertEquals("2026-01-01", localDateKeyFor(instant.toEpochMilliseconds(), kualaLumpur))
    }
}
