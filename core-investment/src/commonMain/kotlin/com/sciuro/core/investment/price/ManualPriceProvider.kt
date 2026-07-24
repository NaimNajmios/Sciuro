package com.sciuro.core.investment.price

import com.sciuro.core.ledger.config.SettingsProvider

class ManualPriceProvider(
    private val settingsProvider: SettingsProvider
) : PriceProvider {
    override suspend fun getCurrentPricePerUnit(assetType: String, assetSymbol: String): Double? {
        val key = "investment_price_${assetType}_${assetSymbol}"
        return settingsProvider.getManualPrice(key)
    }

    override suspend fun refresh() { /* no-op: prices set manually */}

    fun setManualPrice(assetType: String, assetSymbol: String, price: Double) {
        settingsProvider.setManualPrice("investment_price_${assetType}_${assetSymbol}", price)
    }
}
