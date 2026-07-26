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
            "transaction",
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
            "payment",
            "received",
            "paid"
        )
        // Match any of these keywords with word boundaries, case-insensitive
        val regexString = keywords.joinToString(separator = "|") { "\\b$it\\b" }
        Regex(regexString, RegexOption.IGNORE_CASE)
    }

    /**
     * Very lightweight heuristic to drop obvious spam or non-financial emails/SMS.
     * We look for common keywords found in banking email subjects/titles and SMS messages.
     *
     * @param title The title of the notification
     * @param text The text body of the notification
     * @return true if it contains financial keywords and should be processed
     */
    fun isFinancial(title: String, text: String): Boolean {
        val combinedText = "$title $text"
        return financialKeywordsPattern.containsMatchIn(combinedText)
    }
}
