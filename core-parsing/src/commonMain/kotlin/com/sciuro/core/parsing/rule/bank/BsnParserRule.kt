package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant

class BsnParserRule : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "com.bsn.mybsn" || event.text.contains("BSN:", ignoreCase = true)
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(event.title) ?: return null
        
        // BSN generally means outflow in the provided SMS templates
        val isOutflow = text.contains("Transaction of", ignoreCase = true) || 
                        text.contains("Transaksi sebanyak", ignoreCase = true)
                        
        val direction = if (isOutflow) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW
        val merchant = extractMerchant(text)
        
        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "BSN",
            referenceId = null,
            timestamp = event.timestamp,
            isConfident = merchant != null
        )
    }
}
