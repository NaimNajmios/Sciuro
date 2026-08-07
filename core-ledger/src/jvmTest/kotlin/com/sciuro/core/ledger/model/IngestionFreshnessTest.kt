package com.sciuro.core.ledger.model

import kotlin.test.Test
import kotlin.test.assertEquals

class IngestionFreshnessTest {

    private val now = 1_000_000L

    @Test
    fun `listener disabled is OFF regardless of last capture`() {
        assertEquals(CaptureFreshness.OFF, IngestionFreshness.freshness(false, now - 1_000L, now))
        assertEquals(CaptureFreshness.OFF, IngestionFreshness.freshness(false, null, now))
    }

    @Test
    fun `no capture yet is OFF`() {
        assertEquals(CaptureFreshness.OFF, IngestionFreshness.freshness(true, null, now))
    }

    @Test
    fun `capture within five minutes is FRESH`() {
        assertEquals(CaptureFreshness.FRESH, IngestionFreshness.freshness(true, now - 5L * 60L * 1000L, now))
        assertEquals(CaptureFreshness.FRESH, IngestionFreshness.freshness(true, now - 1L, now))
        assertEquals(CaptureFreshness.FRESH, IngestionFreshness.freshness(true, now, now))
    }

    @Test
    fun `capture older than five minutes but within a day is STALE`() {
        assertEquals(CaptureFreshness.STALE, IngestionFreshness.freshness(true, now - 6L * 60L * 1000L, now))
        assertEquals(CaptureFreshness.STALE, IngestionFreshness.freshness(true, now - 23L * 60L * 60L * 1000L, now))
    }

    @Test
    fun `capture older than a day is OFF`() {
        assertEquals(CaptureFreshness.OFF, IngestionFreshness.freshness(true, now - 25L * 60L * 60L * 1000L, now))
    }

    @Test
    fun `future-dated capture is FRESH`() {
        assertEquals(CaptureFreshness.FRESH, IngestionFreshness.freshness(true, now + 1_000L, now))
    }
}
