package com.sciuro.core.ledger.model

data class Account(
    val id: String,
    val name: String,
    val type: String,
    val currency: String = "MYR",
    val balance: Double = 0.0,
    val associatedPackage: String? = null
)

data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null
)

data class Transaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val direction: String,
    val merchant: String?,
    val timestamp: Long,
    val referenceId: String?,
    val isReviewed: Boolean
)
