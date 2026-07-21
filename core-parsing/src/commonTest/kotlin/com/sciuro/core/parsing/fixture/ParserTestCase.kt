package com.sciuro.core.parsing.fixture

import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.parsing.model.TransactionDirection
import com.sciuro.core.parsing.rule.ParserRule
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

data class ParserTestCase(
    val description: String,
    val packageName: String,
    val title: String,
    val text: String,
    val expectedAmount: Double,
    val expectedDirection: TransactionDirection?,
    val expectedMerchant: String? = null,
    val expectedAccount: String? = null,
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

fun runParserTests(parserRule: ParserRule, testCases: List<ParserTestCase>) {
    testCases.forEach { testCase ->
        val rawEvent = testCase.toRawEvent()

        if (testCase.expectNull) {
            assertEquals(false, parserRule.matches(rawEvent), "Rule should NOT match for: ${testCase.description}")
            val result = parserRule.extract(rawEvent)
            assertNull(result, "Parser should return null for: ${testCase.description}")
            return@forEach
        }

        assertEquals(true, parserRule.matches(rawEvent), "Rule should match package: ${testCase.packageName}")

        val result = parserRule.extract(rawEvent)

        assertNotNull(result, "Parser failed to extract data for test case: ${testCase.description}")
        assertEquals(testCase.expectedAmount, result.amount, "Amount mismatch for test case: ${testCase.description}")
        assertEquals(testCase.expectedDirection, result.direction, "Direction mismatch for test case: ${testCase.description}")
        if (testCase.expectedMerchant != null) {
            assertEquals(testCase.expectedMerchant, result.merchant, "Merchant mismatch for test case: ${testCase.description}")
        }
        if (testCase.expectedAccount != null) {
            assertEquals(testCase.expectedAccount, result.accountOrChannel, "Account mismatch for test case: ${testCase.description}")
        }
    }
}
