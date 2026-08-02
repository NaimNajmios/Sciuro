package com.sciuro.core.audit.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DomainEventBus(
    private val eventStore: DomainEventStore? = null,
    private val maxRetries: Int = 5,
    private val leaseDurationMs: Long = 60_000L
) {
    private val _events = MutableSharedFlow<DomainEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private val liveDropCount = AtomicLong(0)
    private val subscriberJobs = ConcurrentHashMap<String, Job>()

    suspend fun publish(event: DomainEvent) {
        val store = eventStore
        if (store != null) {
            val envelope = DomainEventEnvelope(
                eventId = DomainEventCodec.newEventId(),
                sequence = System.currentTimeMillis(),
                event = event,
                eventType = DomainEventCodec.eventTypeOf(event),
                occurredAt = System.currentTimeMillis(),
                critical = DomainEventCodec.isCritical(DomainEventCodec.eventTypeOf(event))
            )
            store.append(envelope)
        }
        _events.emit(event)
    }

    fun subscribe(
        subscriberId: String,
        scope: CoroutineScope,
        handler: suspend (DomainEvent) -> Unit
    ): Job {
        val store = eventStore
        requireNotNull(store) { "Durable subscription requires an event store" }

        val existingJob = subscriberJobs[subscriberId]
        if (existingJob?.isActive == true) return existingJob

        val job = scope.launch {
            val now = System.currentTimeMillis()
            processPendingEvents(subscriberId, store, now, handler)

            _events.collect { event ->
                val claimNow = System.currentTimeMillis()
                val envelope = DomainEventEnvelope(
                    eventId = DomainEventCodec.newEventId(),
                    sequence = claimNow,
                    event = event,
                    eventType = DomainEventCodec.eventTypeOf(event),
                    occurredAt = claimNow,
                    critical = DomainEventCodec.isCritical(DomainEventCodec.eventTypeOf(event))
                )
                store.append(envelope)
                processSingleEvent(subscriberId, store, envelope, handler)
            }
        }
        subscriberJobs[subscriberId] = job
        return job
    }

    private suspend fun processPendingEvents(
        subscriberId: String,
        store: DomainEventStore,
        now: Long,
        handler: suspend (DomainEvent) -> Unit
    ) {
        val pending = store.getPendingEvents()
        for (envelope in pending) {
            processSingleEvent(subscriberId, store, envelope, handler)
        }
    }

    private suspend fun processSingleEvent(
        subscriberId: String,
        store: DomainEventStore,
        envelope: DomainEventEnvelope,
        handler: suspend (DomainEvent) -> Unit
    ) {
        val claimNow = System.currentTimeMillis()
        val claimed = store.claimNext(subscriberId, claimNow, leaseDurationMs)
            ?: return

        try {
            handler(claimed.event)
            val ackNow = System.currentTimeMillis()
            store.acknowledge(subscriberId, claimed.eventId, ackNow)
        } catch (e: Exception) {
            val error = e.message ?: e::class.simpleName ?: "Unknown error"
            val retryAt = System.currentTimeMillis() + leaseDurationMs
            store.recordFailure(subscriberId, claimed.eventId, error, retryAt)
        }
    }

    suspend fun metrics(): DomainEventMetrics {
        val storeMetrics = eventStore?.metrics() ?: DomainEventMetrics(
            pendingCount = 0,
            deadLetterCount = 0,
            retryCount = 0,
            oldestPendingAgeMs = 0,
            subscriberLagMs = 0,
            liveDropCount = 0
        )
        return storeMetrics.copy(liveDropCount = liveDropCount.get())
    }

    fun shutdown() {
        subscriberJobs.values.forEach { it.cancel() }
        subscriberJobs.clear()
    }
}
