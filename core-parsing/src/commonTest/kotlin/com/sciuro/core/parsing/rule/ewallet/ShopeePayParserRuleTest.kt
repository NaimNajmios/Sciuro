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
            ),
            ParserTestCase(
                description = "ShopeePay Inflow (refund)",
                packageName = "com.shopee.my",
                title = "Refund",
                text = "Refund of RM 15.00 has been credited to your ShopeePay wallet.",
                expectedAmount = 15.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = null
            ),
            ParserTestCase(
                description = "ShopeePay Inflow (received)",
                packageName = "com.shopee.my",
                title = "Money Received",
                text = "You have received RM 200.00 from Shopee Seller.",
                expectedAmount = 200.00,
                expectedDirection = TransactionDirection.INFLOW,
                expectedMerchant = "Shopee Seller"
            )
        )
        runParserTests(ShopeePayParserRule(), cases)
    }
}
