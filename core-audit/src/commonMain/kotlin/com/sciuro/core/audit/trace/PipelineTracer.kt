package com.sciuro.core.audit.trace

interface PipelineTracer {
    suspend fun trace(
        rawEventId: String?,
        transactionId: String?,
        stage: TraceStage,
        outcome: TraceOutcome,
        durationMs: Long? = null,
        confidence: Float? = null,
        detail: Map<String, String?>? = null
    )
}
