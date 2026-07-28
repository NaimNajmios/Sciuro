package com.sciuro.core.ingestion.engine

/**
 * Filter to determine whether a notification from an aggregator package (like Gmail, Outlook, SMS apps)
 * should be processed by the ingestion engine, or dropped early as spam/non-financial.
 */
object AggregatorHeuristicFilter {

    private val financialKeywordsPattern: Regex by lazy {
        val keywords = listOf(
            "rm",
            "m2u",
            "cimb notification",
            "funds received",
            "transfer",
            "receipt",
            "duitnow",
            "jompay",
            "fpx",
            "spaylater",
            "bayaran",
            "pemindahan",
            "ditolak",
            "transaksi",
            "berjaya",
            "kredit",
            "debit",
            "paid"
        )
        val regexString = keywords.joinToString(separator = "|") { "\\b$it\\b" }
        Regex(regexString, RegexOption.IGNORE_CASE)
    }

    private val promotionalKeywordsPattern: Regex by lazy {
        val keywords = listOf(
            "promo", "promotion", "diskop", "diskaun", "offer", "tawaran",
            "discount", "coupon", "voucher", "cashback", "rewards",
            "limited", "exclusive", "selamatkan", "jimat",
            "only rm", "hanya rm", "from rm", "dari rm"
        )
        val regexString = keywords.joinToString(separator = "|") { "\\b$it\\b" }
        Regex(regexString, RegexOption.IGNORE_CASE)
    }

    private val transactionKeywordsPattern: Regex by lazy {
        val keywords = listOf(
            "transaction", "payment", "received", "spent", "dibelanjakan",
            "credited", "dikreditkan", "debited", "didebitkan",
            "deducted", "ditolak", "masuk", "transfer from", "transfer to",
            "paid to", "dibayar", "bayaran kepada", "successful", "berjaya"
        )
        val regexString = keywords.joinToString(separator = "|") { "\\b$it\\b" }
        Regex(regexString, RegexOption.IGNORE_CASE)
    }

    fun isFinancial(title: String, text: String): Boolean {
        val combinedText = "$title $text"
        if (!financialKeywordsPattern.containsMatchIn(combinedText)) return false
        if (promotionalKeywordsPattern.containsMatchIn(combinedText) &&
            !transactionKeywordsPattern.containsMatchIn(combinedText)) return false
        return true
    }
}
