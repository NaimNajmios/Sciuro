package com.sciuro.core.parsing.fixture

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.parsing.model.TransactionDirection

data class ParserTestCase(
    val description: String,
    val packageName: String,
    val title: String,
    val text: String,
    val expectedAmount: Double,
    val expectedDirection: TransactionDirection?,
    val expectedMerchant: String? = null,
    val expectedAccount: String? = null,
    val expectedCounterpartyAccountNumber: String? = null,
    val expectNull: Boolean = false
) {
    fun toRawEvent(): RawEvent = RawEvent(
        id = "test-id",
        sourceType = SourceType.NOTIFICATION,
        sourcePackageOrAddress = packageName,
        title = title,
        text = text,
        timestamp = 1000L
    )
}
