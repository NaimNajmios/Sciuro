package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.ConfidenceScorer
import com.sciuro.core.parsing.util.extractAccountNumber
import com.sciuro.core.parsing.util.detectDirection
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant
import com.sciuro.core.parsing.util.matchesAggregatorForward

class ScParserRule(
    private val aggregatorPackages: Set<String> = emptySet()
) : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "com.stanchart.mobile" ||
               event.title.contains("Standard Chartered", ignoreCase = true) ||
               matchesAggregatorForward(event, aggregatorPackages, listOf("Standard Chartered".lowercase()))
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val title = event.title
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(title) ?: return null
        
        val direction = detectDirection(text, event.title)

        val merchant = extractMerchant(text)
        val counterpartyAccount = extractAccountNumber(text)

        val confidenceScore = ConfidenceScorer.score(amount, direction, merchant, counterpartyAccount)

        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "Standard Chartered",
            referenceId = null,
            counterpartyAccountNumber = counterpartyAccount,
            timestamp = event.timestamp,
            confidenceScore = confidenceScore
        )
    }
}
