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
            )
        )
        runParserTests(BoostParserRule(), cases)
    }
}
