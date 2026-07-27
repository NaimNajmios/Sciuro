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
        "com.samsung.android.email.provider",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    val defaultAllowedPackages = directBankPackages + aggregatorPackages

    val bankSmsSenderKeywords = setOf(
        "cimb", "maybank", "m2u", "rhb", "public bank", "pbe", "hong leong",
        "hlb", "ambank", "bank islam", "bimb", "bsn", "alliance bank",
        "affin bank", "standard chartered", "scb", "hsbc", "ocbc", "uob",
        "gxbank", "aeon bank", "tng", "touch", "n go", "grab", "boost",
        "shopee", "bigpay", "setel", "duitnow", "fpx", "jompay",
        "paynet", "spaylater"
    )
}
