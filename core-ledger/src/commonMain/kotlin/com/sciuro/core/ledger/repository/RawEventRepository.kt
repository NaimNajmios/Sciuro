package com.sciuro.core.ledger.repository

import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.db.SciuroDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class RawEventRepository(
    private val database: SciuroDatabase
) {
    suspend fun persistRawEvent(
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

    suspend fun markProcessing(id: String, error: String? = null) {
        database.rawEventStagingQueries.markProcessing(error, id)
    }

    suspend fun markProcessed(id: String) {
        database.rawEventStagingQueries.markProcessed(System.currentTimeMillis(), id)
    }

    suspend fun markDeadLetter(id: String, error: String) {
        database.rawEventStagingQueries.markDeadLetter(error, id)
    }

    fun observePendingEvents(): Flow<List<Raw_event_staging>> {
        return database.rawEventStagingQueries.selectPendingEvents()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    fun observeDeadLetterEvents(): Flow<List<Raw_event_staging>> {
        return database.rawEventStagingQueries.selectDeadLetterEvents()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    suspend fun getLastCapturedAt(): Long? {
        return database.rawEventStagingQueries.selectLastCapturedAt().executeAsOneOrNull()?.last_captured_at
    }

    suspend fun countPending(): Long {
        return database.rawEventStagingQueries.countPending().executeAsOne()
    }

    suspend fun countDeadLetter(): Long {
        return database.rawEventStagingQueries.countDeadLetter().executeAsOne()
    }
}
