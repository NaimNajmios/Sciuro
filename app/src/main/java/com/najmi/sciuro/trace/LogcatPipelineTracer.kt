package com.najmi.sciuro.trace

import android.util.Log
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase

class LogcatPipelineTracer(
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
        val tag = "SciuroTrace"
        val eventPrefix = rawEventId?.take(8) ?: "no_id"
        val suffix = if (durationMs != null) " (${durationMs}ms)" else ""
        val confPrefix = if (confidence != null) " [conf:${"%.2f".format(confidence)}]" else ""

        when (outcome) {
            TraceOutcome.FAILURE, TraceOutcome.DROP ->
                Log.w(tag, "[$eventPrefix] $stage → $outcome$suffix$confPrefix ${detail?.toString() ?: ""}")
            else ->
                Log.d(tag, "[$eventPrefix] $stage → $outcome$suffix$confPrefix")
        }

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
