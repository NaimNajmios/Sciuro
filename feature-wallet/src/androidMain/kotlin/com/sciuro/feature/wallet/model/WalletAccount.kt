package com.sciuro.feature.wallet.model

data class WalletAccount(
    val id: String,
    val name: String,
    val balance: Double,
    val isEWallet: Boolean,
    val type: String,
    val associatedPackage: String?,
    val color: String?
)
