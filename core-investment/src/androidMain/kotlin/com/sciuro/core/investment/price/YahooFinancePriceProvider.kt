package com.sciuro.core.investment.price

import com.sciuro.core.ledger.config.SettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class YahooFinancePriceProvider(
    private val settingsProvider: SettingsProvider
) : PriceProvider {

    private val cache = mutableMapOf<String, CachedPrice>()
    private val cacheTtlMs = 15L * 60L * 1000L

    private data class CachedPrice(val price: Double, val timestamp: Long)

    override suspend fun getCurrentPricePerUnit(assetType: String, assetSymbol: String): Double? {
        val cacheKey = "${assetType}_${assetSymbol}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return cached.price
        }

        val manualPrice = settingsProvider.getManualPrice("investment_price_${assetType}_${assetSymbol}")
        if (manualPrice != null) return manualPrice

        return try {
            val price = fetchPrice(assetType, assetSymbol)
            if (price != null) {
                cache[cacheKey] = CachedPrice(price, System.currentTimeMillis())
            }
            price
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun refresh() {
        cache.clear()
    }

    private suspend fun fetchPrice(assetType: String, assetSymbol: String): Double? {
        return withContext(Dispatchers.IO) {
            when {
                assetType.uppercase() == "GOLD" -> fetchGoldPrice()
                assetSymbol.endsWith(".KL") -> fetchYahooFinance("${assetSymbol.removeSuffix(".KL")}.KL")
                assetSymbol.contains(".") -> fetchYahooFinance(assetSymbol)
                else -> fetchYahooFinance("$assetSymbol.KL")
            }
        }
    }

    private fun fetchGoldPrice(): Double? {
        return try {
            val url = URL("https://api.metals.live/v1/spot/gold")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val pricePerOunce = body.trim().trim('{', '}', '"').split(":").lastOrNull()
                ?.trim('"')?.toDoubleOrNull() ?: return null

            val gramPrice = pricePerOunce / 31.1035
            val myrRate = getUsdToMyrRate()
            gramPrice * myrRate
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchYahooFinance(symbol: String): Double? {
        return try {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val regex = Regex("\"regularMarketPrice\":\\s*(\\d+\\.?\\d*)")
            val match = regex.find(body)
            match?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun getUsdToMyrRate(): Double {
        return try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) return 4.60

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val regex = Regex("\"MYR\":\\s*(\\d+\\.?\\d*)")
            val match = regex.find(body)
            match?.groupValues?.get(1)?.toDoubleOrNull() ?: 4.60
        } catch (e: Exception) {
            4.60
        }
    }
}
