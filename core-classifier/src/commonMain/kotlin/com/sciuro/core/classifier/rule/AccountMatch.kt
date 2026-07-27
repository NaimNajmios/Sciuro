package com.sciuro.core.classifier.rule

data class AccountMatch(
    val accountId: String,
    val score: Int,
    val reason: String
)
