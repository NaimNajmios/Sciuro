package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.model.ReviewTier
import com.sciuro.core.audit.model.TransactionIntent
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
        merchant: String?,
        intent: TransactionIntent? = null
    ): ReviewTier {
        val hasCategory = categoryId != null
        val hasAccount = accountId != null

        val knownIntentWithData = intent != null && intent !is TransactionIntent.Unknown

        if (knownIntentWithData && hasCategory) {
            return ReviewTier.AUTO_SILENT
        }

        if (confidence >= 1.0f) {
            return ReviewTier.AUTO_SILENT
        }

        if (!autoConfirmEnabled) return ReviewTier.MANUAL

        if (confidence >= silentConfidenceThreshold && hasCategory && hasAccount && hasLearnedRule(merchant)) {
            return ReviewTier.AUTO_SILENT
        }

        if (confidence >= autoConfidenceThreshold && hasCategory && hasAccount) {
            return ReviewTier.AUTO_UNDO
        }

        return ReviewTier.MANUAL
    }

    private suspend fun hasLearnedRule(merchant: String?): Boolean {
        if (merchant == null || merchant.isBlank()) return false
        val key = buildString { merchant.forEach { append(it.lowercaseChar()) } }.trim()
        val rule = database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(key)
            .executeAsOneOrNull()
        return rule != null && rule.confirmation_count >= 2
    }
}
