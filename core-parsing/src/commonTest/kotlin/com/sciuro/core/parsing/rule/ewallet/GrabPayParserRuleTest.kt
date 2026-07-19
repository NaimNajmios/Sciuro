package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class GrabPayParserRuleTest {
    @Test
    fun testGrabPayExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "Grab English 1",
                packageName = "com.grabtaxi.passenger",
                title = "Payment",
                text = "Payment Successful. RM 45.00 was paid to JAYA GROCER.",
                expectedAmount = 45.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "JAYA GROCER"
            ),
            ParserTestCase(
                description = "Grab English 2",
                packageName = "com.grabtaxi.passenger",
                title = "Payment",
                text = "You have successfully paid RM18.00 to BURGER KING.",
                expectedAmount = 18.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "BURGER KING"
            )
        )
        runParserTests(GrabPayParserRule(), cases)
    }
}
