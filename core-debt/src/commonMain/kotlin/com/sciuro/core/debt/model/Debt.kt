package com.sciuro.core.debt.model

enum class DebtType {
    LOAN, CREDIT_CARD, MONEY_OWED
}

enum class DebtDirection {
    I_OWE, OWED_TO_ME
}

enum class DebtStatus {
    ACTIVE, PAID_OFF, ARCHIVED
}

data class Debt(
    val id: String,
    val name: String,
    val type: DebtType,
    val direction: DebtDirection = DebtDirection.I_OWE,
    val counterpartyName: String? = null,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val principalAmount: Double,
    val remainingBalance: Double,
    val interestRate: Double? = null,
    val dueDate: Long? = null,
    val associatedAccountId: String? = null,
    val notes: String? = null
)
