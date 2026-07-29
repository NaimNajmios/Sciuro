package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.investment.engine.InvestmentValuationEngine
import com.sciuro.core.investment.repository.InvestmentRepository
import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class InvestmentPriceItem(
    val id: String,
    val assetSymbol: String,
    val assetName: String,
    val assetType: String,
    val bookValue: Double,
    val liveValue: Double,
    val manualPrice: String = "",
    val hasManualOverride: Boolean = false
)

data class InvestmentPriceUiState(
    val items: List<InvestmentPriceItem> = emptyList(),
    val isLoading: Boolean = true
)

class InvestmentPriceViewModel(
    private val investmentRepository: InvestmentRepository,
    private val investmentValuationEngine: InvestmentValuationEngine,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _state = MutableStateFlow(InvestmentPriceUiState(isLoading = true))
    val state: StateFlow<InvestmentPriceUiState> = _state.asStateFlow()

    init {
        loadInvestments()
    }

    fun loadInvestments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val investments = investmentRepository.observeInvestments().first()
                val items = investments.map { inv ->
                    val liveValue = try {
                        investmentValuationEngine.getCurrentValue(inv.id)
                    } catch (_: Exception) { 0.0 }
                    val key = "investment_price_${inv.assetType}_${inv.assetSymbol}"
                    val manualPrice = settingsProvider.getManualPrice(key)
                    InvestmentPriceItem(
                        id = inv.id,
                        assetSymbol = inv.assetSymbol,
                        assetName = inv.assetName,
                        assetType = inv.assetType,
                        bookValue = inv.unitsHeld * inv.averageBuyPrice,
                        liveValue = liveValue,
                        manualPrice = if (manualPrice != null) manualPrice.toString() else "",
                        hasManualOverride = manualPrice != null && manualPrice > 0.0
                    )
                }
                _state.value = InvestmentPriceUiState(items = items, isLoading = false)
            } catch (_: Exception) {
                _state.value = InvestmentPriceUiState(items = emptyList(), isLoading = false)
            }
        }
    }

    fun setManualPrice(assetType: String, assetSymbol: String, price: Double) {
        viewModelScope.launch {
            val key = "investment_price_${assetType}_${assetSymbol}"
            val existing = settingsProvider.getManualPrice(key)
            val idx = _state.value.items.indexOfFirst { it.assetSymbol == assetSymbol && it.assetType == assetType }
            if (idx >= 0) {
                val old = _state.value.items[idx]
                val updated = old.copy(
                    manualPrice = price.toString(),
                    hasManualOverride = true
                )
                val items = _state.value.items.toMutableList()
                items[idx] = updated
                _state.value = _state.value.copy(items = items)
            }
            if (existing != null && existing == price) return@launch
            settingsProvider.setManualPrice(key, price)
            loadInvestments()
        }
    }

    fun clearManualPrice(assetType: String, assetSymbol: String) {
        viewModelScope.launch {
            val key = "investment_price_${assetType}_${assetSymbol}"
            settingsProvider.setManualPrice(key, 0.0)
            loadInvestments()
        }
    }
}
