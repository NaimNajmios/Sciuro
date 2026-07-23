package com.sciuro.core.parsing.engine

import com.sciuro.core.parsing.model.StructuredDraft

data class SimulationResult(
    val packageName: String,
    val title: String,
    val text: String,
    val matchedRule: String?,
    val allRuleResults: List<RuleMatchResult>,
    val deterministicDraft: StructuredDraft?,
    val llmDraft: StructuredDraft?,
    val finalDraft: StructuredDraft?,
    val usedLlmFallback: Boolean,
    val llmLatencyMs: Long?,
    val llmDebugInfo: LlmDebugInfo?,
    val error: String?
) {
    val llmPackageMarker: String?
        get() = if (usedLlmFallback && matchedRule == null) {
            "$packageName | ${text.take(80).replace(Regex("[0-9]"), "\$0")}"
        } else null
}

data class RuleMatchResult(
    val ruleName: String,
    val packageName: String,
    val matches: Boolean,
    val extractedDraft: StructuredDraft?
)

data class LlmDebugInfo(
    val prompt: String?,
    val rawResponse: String?,
    val latencyMs: Long?,
    val error: String?,
    val modelUsed: String?
)
