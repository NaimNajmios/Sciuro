package com.sciuro.core.parsing.model

enum class TransactionDirection {
    INFLOW, OUTFLOW
}

const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f

data class StructuredDraft(
    val amount: Double,
    val direction: TransactionDirection?,
    val merchant: String?,
    val accountOrChannel: String?,
    val referenceId: String?,
    val timestamp: Long,
    val confidenceScore: Float = 1.0f
) {
    val isConfident get() = confidenceScore >= DEFAULT_CONFIDENCE_THRESHOLD
}
