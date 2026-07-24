package com.sciuro.core.classifier.rule

import com.sciuro.core.ledger.db.SciuroDatabase

class CategoryResolver(
    private val database: SciuroDatabase
) {
    suspend fun resolve(merchant: String?): String? {
        if (merchant == null) return null
        val normalizedKey = merchant.lowercase().trim()

        val learnedRule = database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(normalizedKey)
            .executeAsOneOrNull()
        if (learnedRule != null) {
            return learnedRule.category_id
        }

        return guessFromStaticHeuristic(merchant)
    }

    companion object {
        private const val CAT_DINING = "cat_exp_1"
        private const val CAT_TRANSPORT = "cat_exp_2"
        private const val CAT_UTILITIES = "cat_exp_3"
        private const val CAT_GROCERIES = "cat_exp_6"

        fun guessFromStaticHeuristic(merchant: String): String? {
            val lower = merchant.lowercase()
            return when {
                lower.contains("starbucks") || lower.contains("mcdonalds") || lower.contains("kfc") || lower.contains("burger king") || lower.contains("tealive") || lower.contains("warung") -> CAT_DINING
                lower.contains("jaya grocer") || lower.contains("speedmart") || lower.contains("mydin") -> CAT_GROCERIES
                lower.contains("grab") -> CAT_TRANSPORT
                lower.contains("tenaga nasional") -> CAT_UTILITIES
                else -> null
            }
        }
    }
}
