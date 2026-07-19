package com.sciuro.core.debt.model

enum class DebtType {
    LOAN, CREDIT_CARD, MONEY_OWED
}

data class Debt(
    val id: String,
    val name: String,
    val type: DebtType,
    val principalAmount: Double,
    val remainingBalance: Double,
    val interestRate: Double?,
    val dueDate: Long?,
    val associatedAccountId: String?
)
