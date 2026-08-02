package com.sciuro.core.audit.events

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainEventCodecTest {

    @Test
    fun `serialize and deserialize roundtrip for DebtBalanceUpdated`() = runBlocking {
        val event = DomainEvent.DebtBalanceUpdated(debtId = "d1", newBalance = 5000.0, method = "PAYMENT")
        val json = DomainEventCodec.serialize(event)
        val eventType = DomainEventCodec.eventTypeOf(event)
        val deserialized = DomainEventCodec.deserialize(eventType, json)
        assertNotNull(deserialized)
        assertTrue(deserialized is DomainEvent.DebtBalanceUpdated)
        assertEquals("d1", deserialized.debtId)
        assertEquals(5000.0, deserialized.newBalance, 0.001)
        assertEquals("PAYMENT", deserialized.method)
    }

    @Test
    fun `serialize and deserialize roundtrip for TransactionCategorized`() = runBlocking {
        val event = DomainEvent.TransactionCategorized(
            transactionId = "tx1", categoryId = "cat_food",
            confidence = 0.95, source = "REGEX", merchant = "Restaurant"
        )
        val json = DomainEventCodec.serialize(event)
        val eventType = DomainEventCodec.eventTypeOf(event)
        val deserialized = DomainEventCodec.deserialize(eventType, json)
        assertNotNull(deserialized)
        assertTrue(deserialized is DomainEvent.TransactionCategorized)
        assertEquals("tx1", deserialized.transactionId)
        assertEquals("cat_food", deserialized.categoryId)
        assertEquals(0.95, deserialized.confidence, 0.001)
        assertEquals("Restaurant", deserialized.merchant)
    }

    @Test
    fun `serialize and deserialize roundtrip for NetPositionMilestoneReached`() = runBlocking {
        val event = DomainEvent.NetPositionMilestoneReached(netWorth = 15000.0, milestone = 10000.0)
        val json = DomainEventCodec.serialize(event)
        val eventType = DomainEventCodec.eventTypeOf(event)
        val deserialized = DomainEventCodec.deserialize(eventType, json)
        assertNotNull(deserialized)
        assertTrue(deserialized is DomainEvent.NetPositionMilestoneReached)
        assertEquals(15000.0, deserialized.netWorth, 0.001)
        assertEquals(10000.0, deserialized.milestone, 0.001)
    }

    @Test
    fun `isCritical returns true for DebtFullyPaidOff`() {
        assertTrue(DomainEventCodec.isCritical("DebtFullyPaidOff"))
    }

    @Test
    fun `isCritical returns true for NetPositionMilestoneReached`() {
        assertTrue(DomainEventCodec.isCritical("NetPositionMilestoneReached"))
    }

    @Test
    fun `isCritical returns false for TransactionCategorized`() {
        assertEquals(false, DomainEventCodec.isCritical("TransactionCategorized"))
    }

    @Test
    fun `deserialize returns null for unknown event type`() = runBlocking {
        val result = DomainEventCodec.deserialize("UnknownType", "{}")
        assertNull(result)
    }

    @Test
    fun `newEventId produces unique IDs`() {
        val id1 = DomainEventCodec.newEventId()
        val id2 = DomainEventCodec.newEventId()
        assertTrue(id1 != id2)
        assertTrue(id1.length == 36)
    }

    @Test
    fun `eventTypeOf returns correct type for all events`() {
        assertEquals("DebtFullyPaidOff", DomainEventCodec.eventTypeOf(DomainEvent.DebtFullyPaidOff("d1")))
        assertEquals("ObligationCreated", DomainEventCodec.eventTypeOf(DomainEvent.ObligationCreated("o1")))
        assertEquals("BudgetThresholdCrossed", DomainEventCodec.eventTypeOf(DomainEvent.BudgetThresholdCrossed("c1", 0.9)))
        assertEquals("MerchantRuleLearned", DomainEventCodec.eventTypeOf(DomainEvent.MerchantRuleLearned("merchant", "cat")))
        assertEquals("TransactionModified", DomainEventCodec.eventTypeOf(DomainEvent.TransactionModified("tx1")))
        assertEquals("NewFinanceAppDetected", DomainEventCodec.eventTypeOf(DomainEvent.NewFinanceAppDetected("com.app")))
    }
}
