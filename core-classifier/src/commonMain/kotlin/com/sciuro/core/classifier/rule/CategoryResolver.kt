package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.model.TransactionIntent
import com.sciuro.core.ledger.db.SciuroDatabase

class CategoryResolver(
    private val database: SciuroDatabase
) {
    suspend fun resolve(merchant: String?, amount: Double, direction: String): CategorySuggestion? {
        if (merchant == null || merchant.isBlank()) return null

        if (direction == "OUTFLOW") {
            val debtMatch = database.debtQueries.selectDebtsByDirectionAndActive("I_OWE")
                .executeAsList()
                .firstOrNull { merchant.contains(it.name, ignoreCase = true) || it.name.contains(merchant, ignoreCase = true) }
            if (debtMatch != null) {
                return CategorySuggestion(
                    categoryId = "cat_debt_payment",
                    confidence = 0.95f,
                    reason = "debt_match:${debtMatch.name}",
                    autoConfirmable = true,
                    intent = TransactionIntent.DebtPayment(
                        debtId = debtMatch.id,
                        debtName = debtMatch.name,
                        remainingBalance = debtMatch.remaining_balance,
                        counterpartyName = debtMatch.counterparty_name
                    )
                )
            }
        }

        if (direction == "INFLOW") {
            val collectibleMatch = database.debtQueries.selectDebtsByDirectionAndActive("OWED_TO_ME")
                .executeAsList()
                .firstOrNull { it.counterparty_name != null && merchant.contains(it.counterparty_name!!, ignoreCase = true) }
            if (collectibleMatch != null) {
                return CategorySuggestion(
                    categoryId = "cat_inc_1",
                    confidence = 0.90f,
                    reason = "debt_collection:${collectibleMatch.name}",
                    intent = TransactionIntent.DebtCollection(
                        debtId = collectibleMatch.id,
                        counterparty = collectibleMatch.counterparty_name ?: collectibleMatch.name
                    )
                )
            }
        }

        val normalizedKey = buildString { merchant.forEach { append(it.lowercaseChar()) } }.trim()
        val learnedRule = database.merchantCategoryRuleQueries
            .selectMerchantRuleByKey(normalizedKey)
            .executeAsOneOrNull()
        if (learnedRule != null && learnedRule.confirmation_count >= 2) {
            return CategorySuggestion(
                categoryId = learnedRule.category_id,
                confidence = 0.90f,
                reason = "learned_rule",
                autoConfirmable = true
            )
        }

        val heuristicId = guessFromStaticHeuristic(merchant)
        if (heuristicId != null) {
            return CategorySuggestion(
                categoryId = heuristicId,
                confidence = 0.70f,
                reason = "static_heuristic",
                autoConfirmable = false
            )
        }

        return null
    }

    companion object {
        private const val CAT_DINING = "cat_exp_1"
        private const val CAT_TRANSPORT = "cat_exp_2"
        private const val CAT_UTILITIES = "cat_exp_3"
        private const val CAT_SHOPPING = "cat_exp_4"
        private const val CAT_ENTERTAINMENT = "cat_exp_5"
        private const val CAT_GROCERIES = "cat_exp_6"
        private const val CAT_HEALTH = "cat_exp_7"
        private const val CAT_EDUCATION = "cat_exp_8"

        fun guessFromStaticHeuristic(merchant: String): String? {
            val lower = buildString { merchant.forEach { append(it.lowercaseChar()) } }
            return when {
                lower.contains("tenaga nasional") || lower.contains("tnb") || lower.contains("air selangor") || lower.contains("syabas") || lower.contains("indah water") || lower.contains("iwk") || lower.contains("telekom") || lower.contains("unifi") || lower.contains("tm") || lower.contains("celcom") || lower.contains("maxis") || lower.contains("digi") || lower.contains("umobile") || lower.contains("time") || lower.contains("astral") || lower.contains("electric") || lower.contains("water bill") || lower.contains("internet") || lower.contains("phone bill") -> CAT_UTILITIES
                lower.contains("grab") || lower.contains("maxim") || lower.contains("airasia ride") || lower.contains("mycar") || lower.contains("petron") || lower.contains("shell") || lower.contains("caltex") || lower.contains("bdp") || lower.contains("tng") || lower.contains("touch") || lower.contains("ktm") || lower.contains("mrt") || lower.contains("lrt") || lower.contains("monorel") || lower.contains("rapidkl") || lower.contains("bus") || lower.contains("taxi") || lower.contains("parkir") || lower.contains("parking") || lower.contains("toll") || lower.contains("toll plus") || lower.contains("touchngo") || lower.contains("tngewallet") -> CAT_TRANSPORT
                lower.contains("netflix") || lower.contains("spotify") || lower.contains("youtube") || lower.contains("disney") || lower.contains("apple") || lower.contains("steam") || lower.contains("playstation") || lower.contains("xbox") || lower.contains("nintendo") || lower.contains("gaming") || lower.contains("cinema") || lower.contains("ticket") || lower.contains("concert") || lower.contains("movie") || lower.contains("music") || lower.contains("streaming") || lower.contains("subscription") || lower.contains("tonton") || lower.contains("iflix") || lower.contains("vidi") || lower.contains("dimsum") -> CAT_ENTERTAINMENT
                lower.contains("jaya grocer") || lower.contains("speedmart") || lower.contains("mydin") || lower.contains("lotus") || lower.contains("tesco") || lower.contains("aeon") || lower.contains("village grocer") || lower.contains("nsk") || lower.contains("pasar") || lower.contains("grocer") || lower.contains("supermarket") || lower.contains("wet market") -> CAT_GROCERIES
                lower.contains("starbucks") || lower.contains("mcdonalds") || lower.contains("kfc") || lower.contains("burger king") || lower.contains("tealive") || lower.contains("warung") || lower.contains("kopi") || lower.contains("mamak") || lower.contains("nasi") || lower.contains("restoran") || lower.contains("cafe") || lower.contains("domino") || lower.contains("pizza hut") || lower.contains("subway") || lower.contains("texas chicken") || lower.contains("marrybrown") || lower.contains("oldtown") || lower.contains("secret recipe") || lower.contains("dining") || lower.contains("food") || lower.contains("roti canai") || lower.contains("mee") || lower.contains("kuih") -> CAT_DINING
                lower.contains("shopee") || lower.contains("lazada") || lower.contains("zalora") || lower.contains("amazon") || lower.contains("shein") || lower.contains("uniqlo") || lower.contains("h&m") || lower.contains("padini") || lower.contains("nike") || lower.contains("adidas") || lower.contains("clothing") || lower.contains("fashion") || lower.contains("electronics") || lower.contains("phone") || lower.contains("gadget") || lower.contains("furniture") || lower.contains("ikea") || lower.contains("mr diy") || lower.contains("daiso") || lower.contains("miniso") || lower.contains("watson") || lower.contains("guardian") || lower.contains("cosmetics") || lower.contains("mall") || lower.contains("department") || lower.contains("toys") || lower.contains("books") || lower.contains("popular") -> CAT_SHOPPING
                lower.contains("klinik") || lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("farmasi") || lower.contains("doctor") || lower.contains("dental") || lower.contains("optometrist") || lower.contains("health") || lower.contains("medical") || lower.contains("insurance") || lower.contains("wellness") || lower.contains("gym") || lower.contains("fitness") || lower.contains("vitamin") || lower.contains("specialist") || lower.contains("panel") -> CAT_HEALTH
                lower.contains("tuition") || lower.contains("tution") || lower.contains("tuisyen") || lower.contains("university") || lower.contains("college") || lower.contains("school") || lower.contains("education") || lower.contains("course") || lower.contains("training") || lower.contains("exam") || lower.contains("book") && lower.contains("text") || lower.contains("language") || lower.contains("skill") || lower.contains("coursera") || lower.contains("udemy") -> CAT_EDUCATION
                else -> null
            }
        }
    }
}
