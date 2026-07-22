package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.extractAccountNumber
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant

class BsnParserRule : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "com.bsn.mybsn" || event.text.contains("BSN:", ignoreCase = true)
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(event.title) ?: return null
        
        val isOutflow = text.contains("Transaction of", ignoreCase = true) ||
                        text.contains("Transaksi sebanyak", ignoreCase = true) ||
                        text.contains("ditolak", ignoreCase = true)

        val isInflow = text.contains("credited", ignoreCase = true) ||
                       text.contains("received", ignoreCase = true) ||
                       text.contains("masuk", ignoreCase = true) ||
                       text.contains("dikreditkan", ignoreCase = true)

        val direction = when {
            isOutflow -> TransactionDirection.OUTFLOW
            isInflow -> TransactionDirection.INFLOW
            else -> null
        }
        val merchant = extractMerchant(text)
        val counterpartyAccount = extractAccountNumber(text)

        val confidenceScore = (if (amount > 0) 0.3f else 0f) +
                              (if (direction != null) 0.3f else 0f) +
                              (if (merchant != null) 0.2f else 0f) +
                              (if (counterpartyAccount != null) 0.1f else 0f) +
                              0.2f

        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "BSN",
            referenceId = null,
            counterpartyAccountNumber = counterpartyAccount,
            timestamp = event.timestamp,
            confidenceScore = confidenceScore
        )
    }
}
