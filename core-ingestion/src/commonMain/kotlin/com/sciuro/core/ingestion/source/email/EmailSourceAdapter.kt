package com.sciuro.core.ingestion.source.email

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.IngestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class EmailSourceAdapter : IngestionSource {

    override val sourceType: SourceType = SourceType.EMAIL

    private val _events = MutableSharedFlow<RawEvent>(extraBufferCapacity = 50)

    override fun observeEvents(): Flow<RawEvent> = _events

    suspend fun emitEmail(event: RawEvent) {
        _events.emit(event)
    }
}
