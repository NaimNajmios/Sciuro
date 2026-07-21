package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class BsnParserRuleTest {
    @Test
    fun testBsnExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "BSN English SMS",
                packageName = "com.bsn.mybsn", // Could be SMS package too, but matches text BSN:
                title = "SMS",
                text = "BSN: Transaction of RM30.50 to WATSONS on 12/10/24 14:30 was successful. Ref: 12345.",
                expectedAmount = 30.50,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "WATSONS"
            ),
            ParserTestCase(
                description = "BSN BM SMS",
                packageName = "com.bsn.mybsn",
                title = "SMS",
                text = "BSN: Transaksi sebanyak RM15.00 kepada TEALIVE pada 12/10/24 14:30 adalah berjaya. Ruj: 123.",
                expectedAmount = 15.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "TEALIVE"
            ),
            ParserTestCase(
                description = "BSN Inflow (credited)",
                packageName = "com.bsn.mybsn",
                title = "SMS",
                text = "BSN: RM 2000.00 has been credited to your account. Ref: 456.",
                expectedAmount = 2000.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null
            ),
            ParserTestCase(
                description = "BSN Ambiguous (no direction)",
                packageName = "com.bsn.mybsn",
                title = "SMS",
                text = "BSN: RM 50.00 from account ending 789.",
                expectedAmount = 50.00,
                expectedDirection = null,
                expectedMerchant = null
            )
        )
        runParserTests(BsnParserRule(), cases)
    }
}
