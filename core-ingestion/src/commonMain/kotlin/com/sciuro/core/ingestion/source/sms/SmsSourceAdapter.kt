package com.sciuro.core.ingestion.source.sms

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.IngestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SmsSourceAdapter : IngestionSource {

    override val sourceType: SourceType = SourceType.SMS

    private val _events = MutableSharedFlow<RawEvent>(extraBufferCapacity = 50)

    override fun observeEvents(): Flow<RawEvent> = _events

    suspend fun emitSms(event: RawEvent) {
        _events.emit(event)
    }
}
