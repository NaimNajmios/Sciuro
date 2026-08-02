package com.sciuro.core.audit.events

import kotlinx.serialization.Serializable

@Serializable
data class DomainEventEnvelope(
    val eventId: String,
    val sequence: Long,
    val event: DomainEvent,
    val eventType: String,
    val schemaVersion: Int = 1,
    val occurredAt: Long,
    val producer: String? = null,
    val critical: Boolean = false
)
