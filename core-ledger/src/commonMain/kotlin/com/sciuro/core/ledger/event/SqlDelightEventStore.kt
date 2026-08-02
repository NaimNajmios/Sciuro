package com.sciuro.core.ledger.event

import com.sciuro.core.audit.events.DomainEventCodec
import com.sciuro.core.audit.events.DomainEventEnvelope
import com.sciuro.core.audit.events.DomainEventMetrics
import com.sciuro.core.audit.events.DomainEventStore
import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightEventStore(
    private val database: SciuroDatabase
) : DomainEventStore {

    override suspend fun append(event: DomainEventEnvelope) = withContext(Dispatchers.Default) {
        val payloadJson = DomainEventCodec.serialize(event.event)
        val criticalInt = if (event.critical) 1L else 0L
        database.domainEventLogQueries.insertEvent(
            event_id = event.eventId,
            sequence = event.sequence,
            event_type = event.eventType,
            schema_version = event.schemaVersion.toLong(),
            payload_json = payloadJson,
            occurred_at = event.occurredAt,
            producer = event.producer,
            critical = criticalInt
        )
    }

    override suspend fun claimNext(subscriberId: String, now: Long, leaseMs: Long): DomainEventEnvelope? =
        withContext(Dispatchers.Default) {
            val leaseUntil = now + leaseMs

            val pending = database.domainEventLogQueries.selectUnacknowledgedBySubscriber(subscriberId)
                .executeAsList()

            for (row in pending) {
                database.domainEventDeliveryQueries.insertDeliveryIfNotExists(
                    event_id = row.event_id,
                    subscriber_id = subscriberId
                )

                database.domainEventDeliveryQueries.claimEvent(
                    lease_until = leaseUntil,
                    event_id = row.event_id,
                    subscriber_id = subscriberId
                )

                return@withContext DomainEventEnvelope(
                    eventId = row.event_id,
                    sequence = row.sequence,
                    event = DomainEventCodec.deserialize(
                        row.event_type,
                        row.payload_json
                    ) ?: return@withContext null,
                    eventType = row.event_type,
                    schemaVersion = row.schema_version.toInt(),
                    occurredAt = row.occurred_at,
                    producer = row.producer,
                    critical = row.critical == 1L
                )
            }

            return@withContext null
        }

    override suspend fun acknowledge(subscriberId: String, eventId: String, now: Long) =
        withContext(Dispatchers.Default) {
            database.domainEventDeliveryQueries.acknowledgeDelivery(
                acknowledged_at = now,
                event_id = eventId,
                subscriber_id = subscriberId
            )
        }

    override suspend fun recordFailure(
        subscriberId: String,
        eventId: String,
        error: String,
        retryAt: Long
    ) = withContext(Dispatchers.Default) {
        database.domainEventDeliveryQueries.recordDeliveryFailure(
            last_error = error,
            available_at = retryAt,
            event_id = eventId,
            subscriber_id = subscriberId
        )
    }

    override suspend fun deadLetter(subscriberId: String, eventId: String, error: String, now: Long) =
        withContext(Dispatchers.Default) {
            database.domainEventDeliveryQueries.deadLetterDelivery(
                last_error = error,
                event_id = eventId,
                subscriber_id = subscriberId
            )
        }

    override suspend fun reclaimExpiredLeases(now: Long): Int = withContext(Dispatchers.Default) {
        database.domainEventDeliveryQueries.reclaimExpiredLeases(
            available_at = now,
            lease_until = now
        )
        0
    }

    override suspend fun getPendingEvents(): List<DomainEventEnvelope> = withContext(Dispatchers.Default) {
        val rows = database.domainEventLogQueries.selectUnacknowledgedBySubscriber("_global")
            .executeAsList()

        rows.map { row ->
            DomainEventEnvelope(
                eventId = row.event_id,
                sequence = row.sequence,
                event = DomainEventCodec.deserialize(row.event_type, row.payload_json)
                    ?: return@withContext emptyList(),
                eventType = row.event_type,
                schemaVersion = row.schema_version.toInt(),
                occurredAt = row.occurred_at,
                producer = row.producer,
                critical = row.critical == 1L
            )
        }
    }

    override suspend fun metrics(): DomainEventMetrics = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()

        val pendingCount = database.domainEventDeliveryQueries.countAllPending()
            .executeAsOne()

        val deadLetterCount = database.domainEventDeliveryQueries.countDeadLetters()
            .executeAsOne()

        val retryCount = database.domainEventDeliveryQueries.countRetries()
            .executeAsOne()

        val oldestRow = database.domainEventDeliveryQueries.oldestPendingEvent()
            .executeAsOneOrNull()

        val oldestPendingAgeMs = if (oldestRow?.oldest_at != null) {
            now - oldestRow.oldest_at
        } else 0L

        DomainEventMetrics(
            pendingCount = pendingCount,
            deadLetterCount = deadLetterCount,
            retryCount = retryCount,
            oldestPendingAgeMs = oldestPendingAgeMs,
            subscriberLagMs = oldestPendingAgeMs,
            liveDropCount = 0
        )
    }

    override suspend fun cleanupAcknowledged(olderThanMs: Long): Int = withContext(Dispatchers.Default) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        database.domainEventDeliveryQueries.cleanupAcknowledged(cutoff)
        database.domainEventLogQueries.deleteEventsOlderThan(cutoff)
        0
    }
}
