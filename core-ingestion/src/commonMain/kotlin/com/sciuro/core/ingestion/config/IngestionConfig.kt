package com.sciuro.core.ingestion.config

object IngestionConfig {
    /**
     * Packages allowed to produce financial notifications. 
     * Any notification from a package not in this list is dropped immediately.
     */
    val allowedPackages = setOf(
        "com.cimbmalaysia",          // CIMB Clicks / OCTO
        "com.maybank2u.life",        // MAE
        "com.bsn.mybsn",             // BSN
        "my.com.tngdigital.ewallet", // TNG eWallet
        "com.grabtaxi.passenger",    // GrabPay
        "my.com.myboost",            // Boost
        "com.shopee.my"              // ShopeePay
    )
}
