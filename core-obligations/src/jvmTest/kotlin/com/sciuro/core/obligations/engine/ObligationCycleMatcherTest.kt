package com.sciuro.core.obligations.engine

import com.sciuro.core.ledger.db.Transaction
import com.sciuro.core.parsing.model.TransactionDirection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ObligationCycleMatcherTest {

    private fun createTx(amount: Double, timestamp: Long): Transaction {
        return Transaction(
            id = "tx_${timestamp}",
            amount = amount,
            direction = TransactionDirection.OUTFLOW,
            merchant = "Netflix",
            accountOrChannel = "CIMB",
            referenceId = null,
            counterpartyAccountNumber = null,
            timestamp = timestamp,
            createdAt = timestamp
        )
    }

    @Test
    fun `detects perfect monthly cycle`() {
        // 3 consecutive months exactly 30 days apart
        val dayMs = 24 * 60 * 60 * 1000L
        val tx1 = createTx(45.0, 1000L * dayMs)
        val tx2 = createTx(45.0, 1030L * dayMs)
        val tx3 = createTx(45.0, 1060L * dayMs)

        val match = ObligationCycleMatcher.findCycle(listOf(tx1, tx2, tx3))
        assertNotNull(match)
        assertEquals(30, match.intervalDays)
        assertEquals(1.0, match.confidence)
        assertEquals(3, match.matchedCount)
    }

    @Test
    fun `detects cycle with one missing occurrence`() {
        val dayMs = 24 * 60 * 60 * 1000L
        // Missing the 30-day tx
        val tx1 = createTx(45.0, 1000L * dayMs)
        val tx3 = createTx(45.0, 1060L * dayMs)
        val tx4 = createTx(45.0, 1090L * dayMs)

        val match = ObligationCycleMatcher.findCycle(listOf(tx1, tx3, tx4))
        assertNotNull(match)
        assertEquals(30, match.intervalDays)
        assertEquals(3, match.matchedCount)
        // Confidence should be slightly less because of the missing cycle
    }

    @Test
    fun `ignores random daily intervals`() {
        val dayMs = 24 * 60 * 60 * 1000L
        val tx1 = createTx(45.0, 1000L * dayMs)
        val tx2 = createTx(45.0, 1002L * dayMs)
        val tx3 = createTx(45.0, 1005L * dayMs)
        val tx4 = createTx(45.0, 1012L * dayMs)

        val match = ObligationCycleMatcher.findCycle(listOf(tx1, tx2, tx3, tx4))
        // Should not detect a reliable cycle among these random short intervals
        assertNull(match)
    }
    
    @Test
    fun `requires at least 3 transactions to form a cycle`() {
        val dayMs = 24 * 60 * 60 * 1000L
        val tx1 = createTx(45.0, 1000L * dayMs)
        val tx2 = createTx(45.0, 1030L * dayMs)
        
        val match = ObligationCycleMatcher.findCycle(listOf(tx1, tx2))
        assertNull(match)
    }
}
