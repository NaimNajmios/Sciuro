package com.sciuro.core.ingestion.source

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class MultiplexIngestionSource(
    private val adapters: List<IngestionSource>
) : IngestionSource {

    override val sourceType: SourceType = SourceType.NOTIFICATION

    override fun observeEvents(): Flow<RawEvent> {
        val flows = adapters.map { it.observeEvents() }
        return if (flows.isEmpty()) {
            kotlinx.coroutines.flow.emptyFlow()
        } else {
            merge(*flows.toTypedArray())
        }
    }
}
