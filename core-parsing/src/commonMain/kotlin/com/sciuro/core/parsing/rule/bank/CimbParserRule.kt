package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule

class CimbParserRule : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "com.cimbmalaysia"
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val title = event.title
        val text = event.text
        
        val amount = com.sciuro.core.parsing.util.extractAmount(text) ?: com.sciuro.core.parsing.util.extractAmount(title) ?: return null
        
        val isOutflow = text.contains("deducted", ignoreCase = true) ||
                        text.contains("ditolak", ignoreCase = true) ||
                        text.contains("payment to", ignoreCase = true) ||
                        text.contains("bayaran kepada", ignoreCase = true) ||
                        text.contains("transferred to", ignoreCase = true)

        val isInflow = text.contains("credited", ignoreCase = true) ||
                       text.contains("received", ignoreCase = true) ||
                       text.contains("masuk", ignoreCase = true) ||
                       text.contains("dikreditkan", ignoreCase = true)

        val direction = when {
            isOutflow -> TransactionDirection.OUTFLOW
            isInflow -> TransactionDirection.INFLOW
            else -> null
        }

        val merchant = com.sciuro.core.parsing.util.extractMerchant(text)
        val counterpartyAccount = com.sciuro.core.parsing.util.extractAccountNumber(text)

        val confidenceScore = (if (amount > 0) 0.3f else 0f) +
                              (if (direction != null) 0.3f else 0f) +
                              (if (merchant != null) 0.2f else 0f) +
                              (if (counterpartyAccount != null) 0.1f else 0f) +
                              0.2f

        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "CIMB",
            referenceId = null,
            counterpartyAccountNumber = counterpartyAccount,
            timestamp = event.timestamp,
            confidenceScore = confidenceScore
        )
    }
}
