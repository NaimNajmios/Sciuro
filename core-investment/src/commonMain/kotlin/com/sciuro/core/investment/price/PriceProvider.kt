package com.sciuro.core.investment.price

interface PriceProvider {
    suspend fun getCurrentPricePerUnit(assetType: String, assetSymbol: String): Double?
    suspend fun refresh()
}
