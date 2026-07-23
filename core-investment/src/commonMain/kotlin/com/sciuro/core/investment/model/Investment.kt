package com.sciuro.core.investment.model

data class Investment(
    val id: String,
    val assetSymbol: String,
    val assetName: String,
    val assetType: String,
    val unitType: String = "UNITS",
    val unitsHeld: Double,
    val averageBuyPrice: Double,
    val associatedAccountId: String?,
    val status: String = "ACTIVE"
)
