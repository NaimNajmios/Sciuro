package com.sciuro.core.ledger.repository

import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.db.SciuroDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

open class RawEventRepository(
    private val database: SciuroDatabase
) {
    open suspend fun persistRawEvent(
        id: String,
        sourceType: String,
        sourcePackageOrAddress: String,
        title: String,
        text: String,
        timestamp: Long,
        capturedAt: Long = System.currentTimeMillis()
    ) {
        database.rawEventStagingQueries.insertRawEvent(
            id = id,
            source_type = sourceType,
            source_package_or_address = sourcePackageOrAddress,
            title = title,
            text = text,
            timestamp = timestamp,
            captured_at = capturedAt
        )
    }

    open suspend fun markProcessing(id: String, error: String? = null) {
        database.rawEventStagingQueries.markProcessing(error, id)
    }

    open suspend fun markProcessed(id: String) {
        database.rawEventStagingQueries.markProcessed(System.currentTimeMillis(), id)
    }

    open suspend fun markDeadLetter(id: String, error: String) {
        database.rawEventStagingQueries.markDeadLetter(error, id)
    }

    open fun observePendingEvents(): Flow<List<Raw_event_staging>> {
        return database.rawEventStagingQueries.selectPendingEvents()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    open fun observeDeadLetterEvents(): Flow<List<Raw_event_staging>> {
        return database.rawEventStagingQueries.selectDeadLetterEvents()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    open suspend fun getLastCapturedAt(): Long? {
        return database.rawEventStagingQueries.selectLastCapturedAt().executeAsOneOrNull()?.last_captured_at
    }

    open suspend fun countPending(): Long {
        return database.rawEventStagingQueries.countPending().executeAsOne()
    }

    open suspend fun getRawEventById(id: String): Raw_event_staging? {
        return database.rawEventStagingQueries.selectRawEventById(id).executeAsOneOrNull()
    }

    open suspend fun countDeadLetter(): Long {
        return database.rawEventStagingQueries.countDeadLetter().executeAsOne()
    }

    open suspend fun getStrandedEvents(staleProcessedBeforeMs: Long): List<Raw_event_staging> {
        return database.rawEventStagingQueries.selectStrandedEvents(staleProcessedBeforeMs).executeAsList()
    }

    open suspend fun countStrandedEvents(staleProcessedBeforeMs: Long): Long {
        return database.rawEventStagingQueries.selectStrandedEventsCount(staleProcessedBeforeMs).executeAsOne()
    }

    open suspend fun requeueRawEvent(id: String) {
        database.rawEventStagingQueries.requeueRawEvent(id)
    }

    open suspend fun purgeOldTraces(beforeMs: Long) {
        database.pipelineTraceQueries.deleteTraceOlderThan(beforeMs)
    }
}
