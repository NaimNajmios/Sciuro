package com.sciuro.core.classifier.rule

import com.sciuro.core.audit.model.TransactionIntent

data class CategorySuggestion(
    val categoryId: String,
    val confidence: Float,
    val reason: String,
    val autoConfirmable: Boolean = false,
    val intent: TransactionIntent? = null
)
