package com.sciuro.core.parsing.model

enum class TransactionDirection {
    INFLOW, OUTFLOW
}

data class StructuredDraft(
    val amount: Double,
    val direction: TransactionDirection,
    val merchant: String?, 
    val accountOrChannel: String?, 
    val referenceId: String?, 
    val timestamp: Long, 
    val isConfident: Boolean 
)
