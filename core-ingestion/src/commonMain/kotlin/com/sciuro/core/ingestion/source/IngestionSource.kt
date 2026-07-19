package com.sciuro.core.ingestion.source

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import kotlinx.coroutines.flow.Flow

interface IngestionSource {
    val sourceType: SourceType
    
    /**
     * Exposes a continuous stream of raw financial events from this source.
     */
    fun observeEvents(): Flow<RawEvent>
}
