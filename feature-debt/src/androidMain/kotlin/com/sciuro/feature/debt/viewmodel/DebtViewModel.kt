package com.sciuro.feature.debt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.debt.model.Debt
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.model.DebtType
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.feature.debt.model.DebtUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DebtViewModel(
    private val debtRepository: DebtRepository
) : ViewModel() {

    private fun List<Debt>.toUiModel(): List<DebtUiModel> = map {
        DebtUiModel(
            id = it.id,
            name = it.name,
            type = it.type,
            direction = it.direction,
            counterpartyName = it.counterpartyName,
            status = it.status,
            principalAmount = it.principalAmount,
            remainingBalance = it.remainingBalance,
            interestRate = it.interestRate,
            dueDate = it.dueDate,
            associatedAccountId = it.associatedAccountId,
            notes = it.notes
        )
    }

    val debts: StateFlow<List<DebtUiModel>> = debtRepository.observeDebts()
        .map { it.toUiModel() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtsIOwe: StateFlow<List<DebtUiModel>> = debtRepository.observeDebtsByDirection(DebtDirection.I_OWE)
        .map { it.toUiModel() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtsOwedToMe: StateFlow<List<DebtUiModel>> = debtRepository.observeDebtsByDirection(DebtDirection.OWED_TO_ME)
        .map { it.toUiModel() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createDebt(
        name: String,
        type: DebtType,
        direction: DebtDirection,
        principalAmount: Double,
        counterpartyName: String?,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.createDebt(
                Debt(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    direction = direction,
                    counterpartyName = counterpartyName,
                    status = DebtStatus.ACTIVE,
                    principalAmount = principalAmount,
                    remainingBalance = principalAmount,
                    notes = notes
                )
            )
        }
    }

    fun updateDebt(debt: DebtUiModel, name: String, principalAmount: Double, remainingBalance: Double, counterpartyName: String?, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.updateDebt(
                Debt(
                    id = debt.id,
                    name = name,
                    type = debt.type,
                    direction = debt.direction,
                    counterpartyName = counterpartyName,
                    status = debt.status,
                    principalAmount = principalAmount,
                    remainingBalance = remainingBalance,
                    interestRate = debt.interestRate,
                    dueDate = debt.dueDate,
                    associatedAccountId = debt.associatedAccountId,
                    notes = notes
                )
            )
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.deleteDebt(id)
        }
    }

    fun markAsPaidOff(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.markAsPaidOff(id)
        }
    }

    fun recordPayment(debtId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.applyPayment(debtId, amount)
        }
    }
}
