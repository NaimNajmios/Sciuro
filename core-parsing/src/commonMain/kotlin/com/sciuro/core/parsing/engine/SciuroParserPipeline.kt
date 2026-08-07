package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.ParseResult
import com.sciuro.core.parsing.model.StructuredDraft

import com.sciuro.core.parsing.model.DEFAULT_CONFIDENCE_THRESHOLD

class SciuroParserPipeline(
    private val deterministicParser: DeterministicParser,
    private val llmFallbackParser: LlmFallbackParser,
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    suspend fun process(event: RawEvent): ParseResult {
        val deterministicResult = deterministicParser.parse(event)

        if (deterministicResult != null && deterministicResult.confidenceScore >= confidenceThreshold) {
            return ParseResult.Success(deterministicResult)
        }

        val llmResult = llmFallbackParser.parse(event)

        if (llmResult != null) return ParseResult.Success(llmResult)

        deterministicResult?.let {
            return ParseResult.Success(it.copy(isUntrustedFallback = true))
        }

        return when (llmFallbackParser.lastVerdict) {
            "not_a_transaction" -> ParseResult.NotATransaction
            else -> ParseResult.Failure(llmFallbackParser.lastVerdict ?: "parser_failure")
        }
    }
}
