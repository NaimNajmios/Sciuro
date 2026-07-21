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
            ),
            ParserTestCase(
                description = "GrabPay Inflow (received)",
                packageName = "com.grabtaxi.passenger",
                title = "Refund",
                text = "Refund of RM 25.00 has been credited to your GrabPay wallet.",
                expectedAmount = 25.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null
            ),
            ParserTestCase(
                description = "GrabPay Inflow (top-up)",
                packageName = "com.grabtaxi.passenger",
                title = "Top-Up",
                text = "RM 50.00 has been credited to your GrabPay wallet via top-up.",
                expectedAmount = 50.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null
            )
        )
        runParserTests(GrabPayParserRule(), cases)
    }
}
