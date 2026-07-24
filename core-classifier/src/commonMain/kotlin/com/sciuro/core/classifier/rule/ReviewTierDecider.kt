package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.model.ReviewTier
import com.sciuro.core.ledger.db.SciuroDatabase

class ReviewTierDecider(
    private val database: SciuroDatabase,
    private val silentConfidenceThreshold: Float = 0.95f,
    private val autoConfidenceThreshold: Float = 0.7f,
    private val autoConfirmEnabled: Boolean = false
) {
    suspend fun decide(
        confidence: Float,
        categoryId: String?,
        accountId: String?,
        merchant: String?
    ): ReviewTier {
        if (!autoConfirmEnabled) return ReviewTier.MANUAL

        val hasCategory = categoryId != null
        val hasAccount = accountId != null

        if (confidence >= silentConfidenceThreshold && hasCategory && hasAccount && hasLearnedRule(merchant)) {
            return ReviewTier.AUTO_SILENT
        }

        if (confidence >= autoConfidenceThreshold && hasCategory && hasAccount) {
            return ReviewTier.AUTO_UNDO
        }

        return ReviewTier.MANUAL
    }

    private suspend fun hasLearnedRule(merchant: String?): Boolean {
        if (merchant == null) return false
        val key = merchant.lowercase().trim()
        return database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(key)
            .executeAsOneOrNull() != null
    }
}
