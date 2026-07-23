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
        fun guessFromStaticHeuristic(merchant: String): String? {
            val lower = merchant.lowercase()
            return when {
                lower.contains("starbucks") || lower.contains("mcdonalds") || lower.contains("kfc") || lower.contains("burger king") || lower.contains("tealive") || lower.contains("warung") -> "cat_dining"
                lower.contains("jaya grocer") || lower.contains("speedmart") || lower.contains("mydin") -> "cat_groceries"
                lower.contains("grab") -> "cat_transport"
                lower.contains("tenaga nasional") -> "cat_utilities"
                else -> null
            }
        }
    }
}
