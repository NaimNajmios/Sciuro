package com.sciuro.core.parsing.util

val amountRegex = """RM\s*([\d,]+(?:\.\d{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)

private val outflowMerchantRegex = """(?:to|at|kepada|paid to)\s+([A-Za-z0-9\s&@.'-]+?)(?:\s+for|\s+on|\s+was successful|\s+adalah berjaya|\s+pada|\.|$)""".toRegex(RegexOption.IGNORE_CASE)

private val inflowMerchantRegex = """(?:from|dari)\s+([A-Za-z0-9\s&@.'-]+?)(?:\s+for|\s+on|\.|$)""".toRegex(RegexOption.IGNORE_CASE)

fun extractAmount(text: String): Double? {
    val amountStr = amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")
    return amountStr?.toDoubleOrNull()
}

fun extractMerchant(text: String): String? {
    return outflowMerchantRegex.find(text)?.groupValues?.get(1)?.trim()
        ?: inflowMerchantRegex.find(text)?.groupValues?.get(1)?.trim()
}
