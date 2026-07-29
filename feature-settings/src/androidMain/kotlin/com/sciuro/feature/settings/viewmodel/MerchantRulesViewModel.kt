package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.model.Category
import com.sciuro.core.ledger.model.MerchantRuleUiModel
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.repository.MerchantRuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MerchantRulesUiState(
    val rules: List<MerchantRuleUiModel> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
) {
    companion object {
        val Loading = MerchantRulesUiState(isLoading = true)
        val Empty = MerchantRulesUiState(isLoading = false)
    }
}

class MerchantRulesViewModel(
    private val merchantRuleRepository: MerchantRuleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MerchantRulesUiState.Loading)
    val state: StateFlow<MerchantRulesUiState> = _state.asStateFlow()

    init {
        val rulesFlow = merchantRuleRepository.observeAllRules()
        val categoriesFlow = categoryRepository.observeCategories()
        viewModelScope.launch {
            combine(rulesFlow, categoriesFlow) { rules, categories ->
                MerchantRulesUiState(
                    rules = rules,
                    categories = categories,
                    isLoading = false
                )
            }.collect { uiState ->
                _state.value = uiState
            }
        }
    }

    fun deleteRule(merchantKey: String) {
        viewModelScope.launch {
            merchantRuleRepository.deleteRule(merchantKey)
        }
    }

    fun overrideRule(merchantKey: String, newCategoryId: String) {
        viewModelScope.launch {
            merchantRuleRepository.overrideRule(merchantKey, newCategoryId)
        }
    }
}
