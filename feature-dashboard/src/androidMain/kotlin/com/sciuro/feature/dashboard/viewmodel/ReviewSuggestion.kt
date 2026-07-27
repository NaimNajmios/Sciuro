package com.sciuro.feature.dashboard.viewmodel

import com.sciuro.core.audit.model.TransactionIntent

data class ReviewSuggestion(
    val transactionId: String,
    val merchant: String?,
    val amount: Double,
    val direction: String,
    val suggestedCategoryId: String?,
    val suggestedAccountId: String?,
    val intent: TransactionIntent?,
    val reason: String
)
