package com.sciuro.core.audit.events

data class DomainEventMetrics(
    val pendingCount: Long,
    val deadLetterCount: Long,
    val retryCount: Long,
    val oldestPendingAgeMs: Long,
    val subscriberLagMs: Long,
    val liveDropCount: Long
)

interface DomainEventStore {
    suspend fun append(event: DomainEventEnvelope)

    suspend fun claimNext(subscriberId: String, now: Long, leaseMs: Long): DomainEventEnvelope?

    suspend fun acknowledge(subscriberId: String, eventId: String, now: Long)

    suspend fun recordFailure(
        subscriberId: String,
        eventId: String,
        error: String,
        retryAt: Long
    )

    suspend fun deadLetter(subscriberId: String, eventId: String, error: String, now: Long)

    suspend fun reclaimExpiredLeases(now: Long): Int

    suspend fun getPendingEvents(): List<DomainEventEnvelope>

    suspend fun metrics(): DomainEventMetrics

    suspend fun cleanupAcknowledged(olderThanMs: Long): Int
}
