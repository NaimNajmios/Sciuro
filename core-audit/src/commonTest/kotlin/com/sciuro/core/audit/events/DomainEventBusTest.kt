package com.sciuro.core.audit.events

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainEventBusTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun publishPersistsToStoreAndEmitsToLiveFlow() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)
            val received = mutableListOf<DomainEvent>()
            val job = scope.launch {
                bus.events.collect { received.add(it) }
            }

            delay(50)
            bus.publish(DomainEvent.DebtFullyPaidOff("d1"))
            delay(100)
            job.cancel()

            assertEquals(1, received.size)
            assertTrue(received[0] is DomainEvent.DebtFullyPaidOff)

            val pending = store.getPendingEvents()
            assertEquals(1, pending.size)
        }
    }

    @Test
    fun publishWithoutStoreStillEmitsToLiveFlow() {
        runBlocking {
            val bus = DomainEventBus(eventStore = null)
            val received = mutableListOf<DomainEvent>()
            val job = scope.launch {
                bus.events.collect { received.add(it) }
            }

            delay(50)
            bus.publish(DomainEvent.ObligationCreated("o1"))
            delay(100)
            job.cancel()

            assertEquals(1, received.size)
            assertTrue(received[0] is DomainEvent.ObligationCreated)
        }
    }

    @Test
    fun durableSubscriberReceivesEventsAndAcknowledges() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)
            val received = CompletableDeferred<DomainEvent>()

            bus.subscribe("test_sub", scope) { event ->
                received.complete(event)
            }

            delay(50)
            bus.publish(DomainEvent.ObligationCreated("o1"))

            val event = withTimeout(2000) { received.await() }
            assertTrue(event is DomainEvent.ObligationCreated)
        }
    }

    @Test
    fun durableSubscriberReceivesHistoricalEventsOnStartup() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)

            bus.publish(DomainEvent.ObligationCreated("o1"))
            bus.publish(DomainEvent.DebtFullyPaidOff("d1"))

            val received = mutableListOf<DomainEvent>()
            bus.subscribe("test_sub", scope) { event ->
                synchronized(received) { received.add(event) }
            }

            delay(500)

            synchronized(received) {
                assertEquals(2, received.size)
            }
        }
    }

    @Test
    fun independentSubscribersReceiveSameEventIndependently() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)
            val sub1Received = CompletableDeferred<DomainEvent>()
            val sub2Received = CompletableDeferred<DomainEvent>()

            bus.subscribe("sub1", scope) { sub1Received.complete(it) }
            bus.subscribe("sub2", scope) { sub2Received.complete(it) }

            delay(50)
            bus.publish(DomainEvent.DebtFullyPaidOff("d1"))

            val event1 = withTimeout(2000) { sub1Received.await() }
            val event2 = withTimeout(2000) { sub2Received.await() }
            assertTrue(event1 is DomainEvent.DebtFullyPaidOff)
            assertTrue(event2 is DomainEvent.DebtFullyPaidOff)
        }
    }

    @Test
    fun subscribeIsIdempotentForSameSubscriberId() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)

            val job1 = bus.subscribe("sub1", scope) { }
            val job2 = bus.subscribe("sub1", scope) { }

            assertEquals(job1, job2)
        }
    }

    @Test
    fun metricsReflectsLiveDropCount() {
        runBlocking {
            val bus = DomainEventBus(eventStore = InMemoryEventStore())
            val metrics = bus.metrics()
            assertEquals(0, metrics.liveDropCount)
        }
    }

    @Test
    fun shutdownCancelsSubscriberJobs() {
        runBlocking {
            val store = InMemoryEventStore()
            val bus = DomainEventBus(eventStore = store)
            var called = false

            bus.subscribe("sub1", scope) { called = true }
            delay(50)
            bus.shutdown()
            delay(100)

            bus.publish(DomainEvent.ObligationCreated("o1"))
            delay(200)
            assertEquals(false, called)
        }
    }
}
