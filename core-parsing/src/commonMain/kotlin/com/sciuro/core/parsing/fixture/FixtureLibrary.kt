package com.sciuro.core.parsing.fixture

import com.sciuro.core.parsing.model.TransactionDirection

object FixtureLibrary {
    data class Fixture(
        val description: String,
        val packageName: String,
        val title: String,
        val text: String,
        val expectedAmount: Double? = null,
        val expectedDirection: TransactionDirection? = null,
        val expectedMerchant: String? = null,
        val expectedCounterpartyAccount: String? = null,
        val expectedConfident: Boolean? = null
    )

    val fixtures: List<Fixture> = listOf(
        // ── CIMB ──
        Fixture("CIMB EN Spend", "com.cimbmalaysia", "Successful Transaction",
            "RM 15.50 has been deducted from your account ending 1234 for payment to STARBUCKS.",
            expectedAmount = 15.50, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "STARBUCKS", expectedConfident = true),
        Fixture("CIMB BM Spend", "com.cimbmalaysia", "Transaksi Berjaya",
            "RM 120.00 telah ditolak dari akaun anda berakhir 1234 untuk bayaran kepada TENAGA NASIONAL.",
            expectedAmount = 120.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "TENAGA NASIONAL", expectedConfident = true),
        Fixture("CIMB Inflow", "com.cimbmalaysia", "Deposit",
            "RM 5000.00 has been credited to your account ending 5678.",
            expectedAmount = 5000.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("CIMB Inflow BM", "com.cimbmalaysia", "Deposit",
            "RM 250.00 telah masuk ke dalam akaun anda berakhir 5678.",
            expectedAmount = 250.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("CIMB Ambiguous", "com.cimbmalaysia", "Notification",
            "RM 100.00 from account ending 1234.",
            expectedAmount = 100.00, expectedDirection = null, expectedConfident = false),
        Fixture("CIMB OCTO Inflow", "my.com.cimb.octo", "CIMB OCTO MY",
            "RM 2.00 has been credited to your account ending 1234.",
            expectedAmount = 2.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── Maybank ──
        Fixture("MAE Push Payment", "com.maybank2u.life", "Payment Successful",
            "Payment to STARBUCKS for RM 15.50 was successful.",
            expectedAmount = 15.50, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "STARBUCKS", expectedConfident = true),
        Fixture("MAE Push Transfer", "com.maybank2u.life", "Transfer",
            "Transfer Successful. You have successfully transferred RM 100.00 to JOHN DOE.",
            expectedAmount = 100.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "JOHN DOE", expectedConfident = true),
        Fixture("Maybank Card SMS", "com.android.mms", "SMS",
            "RM50.00 Maybank: Your Card ending 1234 was used at MCDONALDS on 10/10/24.",
            expectedAmount = 50.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "MCDONALDS", expectedConfident = true),
        Fixture("Maybank Inflow", "com.maybank2u.life", "Deposit",
            "RM 3000.00 has been credited to your account ending 1234.",
            expectedAmount = 3000.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("Maybank Inflow Salary", "com.maybank2u.life", "Salary",
            "Your salary of RM 8500.00 has been deposited into your account.",
            expectedAmount = 8500.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("Maybank Ambiguous", "com.maybank2u.life", "Alert",
            "RM 75.00 from account ending 5678.",
            expectedAmount = 75.00, expectedDirection = null, expectedConfident = false),
        Fixture("Maybank2u Legacy Inflow", "com.maybank2u.m2u", "Maybank2u: Funds Received",
            "You've just received RM 5.40 in your account ending ***3943.",
            expectedAmount = 5.40, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── BSN ──
        Fixture("BSN EN SMS", "com.bsn.mybsn", "SMS",
            "BSN: Transaction of RM30.50 to WATSONS on 12/10/24 14:30 was successful. Ref: 12345.",
            expectedAmount = 30.50, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "WATSONS", expectedConfident = true),
        Fixture("BSN BM SMS", "com.bsn.mybsn", "SMS",
            "BSN: Transaksi sebanyak RM15.00 kepada TEALIVE pada 12/10/24 14:30 adalah berjaya. Ruj: 123.",
            expectedAmount = 15.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "TEALIVE", expectedConfident = true),
        Fixture("BSN Inflow", "com.bsn.mybsn", "SMS",
            "BSN: RM 2000.00 has been credited to your account. Ref: 456.",
            expectedAmount = 2000.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("BSN DuitNow Outflow", "com.bsn.mybsn", "myBSN",
            "DuitNow to MUHAMMAD NAIM N - RM5.40 on 23/07/2026 09:00:25 AM successful. Didn't do this? Call Contact Centre.",
            expectedAmount = 5.40, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "MUHAMMAD NAIM N", expectedConfident = true),
        Fixture("BSN Ambiguous", "com.bsn.mybsn", "SMS",
            "BSN: RM 50.00 from account ending 789.",
            expectedAmount = 50.00, expectedDirection = null, expectedConfident = false),

        // ── TNG ──
        Fixture("TNG EN Payment", "my.com.tngdigital.ewallet", "Payment Successful",
            "Payment Successful! You have paid RM 25.00 to FAMILY MART.",
            expectedAmount = 25.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "FAMILY MART", expectedConfident = true),
        Fixture("TNG BM Payment", "my.com.tngdigital.ewallet", "Pembayaran Berjaya",
            "Pembayaran Berjaya! Anda telah membayar RM 12.00 kepada NINJA VAN.",
            expectedAmount = 12.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "NINJA VAN", expectedConfident = true),
        Fixture("TNG Inflow Received", "my.com.tngdigital.ewallet", "Money Received",
            "You have received RM 50.00 from AHMAD ALI.",
            expectedAmount = 50.00, expectedDirection = TransactionDirection.INFLOW,
            expectedMerchant = "AHMAD ALI", expectedConfident = true),
        Fixture("TNG Inflow TopUp", "my.com.tngdigital.ewallet", "Top-Up Successful",
            "Top-Up Successful! RM 100.00 has been credited to your TNG eWallet.",
            expectedAmount = 100.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── GrabPay ──
        Fixture("Grab EN Payment", "com.grabtaxi.passenger", "Payment",
            "Payment Successful. RM 45.00 was paid to JAYA GROCER.",
            expectedAmount = 45.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "JAYA GROCER", expectedConfident = true),
        Fixture("Grab EN Payment 2", "com.grabtaxi.passenger", "Payment",
            "You have successfully paid RM18.00 to BURGER KING.",
            expectedAmount = 18.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "BURGER KING", expectedConfident = true),
        Fixture("GrabPay Inflow Refund", "com.grabtaxi.passenger", "Refund",
            "Refund of RM 25.00 has been credited to your GrabPay wallet.",
            expectedAmount = 25.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("GrabPay Inflow TopUp", "com.grabtaxi.passenger", "Top-Up",
            "RM 50.00 has been credited to your GrabPay wallet via top-up.",
            expectedAmount = 50.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── ShopeePay ──
        Fixture("Shopee EN Payment", "com.shopee.my", "Payment",
            "Payment Successful. You paid RM12.80 to KFC.",
            expectedAmount = 12.80, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "KFC", expectedConfident = true),
        Fixture("Shopee BM Payment", "com.shopee.my", "Payment",
            "Pembayaran Berjaya. Anda telah membayar RM30.00 kepada SPEEDMART.",
            expectedAmount = 30.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "SPEEDMART", expectedConfident = true),
        Fixture("ShopeePay Inflow Refund", "com.shopee.my", "Refund",
            "Refund of RM 15.00 has been credited to your ShopeePay wallet.",
            expectedAmount = 15.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),
        Fixture("ShopeePay Inflow Received", "com.shopee.my", "Money Received",
            "You have received RM 200.00 from Shopee Seller.",
            expectedAmount = 200.00, expectedDirection = TransactionDirection.INFLOW,
            expectedMerchant = "Shopee Seller", expectedConfident = true),

        // ── Boost ──
        Fixture("Boost EN Payment", "my.com.myboost", "Payment",
            "Hooray! Your payment of RM20.00 to MYDIN was successful.",
            expectedAmount = 20.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "MYDIN", expectedConfident = true),
        Fixture("Boost EN Payment 2", "my.com.myboost", "Payment",
            "Payment successful! RM 5.50 paid to WARUNG PAK ALI.",
            expectedAmount = 5.50, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "WARUNG PAK ALI", expectedConfident = true),
        Fixture("Boost Inflow Received", "my.com.myboost", "Money Received",
            "You have received RM 100.00 from SIAU KEE.",
            expectedAmount = 100.00, expectedDirection = TransactionDirection.INFLOW,
            expectedMerchant = "SIAU KEE", expectedConfident = true),
        Fixture("Boost Inflow TopUp", "my.com.myboost", "Top-Up",
            "Top-Up successful! RM 30.00 has been credited to your Boost account.",
            expectedAmount = 30.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── NEW: Self-Transfer ──
        Fixture("CIMB Self-Transfer Outflow", "com.cimbmalaysia", "Transfer",
            "RM 500.00 has been deducted from your account ending 1234 for transfer to A/C 9123-4567-8900.",
            expectedAmount = 500.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedConfident = true),

        // ── NEW: Aggregator Forward ──
        Fixture("Gmail Forward Maybank", "com.google.android.gm", "Maybank2u Notification",
            "RM 8.90 Maybank: Payment to STARBUCKS was successful.",
            expectedAmount = 8.90, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "STARBUCKS", expectedConfident = true),
        Fixture("Outlook Forward CIMB", "com.microsoft.office.outlook", "CIMB Notification",
            "RM 450.00 has been credited to your account ending 5678.",
            expectedAmount = 450.00, expectedDirection = TransactionDirection.INFLOW,
            expectedConfident = true),

        // ── NEW: Multi-Currency Rejection ──
        Fixture("USD Amount Rejection", "com.cimbmalaysia", "USD Transaction",
            "USD 50.00 has been deducted from your account.",
            expectedAmount = null, expectedDirection = null, expectedConfident = false),

        // ── NEW: BNPL Detection ──
        Fixture("Boost Atome BNPL", "my.com.myboost", "Atome Payment",
            "Payment Successful. RM 150.00 has been paid to NIKE via Atome.",
            expectedAmount = 150.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "NIKE", expectedConfident = true),

        // ── NEW: Debt Payment ──
        Fixture("Maybank Loan Payment", "com.maybank2u.life", "Loan Payment",
            "RM 680.00 has been deducted for your loan payment. Ref: LN-2024-001.",
            expectedAmount = 680.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedConfident = true),

        // ── NEW: Subscription Detection Pair ──
        Fixture("CIMB Netflix 1", "com.cimbmalaysia", "Payment",
            "RM 55.00 has been deducted for payment to NETFLIX.",
            expectedAmount = 55.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "NETFLIX", expectedConfident = true),
        Fixture("CIMB Netflix 2", "com.cimbmalaysia", "Payment",
            "RM 55.00 has been deducted for payment to NETFLIX. Ref: 987.",
            expectedAmount = 55.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "NETFLIX", expectedConfident = true),

        // ── NEW: Large Amount ──
        Fixture("Maybank Large Transfer", "com.maybank2u.life", "Transfer",
            "Transfer Successful. RM 12,500.00 has been transferred to ALI PROPERTY.",
            expectedAmount = 12500.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedMerchant = "ALI PROPERTY", expectedConfident = true),

        // ── NEW: Minimal Notification (LLM-boundary) ──
        Fixture("TNG Minimal Text", "my.com.tngdigital.ewallet", "eWallet",
            "RM5.00 paid.",
            expectedAmount = 5.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedConfident = true),

        // ── NEW: Investment Purchase ──
        Fixture("Public Gold Purchase", "com.cimbmalaysia", "Gold Investment",
            "RM 250.00 has been deducted for your gold investment purchase.",
            expectedAmount = 250.00, expectedDirection = TransactionDirection.OUTFLOW,
            expectedConfident = true),
    )

    fun fixturesForPackage(packageName: String): List<Fixture> =
        fixtures.filter { it.packageName == packageName }

    fun allPackages(): Set<String> =
        fixtures.map { it.packageName }.toSet()

    val count: Int get() = fixtures.size
}
