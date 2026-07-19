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
                        
        val direction = if (isOutflow) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW
        
        val merchant = com.sciuro.core.parsing.util.extractMerchant(text)
        
        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "CIMB",
            referenceId = null,
            timestamp = event.timestamp,
            isConfident = merchant != null
        )
    }
}
