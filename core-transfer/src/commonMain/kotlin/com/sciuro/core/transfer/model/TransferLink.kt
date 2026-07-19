package com.sciuro.core.transfer.model

data class TransferLink(
    val id: String,
    val outflowTransactionId: String,
    val inflowTransactionId: String,
    val amount: Double,
    val createdAt: Long
)
