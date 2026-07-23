package com.sciuro.core.audit.model

data class NetPosition(
    val totalAccounts: Double,
    val totalCash: Double,
    val totalInvestments: Double,
    val totalDebtsOwed: Double,
    val totalDebtsReceivable: Double,
    val netWorth: Double
)
