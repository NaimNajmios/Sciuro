package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SciuroIngestionOrchestrator(
    private val notificationSource: NotificationSourceAdapter,
    private val parserPipeline: SciuroParserPipeline,
    private val transactionRepository: TransactionRepository
) {
    private var job: Job? = null

    fun startListening(scope: CoroutineScope) {
        if (job?.isActive == true) return
        
        job = scope.launch {
            notificationSource.observeEvents().collect { rawEvent ->
                // 1. Parse Event
                val draft = parserPipeline.process(rawEvent) ?: return@collect
                
                // 2. Triage / Auto-Categorize (Naive mapping for now)
                val categoryId = guessCategoryId(draft.merchant)
                
                // 3. Map to Ledger Transaction
                val transaction = Transaction(
                    id = generateUuid(),
                    accountId = guessAccountId(draft.accountOrChannel),
                    categoryId = categoryId,
                    amount = draft.amount,
                    direction = draft.direction.name,
                    merchant = draft.merchant,
                    timestamp = draft.timestamp,
                    referenceId = draft.referenceId,
                    isReviewed = draft.isConfident && categoryId != null // Send to Inbox if LLM parsed OR unmapped category
                )
                
                // 4. Book to Ledger
                val auditSource = if (draft.isConfident) AuditSource.SYSTEM_AUTO else AuditSource.LLM_INFERRED
                transactionRepository.bookTransaction(transaction, source = auditSource)
            }
        }
    }

    fun stopListening() {
        job?.cancel()
        job = null
    }
    
    private fun guessCategoryId(merchant: String?): String? {
        if (merchant == null) return null
        val lower = merchant.lowercase()
        return when {
            lower.contains("starbucks") || lower.contains("mcdonalds") || lower.contains("kfc") || lower.contains("burger king") || lower.contains("tealive") || lower.contains("warung") -> "cat_dining"
            lower.contains("jaya grocer") || lower.contains("speedmart") || lower.contains("mydin") -> "cat_groceries"
            lower.contains("grab") -> "cat_transport"
            lower.contains("tenaga nasional") -> "cat_utilities"
            else -> null
        }
    }
    
    private fun guessAccountId(channel: String?): String {
        return "acc_${channel?.lowercase()?.replace(" ", "_") ?: "unknown"}"
    }
}
