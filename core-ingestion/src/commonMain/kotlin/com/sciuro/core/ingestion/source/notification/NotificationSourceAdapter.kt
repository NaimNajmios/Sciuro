package com.sciuro.core.ingestion.source.notification

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.IngestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class NotificationSourceAdapter : IngestionSource {

    override val sourceType: SourceType = SourceType.NOTIFICATION

    private val _events = MutableSharedFlow<RawEvent>(extraBufferCapacity = 100)

    override fun observeEvents(): Flow<RawEvent> = _events

    suspend fun emitNotification(event: RawEvent) {
        if (_events.subscriptionCount.value == 0) {
            println("[NotificationSourceAdapter] No collector attached — dropping event: ${event.id}")
        }
        _events.emit(event)
    }
}
