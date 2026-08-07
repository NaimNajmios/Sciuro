package com.sciuro.core.parsing.model

sealed interface ParseResult {
    data class Success(val draft: StructuredDraft) : ParseResult

    /** The message is not a financial transaction (e.g. a promo). Suppressed from user-facing activity. */
    data object NotATransaction : ParseResult

    /** A financial-looking message could not be parsed (parser failure). Surfaced as a dropped event. */
    data class Failure(val reason: String) : ParseResult
}
