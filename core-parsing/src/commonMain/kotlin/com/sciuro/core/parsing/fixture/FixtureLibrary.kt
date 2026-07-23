package com.sciuro.core.parsing.fixture

import com.sciuro.core.parsing.model.TransactionDirection

object FixtureLibrary {
    data class Fixture(
        val description: String,
        val packageName: String,
        val title: String,
        val text: String
    )

    val fixtures: List<Fixture> = listOf(
        // CIMB
        Fixture("CIMB EN Spend", "com.cimbmalaysia", "Successful Transaction",
            "RM 15.50 has been deducted from your account ending 1234 for payment to STARBUCKS."),
        Fixture("CIMB BM Spend", "com.cimbmalaysia", "Transaksi Berjaya",
            "RM 120.00 telah ditolak dari akaun anda berakhir 1234 untuk bayaran kepada TENAGA NASIONAL."),
        Fixture("CIMB Inflow", "com.cimbmalaysia", "Deposit",
            "RM 5000.00 has been credited to your account ending 5678."),
        Fixture("CIMB Inflow BM", "com.cimbmalaysia", "Deposit",
            "RM 250.00 telah masuk ke dalam akaun anda berakhir 5678."),
        Fixture("CIMB Ambiguous", "com.cimbmalaysia", "Notification",
            "RM 100.00 from account ending 1234."),
        Fixture("CIMB OCTO Inflow", "my.com.cimb.octo", "CIMB OCTO MY",
            "RM 2.00 has been credited to your account ending 1234."),

        // Maybank
        Fixture("MAE Push Payment", "com.maybank2u.life", "Payment Successful",
            "Payment to STARBUCKS for RM 15.50 was successful."),
        Fixture("MAE Push Transfer", "com.maybank2u.life", "Transfer",
            "Transfer Successful. You have successfully transferred RM 100.00 to JOHN DOE."),
        Fixture("Maybank Card SMS", "com.android.mms", "SMS",
            "RM50.00 Maybank: Your Card ending 1234 was used at MCDONALDS on 10/10/24."),
        Fixture("Maybank Inflow", "com.maybank2u.life", "Deposit",
            "RM 3000.00 has been credited to your account ending 1234."),
        Fixture("Maybank Inflow Salary", "com.maybank2u.life", "Salary",
            "Your salary of RM 8500.00 has been deposited into your account."),
        Fixture("Maybank Ambiguous", "com.maybank2u.life", "Alert",
            "RM 75.00 from account ending 5678."),
        Fixture("Maybank2u Legacy Inflow", "com.maybank2u.m2u", "Maybank2u: Funds Received",
            "You've just received RM 5.40 in your account ending ***3943."),

        // BSN
        Fixture("BSN EN SMS", "com.bsn.mybsn", "SMS",
            "BSN: Transaction of RM30.50 to WATSONS on 12/10/24 14:30 was successful. Ref: 12345."),
        Fixture("BSN BM SMS", "com.bsn.mybsn", "SMS",
            "BSN: Transaksi sebanyak RM15.00 kepada TEALIVE pada 12/10/24 14:30 adalah berjaya. Ruj: 123."),
        Fixture("BSN Inflow", "com.bsn.mybsn", "SMS",
            "BSN: RM 2000.00 has been credited to your account. Ref: 456."),
        Fixture("BSN DuitNow Outflow", "com.bsn.mybsn", "myBSN",
            "DuitNow to MUHAMMAD NAIM N - RM5.40 on 23/07/2026 09:00:25 AM successful. Didn't do this? Call Contact Centre."),
        Fixture("BSN Ambiguous", "com.bsn.mybsn", "SMS",
            "BSN: RM 50.00 from account ending 789."),

        // TNG
        Fixture("TNG EN Payment", "my.com.tngdigital.ewallet", "Payment Successful",
            "Payment Successful! You have paid RM 25.00 to FAMILY MART."),
        Fixture("TNG BM Payment", "my.com.tngdigital.ewallet", "Pembayaran Berjaya",
            "Pembayaran Berjaya! Anda telah membayar RM 12.00 kepada NINJA VAN."),
        Fixture("TNG Inflow Received", "my.com.tngdigital.ewallet", "Money Received",
            "You have received RM 50.00 from AHMAD ALI."),
        Fixture("TNG Inflow TopUp", "my.com.tngdigital.ewallet", "Top-Up Successful",
            "Top-Up Successful! RM 100.00 has been credited to your TNG eWallet."),

        // GrabPay
        Fixture("Grab EN Payment", "com.grabtaxi.passenger", "Payment",
            "Payment Successful. RM 45.00 was paid to JAYA GROCER."),
        Fixture("Grab EN Payment 2", "com.grabtaxi.passenger", "Payment",
            "You have successfully paid RM18.00 to BURGER KING."),
        Fixture("GrabPay Inflow Refund", "com.grabtaxi.passenger", "Refund",
            "Refund of RM 25.00 has been credited to your GrabPay wallet."),
        Fixture("GrabPay Inflow TopUp", "com.grabtaxi.passenger", "Top-Up",
            "RM 50.00 has been credited to your GrabPay wallet via top-up."),

        // ShopeePay
        Fixture("Shopee EN Payment", "com.shopee.my", "Payment",
            "Payment Successful. You paid RM12.80 to KFC."),
        Fixture("Shopee BM Payment", "com.shopee.my", "Payment",
            "Pembayaran Berjaya. Anda telah membayar RM30.00 kepada SPEEDMART."),
        Fixture("ShopeePay Inflow Refund", "com.shopee.my", "Refund",
            "Refund of RM 15.00 has been credited to your ShopeePay wallet."),
        Fixture("ShopeePay Inflow Received", "com.shopee.my", "Money Received",
            "You have received RM 200.00 from Shopee Seller."),

        // Boost
        Fixture("Boost EN Payment", "my.com.myboost", "Payment",
            "Hooray! Your payment of RM20.00 to MYDIN was successful."),
        Fixture("Boost EN Payment 2", "my.com.myboost", "Payment",
            "Payment successful! RM 5.50 paid to WARUNG PAK ALI."),
        Fixture("Boost Inflow Received", "my.com.myboost", "Money Received",
            "You have received RM 100.00 from SIAU KEE."),
        Fixture("Boost Inflow TopUp", "my.com.myboost", "Top-Up",
            "Top-Up successful! RM 30.00 has been credited to your Boost account.")
    )

    fun fixturesForPackage(packageName: String): List<Fixture> =
        fixtures.filter { it.packageName == packageName }

    fun allPackages(): Set<String> =
        fixtures.map { it.packageName }.toSet()
}
