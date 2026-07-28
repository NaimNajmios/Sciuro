package com.sciuro.core.parsing.util

import com.sciuro.core.parsing.model.TransactionDirection

val amountRegex = """RM\s*([\d,]+(?:\.\d{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)

private val outflowMerchantRegex = """(?:to|at|kepada|paid to|dibayar kepada)\s+([A-Za-z0-9\s&@.'-]+?)(?:\s+for|\s+on|\s+was successful|\s+adalah berjaya|\s+pada|\.(?=\s|$)|$)""".toRegex(RegexOption.IGNORE_CASE)

private val inflowMerchantRegex = """(?:from|dari|daripada)\s+([A-Za-z0-9\s&@.'-]+?)(?:\s+for|\s+on|\.(?=\s|$)|$)""".toRegex(RegexOption.IGNORE_CASE)

private val accountNumberRegex = """(?:A/C|Account|Acc)[\s.:]*(?:no\.?)?\s*([\d*Xx]{4,20})""".toRegex(RegexOption.IGNORE_CASE)
private val endingAccountNumberRegex = """(?:ending|berakhir)\s+([\d*Xx]{4,20})""".toRegex(RegexOption.IGNORE_CASE)

fun extractAmount(text: String): Double? {
    val amountStr = amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")
    return amountStr?.toDoubleOrNull()
}

private val merchantBlacklist = listOf(
    "your account",
    "akaun anda",
    "account ending",
    "akaun berakhir",
    "account no"
)

fun extractMerchant(text: String): String? {
    val extracted = outflowMerchantRegex.find(text)?.groupValues?.get(1)?.trim()
        ?: inflowMerchantRegex.find(text)?.groupValues?.get(1)?.trim()
        
    if (extracted != null) {
        val lowerExtracted = extracted.lowercase()
        if (merchantBlacklist.any { lowerExtracted.contains(it) }) {
            return null
        }
    }
    return extracted
}

fun extractAccountNumber(text: String): String? {
    return accountNumberRegex.find(text)?.groupValues?.get(1)?.trim()
        ?: endingAccountNumberRegex.find(text)?.groupValues?.get(1)?.trim()
}

fun matchesAccountSuffix(extracted: String, stored: String): Boolean {
    val normalizedExtracted = extracted.filter { it.isDigit() }
    val normalizedStored = stored.filter { it.isDigit() }
    if (normalizedExtracted.isEmpty() || normalizedStored.isEmpty()) return false
    val len = minOf(normalizedExtracted.length, normalizedStored.length)
    return normalizedExtracted.takeLast(len) == normalizedStored.takeLast(len)
}

private val outflowKeywords = listOf("deducted", "ditolak", "payment to", "bayaran kepada", "transferred", "paid", "debited", "used at", "spent", "didebitkan", "membayar", "payment", "transaction of", "transaksi sebanyak", "duitnow to")
private val inflowKeywords = listOf("credited", "received", "masuk", "dikreditkan", "deposit", "deposited", "salary", "refund", "credited to", "received from")

fun detectDirection(text: String, title: String): TransactionDirection? {
    val combined = "$title $text".lowercase()
    
    val isOutflow = outflowKeywords.any { combined.contains(it) }
    val isInflow = inflowKeywords.any { combined.contains(it) }
    
    return when {
        isOutflow && !isInflow -> TransactionDirection.OUTFLOW
        isInflow && !isOutflow -> TransactionDirection.INFLOW
        else -> null
    }
}
