package com.sciuro.core.ingestion.config

object IngestionConfig {
    /**
     * Direct financial apps. Notifications from these are implicitly trusted to be financial.
     */
    val directBankPackages = setOf(
        "com.cimbmalaysia",                     // CIMB Clicks
        "my.com.cimb.octo",                     // CIMB OCTO MY
        "com.maybank2u.life",                   // MAE by Maybank2u
        "com.maybank2u.m2u",                    // Maybank2u (Legacy)
        "com.publicbank.pbebank",               // PB engage
        "com.publicbank.pbepay",                // PB enterprise
        "my.com.rhb.mymobilebanking",           // RHB Mobile
        "my.com.rhb.mb",                        // RHB (Legacy)
        "my.com.hongleongconnect.mobile.connect", // HLB Connect
        "com.ambank.ambonline",                 // AmOnline
        "com.bankislam.bimbmobile",             // GO by Bank Islam
        "com.irakyat.mobile",                   // iRakyat
        "com.bsn.mybsn",                        // BSN
        "my.com.alliancebank.allianceonline",   // Alliance Bank
        "my.com.affinbank.affinonline",         // Affin Bank
        "com.sc.breeze.malaysia",               // Standard Chartered
        "com.htsu.hsbcpersonalbanking",         // HSBC
        "com.ocbc.mobile",                      // OCBC
        "com.uob.mighty.my",                    // UOB
        "com.gxbank.my",                        // GXBank
        "com.aeonbank.my",                      // AEON Bank
        "my.com.tngdigital.ewallet",            // TNG eWallet
        "com.grabtaxi.passenger",               // GrabPay
        "my.com.myboost",                       // Boost
        "com.shopee.my",                        // ShopeePay
        "my.com.setel",                         // Setel
        "com.bigpay.consumer"                   // BigPay
    )

    /**
     * Aggregator apps (like email clients). Notifications from these require a heuristic pre-filter.
     */
    val aggregatorPackages = setOf(
        "com.google.android.gm",                // Gmail
        "com.microsoft.office.outlook",         // Outlook
        "com.samsung.android.email.provider"    // Samsung Email
    )

    val allowedPackages = directBankPackages + aggregatorPackages
}
