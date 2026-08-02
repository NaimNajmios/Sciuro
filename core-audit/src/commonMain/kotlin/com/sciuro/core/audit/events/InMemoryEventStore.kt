package com.sciuro.core.audit.events

class InMemoryEventStore : DomainEventStore {

    private val events = mutableListOf<DomainEventEnvelope>()
    private val deliveryState = mutableMapOf<String, MutableMap<String, DeliveryEntry>>()

    data class DeliveryEntry(
        var status: String = "PENDING",
        var attempts: Int = 0,
        var leaseUntil: Long = 0L,
        var lastError: String? = null,
        var availableAt: Long = 0L,
        var acknowledgedAt: Long? = null
    )

    override suspend fun append(event: DomainEventEnvelope) {
        synchronized(events) {
            events.add(event)
        }
    }

    override suspend fun claimNext(subscriberId: String, now: Long, leaseMs: Long): DomainEventEnvelope? {
        synchronized(events) {
            for (event in events) {
                val subMap = deliveryState.getOrPut(event.eventId) { mutableMapOf() }
                val entry = subMap.getOrPut(subscriberId) { DeliveryEntry() }

                if (entry.status == "ACKNOWLEDGED" || entry.status == "DEAD_LETTER") continue
                if (entry.status == "PROCESSING" && entry.leaseUntil > now) continue

                entry.status = "PROCESSING"
                entry.attempts++
                entry.leaseUntil = now + leaseMs
                return event
            }
        }
        return null
    }

    override suspend fun acknowledge(subscriberId: String, eventId: String, now: Long) {
        synchronized(events) {
            val subMap = deliveryState[eventId] ?: return
            subMap[subscriberId]?.let { it.status = "ACKNOWLEDGED"; it.acknowledgedAt = now }
        }
    }

    override suspend fun recordFailure(subscriberId: String, eventId: String, error: String, retryAt: Long) {
        synchronized(events) {
            val subMap = deliveryState.getOrPut(eventId) { mutableMapOf() }
            val entry = subMap.getOrPut(subscriberId) { DeliveryEntry() }
            entry.status = "RETRYABLE"
            entry.lastError = error
            entry.availableAt = retryAt
        }
    }

    override suspend fun deadLetter(subscriberId: String, eventId: String, error: String, now: Long) {
        synchronized(events) {
            val subMap = deliveryState.getOrPut(eventId) { mutableMapOf() }
            val entry = subMap.getOrPut(subscriberId) { DeliveryEntry() }
            entry.status = "DEAD_LETTER"
            entry.lastError = error
        }
    }

    override suspend fun reclaimExpiredLeases(now: Long): Int {
        var count = 0
        synchronized(events) {
            for ((_, subMap) in deliveryState) {
                for ((_, entry) in subMap) {
                    if (entry.status == "PROCESSING" && entry.leaseUntil < now) {
                        entry.status = "RETRYABLE"
                        entry.availableAt = now
                        count++
                    }
                }
            }
        }
        return count
    }

    override suspend fun getPendingEvents(): List<DomainEventEnvelope> {
        synchronized(events) {
            return events.filter { event ->
                val subMap = deliveryState[event.eventId] ?: return@filter true
                subMap.values.none { it.status == "ACKNOWLEDGED" }
            }
        }
    }

    override suspend fun metrics(): DomainEventMetrics {
        val now = System.currentTimeMillis()
        var pendingCount = 0L
        var deadLetterCount = 0L
        var retryCount = 0L
        var oldestPendingAge = 0L
        var maxLag = 0L

        synchronized(events) {
            for ((eventId, subMap) in deliveryState) {
                val event = events.find { it.eventId == eventId } ?: continue
                val age = now - event.occurredAt
                for ((_, entry) in subMap) {
                    when (entry.status) {
                        "PROCESSING" -> {
                            pendingCount++
                            if (age > maxLag) maxLag = age
                        }
                        "RETRYABLE" -> retryCount++
                        "DEAD_LETTER" -> deadLetterCount++
                        "PENDING" -> {
                            pendingCount++
                            if (age > maxLag) maxLag = age
                        }
                    }
                }
            }
            if (events.isNotEmpty()) {
                val oldestEvent = events.minByOrNull { it.occurredAt }
                if (oldestEvent != null) oldestPendingAge = now - oldestEvent.occurredAt
            }
        }

        return DomainEventMetrics(
            pendingCount = pendingCount,
            deadLetterCount = deadLetterCount,
            retryCount = retryCount,
            oldestPendingAgeMs = oldestPendingAge,
            subscriberLagMs = maxLag,
            liveDropCount = 0
        )
    }

    override suspend fun cleanupAcknowledged(olderThanMs: Long): Int {
        var count = 0
        synchronized(events) {
            val now = System.currentTimeMillis()
            val cutoff = now - olderThanMs
            val toRemove = events.filter { event ->
                val subMap = deliveryState[event.eventId] ?: return@filter false
                val allAcked = subMap.values.all { it.status == "ACKNOWLEDGED" && (it.acknowledgedAt ?: 0L) < cutoff }
                allAcked
            }
            for (event in toRemove) {
                events.removeAll { it.eventId == event.eventId }
                deliveryState.remove(event.eventId)
                count++
            }
        }
        return count
    }
}
