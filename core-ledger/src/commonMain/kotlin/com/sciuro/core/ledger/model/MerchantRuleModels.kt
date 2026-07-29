package com.sciuro.core.ledger.model

data class MerchantAccountRuleUiModel(
    val merchantKey: String,
    val displayName: String,
    val accountId: String,
    val accountName: String,
    val confirmationCount: Int
) {
    companion object {
        fun displayNameFromKey(key: String): String =
            key.split("_", " ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

data class MerchantRuleUiModel(
    val merchantKey: String,
    val displayName: String,
    val categoryId: String,
    val categoryName: String,
    val confirmationCount: Int,
    val firstSeenAt: Long,
    val lastConfirmedAt: Long,
    val isTrusted: Boolean
) {
    companion object {
        const val TRUST_THRESHOLD = 3

        fun displayNameFromKey(key: String): String =
            key.split("_", " ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
