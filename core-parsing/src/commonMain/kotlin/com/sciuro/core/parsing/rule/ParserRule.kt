package com.sciuro.core.parsing.rule

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft

interface ParserRule {
    fun matches(event: RawEvent): Boolean
    fun extract(event: RawEvent): StructuredDraft?
}
