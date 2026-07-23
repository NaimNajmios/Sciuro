package com.sciuro.feature.kanban.model

import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtType

data class DebtTask(
    val id: String,
    val name: String,
    val counterpartyName: String?,
    val type: DebtType,
    val direction: DebtDirection,
    val principalAmount: Double,
    val remainingBalance: Double,
    val debt: Debt
) {
    val progress: Float get() = if (principalAmount > 0) ((principalAmount - remainingBalance) / principalAmount).toFloat() else 0f

    companion object {
        fun fromDebt(debt: Debt): DebtTask = DebtTask(
            id = debt.id,
            name = debt.name,
            counterpartyName = debt.counterpartyName,
            type = debt.type,
            direction = debt.direction,
            principalAmount = debt.principalAmount,
            remainingBalance = debt.remainingBalance,
            debt = debt
        )
    }
}
