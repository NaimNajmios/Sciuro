package com.sciuro.core.parsing.engine

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.rule.ParserRule

class DeterministicParser(
    private val rules: List<ParserRule>
) {
    fun parse(event: RawEvent): StructuredDraft? {
        for (rule in rules) {
            if (rule.matches(event)) {
                val draft = rule.extract(event)
                if (draft != null) return draft
            }
        }
        return null
    }
}
