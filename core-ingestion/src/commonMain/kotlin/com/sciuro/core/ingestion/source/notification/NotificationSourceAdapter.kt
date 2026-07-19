package com.sciuro.core.ingestion.source.notification

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.IngestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Adapter that connects the Android NotificationListenerService to the Sciuro ingestion pipeline.
 */
class NotificationSourceAdapter : IngestionSource {
    
    override val sourceType: SourceType = SourceType.NOTIFICATION
    
    // Buffer raw events (e.g., if the parsing engine is temporarily busy)
    private val _events = MutableSharedFlow<RawEvent>(extraBufferCapacity = 100)
    
    override fun observeEvents(): Flow<RawEvent> = _events
    
    /**
     * Pushes a new raw event into the ingestion pipeline. 
     * This is typically called by the NotificationListenerService on `onNotificationPosted`.
     */
    suspend fun emitNotification(event: RawEvent) {
        _events.emit(event)
    }
}
