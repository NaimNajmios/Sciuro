package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import com.sciuro.core.parsing.util.extractAmount
import com.sciuro.core.parsing.util.extractMerchant

class BoostParserRule : ParserRule {
    override fun matches(event: RawEvent): Boolean {
        return event.sourcePackageOrAddress == "my.com.myboost"
    }

    override fun extract(event: RawEvent): StructuredDraft? {
        val text = event.text
        
        val amount = extractAmount(text) ?: extractAmount(event.title) ?: return null
        
        val isOutflow = text.contains("payment", ignoreCase = true) || text.contains("paid", ignoreCase = true)
        val direction = if (isOutflow) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW
        val merchant = extractMerchant(text)
        
        return StructuredDraft(
            amount = amount,
            direction = direction,
            merchant = merchant,
            accountOrChannel = "Boost",
            referenceId = null,
            timestamp = event.timestamp,
            isConfident = merchant != null
        )
    }
}
