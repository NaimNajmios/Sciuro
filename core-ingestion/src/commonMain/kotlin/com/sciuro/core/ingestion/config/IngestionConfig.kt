package com.sciuro.core.ingestion.config

object IngestionDefaults {
    val directBankPackages = setOf(
        "com.cimbmalaysia",
        "my.com.cimb.octo",
        "com.maybank2u.life",
        "com.maybank2u.m2u",
        "com.publicbank.pbebank",
        "com.publicbank.pbepay",
        "my.com.rhb.mymobilebanking",
        "my.com.rhb.mb",
        "my.com.hongleongconnect.mobile.connect",
        "com.ambank.ambonline",
        "com.bankislam.bimbmobile",
        "com.irakyat.mobile",
        "com.bsn.mybsn",
        "my.com.alliancebank.allianceonline",
        "my.com.affinbank.affinonline",
        "com.sc.breeze.malaysia",
        "com.htsu.hsbcpersonalbanking",
        "com.ocbc.mobile",
        "com.uob.mighty.my",
        "com.gxbank.my",
        "com.aeonbank.my",
        "my.com.tngdigital.ewallet",
        "com.grabtaxi.passenger",
        "my.com.myboost",
        "com.shopee.my",
        "my.com.setel",
        "com.bigpay.consumer"
    )

    val aggregatorPackages = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.samsung.android.email.provider"
    )

    val defaultAllowedPackages = directBankPackages + aggregatorPackages
}
