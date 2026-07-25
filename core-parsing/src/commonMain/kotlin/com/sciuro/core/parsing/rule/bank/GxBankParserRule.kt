package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.ConfidenceScorer
import com.sciuro.core.parsing.util.extractAccountNumber
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant
import com.sciuro.core.parsing.util.matchesAggregatorForward

class GxBankParserRule(
    private val aggregatorPackages: Set<String> = emptySet()
) : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "my.com.gxbank" ||
               event.title.contains("GXBank", ignoreCase = true) ||
               matchesAggregatorForward(event, aggregatorPackages, listOf("GXBank".lowercase()))
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val title = event.title
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(title) ?: return null
        
        val isOutflow = text.contains("deducted", ignoreCase = true) ||
                        text.contains("payment to", ignoreCase = true) ||
                        text.contains("transferred to", ignoreCase = true) ||
                        text.contains("paid to", ignoreCase = true) ||
                        text.contains("sent to", ignoreCase = true)

        val isInflow = text.contains("credited", ignoreCase = true) ||
                       text.contains("received", ignoreCase = true)

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
            accountOrChannel = "GXBank",
            referenceId = null,
            counterpartyAccountNumber = counterpartyAccount,
            timestamp = event.timestamp,
            confidenceScore = confidenceScore
        )
    }
}
