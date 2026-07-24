package com.sciuro.core.parsing.util

import com.sciuro.core.parsing.model.TransactionDirection

object ConfidenceScorer {
    private const val AMOUNT_WEIGHT = 0.3f
    private const val DIRECTION_WEIGHT = 0.3f
    private const val MERCHANT_WEIGHT = 0.2f
    private const val COUNTERPARTY_WEIGHT = 0.1f
    private const val BASE_SCORE = 0.2f
    private const val MAX_CONFIDENCE = 1.0f

    fun score(
        amount: Double,
        direction: TransactionDirection?,
        merchant: String?,
        counterpartyAccount: String?
    ): Float {
        val raw = (if (amount > 0) AMOUNT_WEIGHT else 0f) +
                  (if (direction != null) DIRECTION_WEIGHT else 0f) +
                  (if (merchant != null) MERCHANT_WEIGHT else 0f) +
                  (if (counterpartyAccount != null) COUNTERPARTY_WEIGHT else 0f) +
                  BASE_SCORE
        return raw.coerceAtMost(MAX_CONFIDENCE)
    }
}
