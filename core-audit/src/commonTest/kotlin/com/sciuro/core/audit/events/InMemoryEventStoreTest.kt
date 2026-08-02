package com.sciuro.core.audit.events

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryEventStoreTest {

    private val store = InMemoryEventStore()

    private fun createEnvelope(
        id: String = "evt_1",
        eventType: String = "TransactionCategorized",
        occurredAt: Long = System.currentTimeMillis(),
        critical: Boolean = false
    ) = DomainEventEnvelope(
        eventId = id,
        sequence = occurredAt,
        event = DomainEvent.TransactionCategorized(
            transactionId = "tx1", categoryId = "cat1",
            confidence = 0.9, source = "REGEX", merchant = null
        ),
        eventType = eventType,
        occurredAt = occurredAt,
        critical = critical
    )

    @Test
    fun appendAndClaimReturnsTheEvent() {
        runBlocking {
            val envelope = createEnvelope()
            store.append(envelope)

            val claimed = store.claimNext("sub1", System.currentTimeMillis(), 60_000)
            assertNotNull(claimed)
            assertEquals("evt_1", claimed.eventId)
        }
    }

    @Test
    fun claimReturnsNullWhenNoEventsExist() {
        runBlocking {
            val claimed = store.claimNext("sub1", System.currentTimeMillis(), 60_000)
            assertNull(claimed)
        }
    }

    @Test
    fun acknowledgePreventsReClaimBySameSubscriber() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 60_000)
            assertNotNull(claimed)
            store.acknowledge("sub1", claimed.eventId, now + 1)

            val reClaimed = store.claimNext("sub1", now + 2, 60_000)
            assertNull(reClaimed)
        }
    }

    @Test
    fun independentSubscribersCanClaimSameEvent() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            val claimed1 = store.claimNext("sub1", now, 60_000)
            val claimed2 = store.claimNext("sub2", now, 60_000)

            assertNotNull(claimed1)
            assertNotNull(claimed2)
            assertEquals(claimed1.eventId, claimed2.eventId)
        }
    }

    @Test
    fun expiredLeaseAllowsReclaim() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 1000)
            assertNotNull(claimed)

            val reClaimed = store.claimNext("sub1", now + 2000, 60_000)
            assertNotNull(reClaimed)
        }
    }

    @Test
    fun recordFailureSetsStatusToRetryable() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 60_000)
            assertNotNull(claimed)
            store.recordFailure("sub1", claimed.eventId, "error", now + 5000)

            val metrics = store.metrics()
            assertEquals(1, metrics.retryCount)
        }
    }

    @Test
    fun deadLetterPreventsReClaim() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 60_000)
            assertNotNull(claimed)
            store.deadLetter("sub1", claimed.eventId, "error", now)

            val reClaimed = store.claimNext("sub1", now + 1, 60_000)
            assertNull(reClaimed)

            val metrics = store.metrics()
            assertEquals(1, metrics.deadLetterCount)
        }
    }

    @Test
    fun reclaimExpiredLeasesResetsExpiredProcessingStates() {
        runBlocking {
            store.append(createEnvelope())
            val now = System.currentTimeMillis()

            store.claimNext("sub1", now, 1000)
            val reclaimed = store.reclaimExpiredLeases(now + 2000)

            assertEquals(1, reclaimed)
            val reClaimed = store.claimNext("sub1", now + 2001, 60_000)
            assertNotNull(reClaimed)
        }
    }

    @Test
    fun getPendingEventsExcludesAcknowledged() {
        runBlocking {
            store.append(createEnvelope(id = "e1"))
            store.append(createEnvelope(id = "e2"))
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 60_000)
            assertNotNull(claimed)
            store.acknowledge("sub1", claimed.eventId, now)

            val pending = store.getPendingEvents()
            assertEquals(1, pending.size)
            assertEquals("e2", pending[0].eventId)
        }
    }

    @Test
    fun cleanupAcknowledgedRemovesOldAcknowledgedEvents() {
        runBlocking {
            store.append(createEnvelope(id = "e1"))
            val now = System.currentTimeMillis()

            val claimed = store.claimNext("sub1", now, 60_000)
            assertNotNull(claimed)
            store.acknowledge("sub1", claimed.eventId, now - 100_000)

            val cleaned = store.cleanupAcknowledged(60_000)
            assertEquals(1, cleaned)
            assertEquals(0, store.getPendingEvents().size)
        }
    }

    @Test
    fun metricsReportsCorrectCounts() {
        runBlocking {
            store.append(createEnvelope(id = "e1"))
            store.append(createEnvelope(id = "e2"))
            store.append(createEnvelope(id = "e3"))
            val now = System.currentTimeMillis()

            store.claimNext("sub1", now, 60_000)
            store.claimNext("sub1", now, 60_000)
            val third = store.claimNext("sub1", now, 60_000)
            assertNotNull(third)
            store.acknowledge("sub1", third.eventId, now)

            val metrics = store.metrics()
            assertEquals(2, metrics.pendingCount)
            assertEquals(0, metrics.deadLetterCount)
            assertEquals(0, metrics.retryCount)
        }
    }

    @Test
    fun claimSkipsAcknowledgedEvents() {
        runBlocking {
            store.append(createEnvelope(id = "e1"))
            store.append(createEnvelope(id = "e2"))
            val now = System.currentTimeMillis()

            val first = store.claimNext("sub1", now, 60_000)
            assertNotNull(first)
            store.acknowledge("sub1", first.eventId, now)

            val second = store.claimNext("sub1", now + 1, 60_000)
            assertNotNull(second)
            assertEquals("e2", second.eventId)
        }
    }
}
