package com.sciuro.core.parsing.rule.ewallet

import com.sciuro.core.parsing.fixture.ParserTestCase
import com.sciuro.core.parsing.fixture.runParserTests
import com.sciuro.core.parsing.model.TransactionDirection
import kotlin.test.Test

class ShopeePayParserRuleTest {
    @Test
    fun testShopeePayExtraction() {
        val cases = listOf(
            ParserTestCase(
                description = "Shopee English",
                packageName = "com.shopee.my",
                title = "Payment",
                text = "Payment Successful. You paid RM12.80 to KFC.",
                expectedAmount = 12.80,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "KFC"
            ),
            ParserTestCase(
                description = "Shopee BM",
                packageName = "com.shopee.my",
                title = "Payment",
                text = "Pembayaran Berjaya. Anda telah membayar RM30.00 kepada SPEEDMART.",
                expectedAmount = 30.00,
                expectedDirection = TransactionDirection.OUTFLOW,
                expectedMerchant = "SPEEDMART"
            )
        )
        runParserTests(ShopeePayParserRule(), cases)
    }
}
