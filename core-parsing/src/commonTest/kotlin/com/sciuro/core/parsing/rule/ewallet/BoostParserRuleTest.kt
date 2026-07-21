package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class BoostParserRuleTest {
    @Test
    fun testBoostExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "Boost English 1",
                packageName = "my.com.myboost",
                title = "Payment",
                text = "Hooray! Your payment of RM20.00 to MYDIN was successful.",
                expectedAmount = 20.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "MYDIN"
            ),
            ParserTestCase(
                description = "Boost English 2",
                packageName = "my.com.myboost",
                title = "Payment",
                text = "Payment successful! RM 5.50 paid to WARUNG PAK ALI.",
                expectedAmount = 5.50,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "WARUNG PAK ALI"
            ),
            ParserTestCase(
                description = "Boost Inflow (received)",
                packageName = "my.com.myboost",
                title = "Money Received",
                text = "You have received RM 100.00 from SIAU KEE.",
                expectedAmount = 100.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = "SIAU KEE"
            ),
            ParserTestCase(
                description = "Boost Inflow (top-up)",
                packageName = "my.com.myboost",
                title = "Top-Up",
                text = "Top-Up successful! RM 30.00 has been credited to your Boost account.",
                expectedAmount = 30.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null
            )
        )
        runParserTests(BoostParserRule(), cases)
    }
}
