package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.ConfidenceScorer
import com.sciuro.core.parsing.util.extractAccountNumber
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant

class GrabPayParserRule : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "com.grabtaxi.passenger"
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(event.title) ?: return null
        
        val isOutflow = text.contains("paid", ignoreCase = true)

        val isInflow = text.contains("received", ignoreCase = true) ||
                       text.contains("credited", ignoreCase = true) ||
                       text.contains("top-up", ignoreCase = true)

        val direction = when {
            isOutflow -> TransactionDirection.OUTFLOW
            isInflow -> TransactionDirection.INFLOW
            else -> null
        }
        val merchant = extractMerchant(text)
        val counterpartyAccount = extractAccountNumber(text)

        val confidenceScore = ConfidenceScorer.score(amount, direction, merchant, counterpartyAccount)

        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "GrabPay",
            referenceId = null,
            counterpartyAccountNumber = counterpartyAccount,
            timestamp = event.timestamp,
            confidenceScore = confidenceScore
        )
    }
}
