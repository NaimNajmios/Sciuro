package com.sciuro.core.parsing.rule.bank

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class CimbParserRuleTest {
    
    @Test
    fun testCimbExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "CIMB English Spend",
                packageName = "com.cimbmalaysia",
                title = "Successful Transaction",
                text = "RM 15.50 has been deducted from your account ending 1234 for payment to STARBUCKS.",
                expectedAmount = 15.50,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "STARBUCKS",
                expectedAccount = "CIMB",
                expectedCounterpartyAccountNumber = "1234"
            ),
            ParserTestCase(
                description = "CIMB BM Spend",
                packageName = "com.cimbmalaysia",
                title = "Transaksi Berjaya",
                text = "RM 120.00 telah ditolak dari akaun anda berakhir 1234 untuk bayaran kepada TENAGA NASIONAL.",
                expectedAmount = 120.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "TENAGA NASIONAL",
                expectedAccount = "CIMB",
                expectedCounterpartyAccountNumber = "1234"
            ),
            ParserTestCase(
                description = "CIMB Inflow (credited)",
                packageName = "com.cimbmalaysia",
                title = "Deposit",
                text = "RM 5000.00 has been credited to your account ending 5678.",
                expectedAmount = 5000.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null,
                expectedAccount = "CIMB",
                expectedCounterpartyAccountNumber = "5678"
            ),
            ParserTestCase(
                description = "CIMB Inflow BM (masuk)",
                packageName = "com.cimbmalaysia",
                title = "Deposit",
                text = "RM 250.00 telah masuk ke dalam akaun anda berakhir 5678.",
                expectedAmount = 250.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null,
                expectedAccount = "CIMB",
                expectedCounterpartyAccountNumber = "5678"
            ),
            ParserTestCase(
                description = "CIMB Ambiguous (no direction keywords)",
                packageName = "com.cimbmalaysia",
                title = "Notification",
                text = "RM 100.00 from account ending 1234.",
                expectedAmount = 100.00,
                expectedDirection = null,
                expectedMerchant = null,
                expectedAccount = "CIMB",
                expectedCounterpartyAccountNumber = "1234"
            )
        )
        
        runParserTests(CimbParserRule(), cases)
    }
}
