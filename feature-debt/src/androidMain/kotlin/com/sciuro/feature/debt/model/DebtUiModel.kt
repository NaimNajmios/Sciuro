package com.sciuro.feature.debt.model

import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.model.DebtType

data class DebtUiModel(
    val id: String,
    val name: String,
    val type: DebtType,
    val direction: DebtDirection,
    val counterpartyName: String?,
    val status: DebtStatus,
    val principalAmount: Double,
    val remainingBalance: Double,
    val interestRate: Double?,
    val dueDate: Long?,
    val associatedAccountId: String?,
    val notes: String?
) {
    val progress: Float
        get() = if (principalAmount > 0) ((principalAmount - remainingBalance) / principalAmount).toFloat() else 0f

    val isSettled: Boolean get() = status == DebtStatus.PAID_OFF
}
