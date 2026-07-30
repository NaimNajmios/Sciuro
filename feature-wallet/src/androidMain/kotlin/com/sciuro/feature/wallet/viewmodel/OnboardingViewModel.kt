package com.sciuro.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.model.Account
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.budget.model.Budget
import com.sciuro.core.budget.model.BudgetPeriod
import com.sciuro.core.budget.repository.BudgetRepository

enum class OnboardingStep {
    Welcome,
    WalletSetup,
    Categories,
    Budget,
    Complete
}

data class OnboardingState(
    val isLoading: Boolean = true,
    val isOnboardingComplete: Boolean = false,
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val expenseCategories: List<com.sciuro.core.ledger.model.Category> = emptyList(),
    val incomeCategories: List<com.sciuro.core.ledger.model.Category> = emptyList()
)

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    init {
        viewModelScope.launch {
            categoryRepository.seedCategories()
            val accounts = accountRepository.observeAccounts().first()
            if (accounts.isNotEmpty() && !settingsProvider.hasCompletedOnboarding()) {
                settingsProvider.setHasCompletedOnboarding(true)
            }
        }
    }

    private val _currentStep = MutableStateFlow(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    val state: StateFlow<OnboardingState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeCategoriesByType("OUTFLOW"),
        categoryRepository.observeCategoriesByType("INFLOW"),
        currentStep
    ) { accounts, expenseCats, incomeCats, step ->
        val fullyComplete = accounts.isNotEmpty() && settingsProvider.hasCompletedOnboarding()
        OnboardingState(
            isLoading = false,
            isOnboardingComplete = fullyComplete,
            currentStep = if (fullyComplete) OnboardingStep.Complete else step,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OnboardingState())

    fun advanceToWalletSetup() {
        _currentStep.value = OnboardingStep.WalletSetup
    }

    fun advanceToCategories() {
        _currentStep.value = OnboardingStep.Categories
    }

    fun advanceToBudget() {
        _currentStep.value = OnboardingStep.Budget
    }

    fun setupPersonalWallet(initialBalance: Double) {
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first()
            if (accounts.isEmpty()) {
                accountRepository.createAccount(
                    Account(
                        id = "default-${currentTimeMillis()}",
                        name = "Personal Wallet",
                        type = "Cash",
                        currency = "MYR",
                        balance = initialBalance,
                        associatedPackage = null,
                        isSystem = true
                    )
                )
            }
            advanceToCategories()
        }
    }

    fun saveCategoryPreferences(enabledCategoryIds: Set<String>) {
        viewModelScope.launch {
            val allCats = categoryRepository.observeCategories().first()
            allCats.forEach { cat ->
                if (cat.id !in enabledCategoryIds) {
                    categoryRepository.deleteCategory(cat.id)
                }
            }
            advanceToBudget()
        }
    }

    fun createInitialBudgets(budgets: List<Pair<String, Double>>) {
        viewModelScope.launch {
            budgets.forEach { (categoryId, amount) ->
                budgetRepository.createBudget(
                    Budget(
                        id = "budget-${categoryId}-${currentTimeMillis()}",
                        categoryId = categoryId,
                        allocatedAmount = amount,
                        currentSpent = 0.0,
                        period = BudgetPeriod.MONTHLY,
                        rollover = false,
                        alertThresholdPercent = 80.0
                    )
                )
            }
            completeOnboarding()
        }
    }

    fun skipBudgets() {
        viewModelScope.launch {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        settingsProvider.setHasCompletedOnboarding(true)
        _currentStep.value = OnboardingStep.Complete
    }
}
