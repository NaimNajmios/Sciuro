package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.feature.wallet.model.WalletAccount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.sciuro.core.ledger.engine.ReconciliationEngine
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.investment.repository.InvestmentRepository
import com.sciuro.core.investment.model.Investment

import com.sciuro.core.ledger.repository.CategoryRepository

class WalletViewModel(
    private val accountRepository: AccountRepository,
    private val reconciliationEngine: ReconciliationEngine,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    val accounts: StateFlow<List<WalletAccount>> = accountRepository.observeAccounts()
        .map { accounts ->
            accounts.map {
                WalletAccount(
                    id = it.id,
                    name = it.name,
                    balance = it.balance,
                    isEWallet = it.type.lowercase().contains("ewallet") || it.type.lowercase().contains("e-wallet"),
                    type = it.type,
                    associatedPackage = it.associated_package
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val investments: StateFlow<List<Investment>> = investmentRepository.observeInvestments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val allTransactions: StateFlow<List<com.sciuro.core.ledger.db.Transaction_record>> = transactionRepository.observeAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val expenseCategories: StateFlow<List<com.sciuro.core.ledger.model.Category>> = categoryRepository.observeCategoriesByType("OUTFLOW")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incomeCategories: StateFlow<List<com.sciuro.core.ledger.model.Category>> = categoryRepository.observeCategoriesByType("INFLOW")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    fun addAccount(name: String, type: String, associatedPackage: String, initialBalance: Double) {
        viewModelScope.launch {
            accountRepository.createAccount(
                com.sciuro.core.ledger.model.Account(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    balance = initialBalance,
                    associatedPackage = associatedPackage.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun updateAccount(id: String, name: String, type: String, associatedPackage: String, balance: Double) {
        viewModelScope.launch {
            accountRepository.updateAccount(
                com.sciuro.core.ledger.model.Account(
                    id = id,
                    name = name,
                    type = type,
                    balance = balance, // Deprecated usage for balance, preserved for model integrity
                    associatedPackage = associatedPackage.takeIf { it.isNotBlank() }
                )
            )
            reconciliationEngine.reconcileAccount(id, balance)
        }
    }
    
    fun deleteAccount(id: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(id)
        }
    }
    
    fun addTransaction(accountId: String, amount: Double, direction: String, merchant: String, categoryId: String?) {
        viewModelScope.launch {
            transactionRepository.bookTransaction(
                Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    direction = direction,
                    merchant = merchant.takeIf { it.isNotBlank() },
                    timestamp = currentTimeMillis(),
                    referenceId = null,
                    isReviewed = true // User manual entry is implicitly reviewed
                ),
                source = com.sciuro.core.audit.model.AuditSource.USER_MANUAL
            )
        }
    }
    
    fun editTransaction(transactionId: String, amount: Double, merchant: String, categoryId: String?, accountId: String?) {
        viewModelScope.launch {
            transactionRepository.editTransaction(
                transactionId = transactionId,
                newAmount = amount,
                newMerchant = merchant,
                newCategoryId = categoryId,
                newAccountId = accountId
            )
        }
    }
    
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
        }
    }
    
    fun addInvestment(assetSymbol: String, assetName: String, assetType: String, unitsHeld: Double, averageBuyPrice: Double, associatedAccountId: String?) {
        viewModelScope.launch {
            investmentRepository.createInvestment(
                Investment(
                    id = java.util.UUID.randomUUID().toString(),
                    assetSymbol = assetSymbol,
                    assetName = assetName,
                    assetType = assetType,
                    unitsHeld = unitsHeld,
                    averageBuyPrice = averageBuyPrice,
                    associatedAccountId = associatedAccountId.takeIf { !it.isNullOrBlank() }
                )
            )
        }
    }
    
    fun updateInvestment(id: String, assetSymbol: String, assetName: String, assetType: String, unitsHeld: Double, averageBuyPrice: Double, associatedAccountId: String?) {
        viewModelScope.launch {
            investmentRepository.updateInvestment(
                Investment(
                    id = id,
                    assetSymbol = assetSymbol,
                    assetName = assetName,
                    assetType = assetType,
                    unitsHeld = unitsHeld,
                    averageBuyPrice = averageBuyPrice,
                    associatedAccountId = associatedAccountId.takeIf { !it.isNullOrBlank() }
                )
            )
        }
    }
    
    fun deleteInvestment(id: String) {
        viewModelScope.launch {
            investmentRepository.deleteInvestment(id)
        }
    }
}
