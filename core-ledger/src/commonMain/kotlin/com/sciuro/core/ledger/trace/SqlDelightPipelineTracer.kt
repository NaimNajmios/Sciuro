package com.sciuro.core.ledger.trace

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase

class SqlDelightPipelineTracer(
    private val database: SciuroDatabase
) : PipelineTracer {

    override suspend fun trace(
        rawEventId: String?,
        transactionId: String?,
        stage: TraceStage,
        outcome: TraceOutcome,
        durationMs: Long?,
        confidence: Float?,
        detail: Map<String, String?>?
    ) {
        database.pipelineTraceQueries.insertTrace(
            id = generateUuid(),
            raw_event_id = rawEventId,
            transaction_id = transactionId,
            stage = stage.name,
            outcome = outcome.name,
            duration_ms = durationMs,
            confidence = confidence?.toDouble(),
            detail_json = detail?.toString(),
            created_at = currentTimeMillis()
        )
    }
}
