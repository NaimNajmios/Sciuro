package com.sciuro.core.ingestion.model

enum class SourceType {
    NOTIFICATION, 
    EMAIL, 
    SMS, 
    MANUAL
}

data class RawEvent(
    val id: String,
    val sourceType: SourceType,
    val sourcePackageOrAddress: String, // e.g. "com.maybank2u.life" or "SMS_SENDER"
    val title: String,
    val text: String,
    val timestamp: Long
)
