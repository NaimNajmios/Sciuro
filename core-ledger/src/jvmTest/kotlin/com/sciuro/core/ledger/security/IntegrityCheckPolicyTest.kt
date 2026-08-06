package com.sciuro.core.ledger.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegrityCheckPolicyTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `never checked means due`() {
        assertTrue(IntegrityCheckPolicy.isDue(lastCheckMs = 0L, nowMs = now))
    }

    @Test
    fun `not due before the weekly interval elapses`() {
        val lastCheck = now - IntegrityCheckPolicy.CHECK_INTERVAL_MS + 1
        assertFalse(IntegrityCheckPolicy.isDue(lastCheckMs = lastCheck, nowMs = now))
    }

    @Test
    fun `due exactly when the weekly interval elapses`() {
        val lastCheck = now - IntegrityCheckPolicy.CHECK_INTERVAL_MS
        assertTrue(IntegrityCheckPolicy.isDue(lastCheckMs = lastCheck, nowMs = now))
    }

    @Test
    fun `due once the interval has been exceeded`() {
        val lastCheck = now - IntegrityCheckPolicy.CHECK_INTERVAL_MS - 60_000L
        assertTrue(IntegrityCheckPolicy.isDue(lastCheckMs = lastCheck, nowMs = now))
    }

    @Test
    fun `freshly checked is not due`() {
        assertFalse(IntegrityCheckPolicy.isDue(lastCheckMs = now - 1, nowMs = now))
    }
}
