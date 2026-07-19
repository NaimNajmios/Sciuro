package com.sciuro.core.audit.model

enum class EntityType {
    TRANSACTION, 
    RECURRING_OBLIGATION, 
    DEBT, 
    TRANSFER_LINK,
    CASH_ADJUSTMENT, 
    BUDGET, 
    INVESTMENT_ACCOUNT, 
    EWALLET_ACCOUNT
}

enum class AuditAction {
    CREATE, 
    UPDATE, 
    DELETE, 
    RECLASSIFY, 
    MATCH, 
    UNMATCH
}

enum class AuditSource {
    SYSTEM_AUTO, 
    USER_MANUAL, 
    LLM_INFERRED
}

data class AuditLog(
    val id: String,
    val entityType: EntityType,
    val entityId: String,
    val action: AuditAction,
    val beforeState: String?, // JSON representation
    val afterState: String?,  // JSON representation
    val source: AuditSource,
    val confidence: Float?,
    val timestamp: Long
)
