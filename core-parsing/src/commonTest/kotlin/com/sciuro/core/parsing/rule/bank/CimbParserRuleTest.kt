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
                expectedAccount = "CIMB"
            ),
            ParserTestCase(
                description = "CIMB BM Spend",
                packageName = "com.cimbmalaysia",
                title = "Transaksi Berjaya",
                text = "RM 120.00 telah ditolak dari akaun anda berakhir 1234 untuk bayaran kepada TENAGA NASIONAL.",
                expectedAmount = 120.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "TENAGA NASIONAL",
                expectedAccount = "CIMB"
            )
        )
        
        runParserTests(CimbParserRule(), cases)
    }
}
