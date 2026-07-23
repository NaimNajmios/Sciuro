package com.sciuro.core.obligations.engine

import com.sciuro.core.ledger.db.SciuroDatabase

class ConfidenceTracker(
    private val database: SciuroDatabase
) {
    fun getConfirmationCount(merchant: String): Int {
        val normalized = merchant.lowercase().trim()
        val rule = database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(normalized)
            .executeAsOneOrNull()
        return rule?.confirmation_count?.toInt() ?: 0
    }

    fun isTrusted(merchant: String, threshold: Int): Boolean {
        return getConfirmationCount(merchant) >= threshold
    }
}
