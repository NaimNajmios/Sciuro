package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.DEFAULT_CONFIDENCE_THRESHOLD
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.rule.ParserRule

class SimulationEngine(
    private val rules: List<ParserRule>,
    private val llmFallbackParser: LlmFallbackParser?,
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    suspend fun simulate(event: RawEvent): SimulationResult {
        val allRuleResults = rules.map { rule ->
            val matches = rule.matches(event)
            val draft = if (matches) rule.extract(event) else null
            RuleMatchResult(
                ruleName = rule::class.simpleName ?: "Unknown",
                packageName = event.sourcePackageOrAddress,
                matches = matches,
                extractedDraft = draft
            )
        }

        val deterministicResult = allRuleResults.firstOrNull { it.extractedDraft != null }
        val deterministicDraft = deterministicResult?.extractedDraft

        var llmDraft: StructuredDraft? = null
        var llmLatencyMs: Long? = null
        var error: String? = null
        var usedLlmFallback = false

        if (llmFallbackParser != null && (deterministicDraft == null || deterministicDraft.confidenceScore < confidenceThreshold)) {
            val startTime = System.currentTimeMillis()
            try {
                llmDraft = llmFallbackParser.parse(event)
                llmLatencyMs = System.currentTimeMillis() - startTime
                usedLlmFallback = true
            } catch (e: Exception) {
                llmLatencyMs = System.currentTimeMillis() - startTime
                error = e.message
            }
        }

        val finalDraft = llmDraft ?: deterministicDraft

        val debugCapture = llmFallbackParser?.lastDebugCapture

        return SimulationResult(
            packageName = event.sourcePackageOrAddress,
            title = event.title,
            text = event.text,
            matchedRule = deterministicResult?.ruleName,
            allRuleResults = allRuleResults,
            deterministicDraft = deterministicDraft,
            llmDraft = llmDraft,
            finalDraft = finalDraft,
            usedLlmFallback = usedLlmFallback,
            llmLatencyMs = llmLatencyMs,
            llmDebugInfo = debugCapture?.let {
                LlmDebugInfo(
                    prompt = it.prompt,
                    rawResponse = it.rawResponse,
                    latencyMs = it.latencyMs,
                    error = null,
                    modelUsed = it.modelUsed
                )
            },
            error = error
        )
    }
}
