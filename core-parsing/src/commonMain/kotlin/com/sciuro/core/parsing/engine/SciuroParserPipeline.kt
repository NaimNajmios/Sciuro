package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft

class SciuroParserPipeline(
    private val deterministicParser: DeterministicParser,
    private val llmFallbackParser: LlmFallbackParser
) {
    suspend fun process(event: RawEvent): StructuredDraft? {
        val deterministicResult = deterministicParser.parse(event)
        
        if (deterministicResult != null && deterministicResult.isConfident) {
            return deterministicResult
        }
        
        val llmResult = llmFallbackParser.parse(event)
        
        return llmResult ?: deterministicResult
    }
}
