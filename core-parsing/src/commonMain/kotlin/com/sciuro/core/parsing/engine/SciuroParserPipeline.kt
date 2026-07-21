package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft

import com.sciuro.core.parsing.model.DEFAULT_CONFIDENCE_THRESHOLD

class SciuroParserPipeline(
    private val deterministicParser: DeterministicParser,
    private val llmFallbackParser: LlmFallbackParser,
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    suspend fun process(event: RawEvent): StructuredDraft? {
        val deterministicResult = deterministicParser.parse(event)

        if (deterministicResult != null && deterministicResult.confidenceScore >= confidenceThreshold) {
            return deterministicResult
        }

        val llmResult = llmFallbackParser.parse(event)

        return llmResult ?: deterministicResult
    }
}
