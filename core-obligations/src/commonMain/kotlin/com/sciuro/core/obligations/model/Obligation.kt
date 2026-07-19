package com.sciuro.core.obligations.model

enum class ObligationFrequency {
    WEEKLY, MONTHLY, YEARLY
}

data class Obligation(
    val id: String,
    val name: String,
    val amount: Double,
    val frequency: ObligationFrequency,
    val nextDueDate: Long,
    val categoryId: String?,
    val accountId: String?,
    val isActive: Boolean
)
