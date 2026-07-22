package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class MaybankParserRuleTest {
    @Test
    fun testMaybankExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "MAE Push (Payment)",
                packageName = "com.maybank2u.life",
                title = "Payment Successful",
                text = "Payment to STARBUCKS for RM 15.50 was successful.",
                expectedAmount = 15.50,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "STARBUCKS"
            ),
            ParserTestCase(
                description = "MAE Push (Transfer)",
                packageName = "com.maybank2u.life",
                title = "Transfer",
                text = "Transfer Successful. You have successfully transferred RM 100.00 to JOHN DOE.",
                expectedAmount = 100.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "JOHN DOE"
            ),
            ParserTestCase(
                description = "Card SMS",
                packageName = "com.android.mms",
                title = "SMS",
                text = "RM50.00 Maybank: Your Card ending 1234 was used at MCDONALDS on 10/10/24. Call 1300886688 if unauthorised.",
                expectedAmount = 50.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "MCDONALDS"
            ),
            ParserTestCase(
                description = "Card SMS",
                packageName = "com.android.mms",
                title = "SMS",
                text = "RM50.00 Maybank: Your Card ending 1234 was used at MCDONALDS on 10/10/24. Call 1300886688 if unauthorised.",
                expectedAmount = 50.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "MCDONALDS",
                expectedCounterpartyAccountNumber = "1234"
            ),
            ParserTestCase(
                description = "Maybank Inflow (credited)",
                packageName = "com.maybank2u.life",
                title = "Deposit",
                text = "RM 3000.00 has been credited to your account ending 1234.",
                expectedAmount = 3000.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null,
                expectedCounterpartyAccountNumber = "1234"
            ),
            ParserTestCase(
                description = "Maybank Ambiguous (no direction)",
                packageName = "com.maybank2u.life",
                title = "Alert",
                text = "RM 75.00 from account ending 5678.",
                expectedAmount = 75.00,
                expectedDirection = null,
                expectedMerchant = null,
                expectedCounterpartyAccountNumber = "5678"
            )
        )
        runParserTests(MaybankParserRule(), cases)
    }
}
