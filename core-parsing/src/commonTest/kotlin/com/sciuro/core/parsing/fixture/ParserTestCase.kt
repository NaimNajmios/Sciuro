package com.sciuro.core.parsing.fixture

import com.sciuro.core.parsing.rule.ParserRule
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        if (testCase.expectedCounterpartyAccountNumber != null) {
            assertEquals(testCase.expectedCounterpartyAccountNumber, result.counterpartyAccountNumber, "Counterparty account number mismatch for test case: ${testCase.description}")
        }
    }
}
