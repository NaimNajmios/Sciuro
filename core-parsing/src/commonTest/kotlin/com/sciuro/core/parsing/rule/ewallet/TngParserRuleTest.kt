package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class TngParserRuleTest {
    @Test
    fun testTngExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "English Payment",
                packageName = "my.com.tngdigital.ewallet",
                title = "Payment Successful",
                text = "Payment Successful! You have paid RM 25.00 to FAMILY MART.",
                expectedAmount = 25.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "FAMILY MART"
            ),
            ParserTestCase(
                description = "BM Payment",
                packageName = "my.com.tngdigital.ewallet",
                title = "Pembayaran Berjaya",
                text = "Pembayaran Berjaya! Anda telah membayar RM 12.00 kepada NINJA VAN.",
                expectedAmount = 12.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "NINJA VAN"
            )
        )
        runParserTests(TngParserRule(), cases)
    }
}
