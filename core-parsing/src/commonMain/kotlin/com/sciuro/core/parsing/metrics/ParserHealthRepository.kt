package com.sciuro.core.parsing.metrics

import com.sciuro.core.ledger.db.SciuroDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParserHealthRow(
    val packageName: String,
    val total: Long,
    val processed: Long,
    val deadLetter: Long,
    val lastCapturedAt: Long?
) {
    val matchRate: Double
        get() = if (total > 0) processed.toDouble() / total.toDouble() else 0.0
}

class ParserHealthRepository(
    private val database: SciuroDatabase
) {
    suspend fun getMatchRatesSince(sinceMs: Long): List<ParserHealthRow> {
        return withContext(Dispatchers.Default) {
            database.rawEventStagingQueries.selectMatchRateByPackage(sinceMs)
                .executeAsList()
                .mapNotNull { row ->
                    if (row.total == 0L) return@mapNotNull null
                    ParserHealthRow(
                        packageName = row.package_name,
                        total = row.total,
                        processed = row.processed ?: 0L,
                        deadLetter = row.dead_letter ?: 0L,
                        lastCapturedAt = row.last_captured_at
                    )
                }
        }
    }

    suspend fun getMatchRatesInWindow(sinceMs: Long, untilMs: Long): List<ParserHealthRow> {
        return withContext(Dispatchers.Default) {
            database.rawEventStagingQueries.selectMatchRateByPackageRecent(sinceMs, untilMs)
                .executeAsList()
                .mapNotNull { row ->
                    if (row.total == 0L) return@mapNotNull null
                    ParserHealthRow(
                        packageName = row.package_name,
                        total = row.total,
                        processed = row.processed ?: 0L,
                        deadLetter = row.dead_letter ?: 0L,
                        lastCapturedAt = row.last_captured_at
                    )
                }
        }
    }
}
