package com.sciuro.core.audit.trace

enum class TraceStage {
    CAPTURE,
    STAGING,
    PARSE_REGEX,
    PARSE_LLM,
    DEDUP,
    CATEGORIZE,
    ACCOUNT_MATCH,
    BOOK,
    ENGINE,
    EVENT
}
