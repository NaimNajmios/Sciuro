package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import com.sciuro.core.parsing.model.DEFAULT_CONFIDENCE_THRESHOLD
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.transfer.engine.TransferDetectionEngine
import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.debt.engine.DebtEngine
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SciuroIngestionOrchestrator(
    private val notificationSource: NotificationSourceAdapter,
    private val parserPipeline: SciuroParserPipeline,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val rawEventRepository: RawEventRepository,
    private val transferDetectionEngine: TransferDetectionEngine,
    private val obligationCycleMatcher: ObligationCycleMatcher,
    private val budgetEngine: BudgetEngine,
    private val debtEngine: DebtEngine,
    private val investmentEngine: InvestmentEngine,
    private val obligationDetectionEngine: ObligationDetectionEngine,
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    private var job: Job? = null

    fun startListening(scope: CoroutineScope) {
        if (job?.isActive == true) return

        job = scope.launch {
            try {
                notificationSource.observeEvents().collect { rawEvent ->
                    processOneEvent(rawEvent)
                }
            } catch (e: Exception) {
                println("SCIURO_ORCHESTRATOR: Collector failed - ${e.message}")
                e.printStackTrace()
            }
        }

        job?.invokeOnCompletion { throwable ->
            if (throwable != null) {
                println("SCIURO_ORCHESTRATOR: Collector job terminated with error, restarting...")
                job = scope.launch {
                    try {
                        notificationSource.observeEvents().collect { rawEvent ->
                            processOneEvent(rawEvent)
                        }
                    } catch (e: Exception) {
                        println("SCIURO_ORCHESTRATOR: Collector failed again - ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun processOneEvent(rawEvent: RawEvent) {
        try {
            rawEventRepository.markProcessing(rawEvent.id)

            println("SCIURO_ORCHESTRATOR: Received event from ${rawEvent.sourcePackageOrAddress}")

            val draft = parserPipeline.process(rawEvent)
            if (draft == null) {
                println("SCIURO_ORCHESTRATOR: Parser pipeline returned null. Marking dead letter.")
                rawEventRepository.markDeadLetter(rawEvent.id, "Parser pipeline returned null")
                return
            }

            println("SCIURO_ORCHESTRATOR: Parsed successfully: $draft")

            val directionName = draft.direction?.name ?: run {
                println("SCIURO_ORCHESTRATOR: Direction unknown for event ${rawEvent.id}. Marking dead letter.")
                rawEventRepository.markDeadLetter(rawEvent.id, "Direction could not be determined")
                return
            }

            val categoryId = guessCategoryId(draft.merchant)

            val matchedAccount = accountRepository.getAccountByPackageName(rawEvent.sourcePackageOrAddress)
            val accountId = matchedAccount?.id

            val extractionMethod = if (draft.confidenceScore >= confidenceThreshold) "REGEX" else "LLM_FALLBACK"

            val transaction = Transaction(
                id = generateUuid(),
                accountId = accountId,
                categoryId = categoryId,
                amount = draft.amount,
                direction = directionName,
                merchant = draft.merchant,
                timestamp = draft.timestamp,
                referenceId = draft.referenceId,
                isReviewed = draft.confidenceScore >= confidenceThreshold && categoryId != null && accountId != null,
                extractionMethod = extractionMethod,
                confidence = draft.confidenceScore.toDouble(),
                rawEventId = rawEvent.id
            )

            println("SCIURO_ORCHESTRATOR: Booking transaction... categoryId=$categoryId, accountId=$accountId, isReviewed=${transaction.isReviewed}")

            val auditSource = if (draft.confidenceScore >= confidenceThreshold) AuditSource.SYSTEM_AUTO else AuditSource.LLM_INFERRED
            transactionRepository.bookTransaction(transaction, source = auditSource)
            println("SCIURO_ORCHESTRATOR: Successfully booked transaction!")

            transferDetectionEngine.onTransactionBooked(
                newTxId = transaction.id,
                newTxAccountId = transaction.accountId,
                newTxAmount = transaction.amount,
                newTxDirection = transaction.direction,
                newTxTimestamp = transaction.timestamp,
                counterpartyAccountNumber = draft.counterpartyAccountNumber
            )

            obligationCycleMatcher.onTransactionBooked(
                transactionId = transaction.id,
                amount = transaction.amount,
                direction = transaction.direction,
                categoryId = transaction.categoryId,
                merchant = transaction.merchant,
                timestamp = transaction.timestamp
            )

            budgetEngine.processBudgets()
            debtEngine.processDebtPayments()

            investmentEngine.processInvestments()
            obligationDetectionEngine.runDetection()

            rawEventRepository.markProcessed(rawEvent.id)
        } catch (e: Exception) {
            println("SCIURO_ORCHESTRATOR: Error processing event ${rawEvent.id} - ${e.message}")
            e.printStackTrace()
            rawEventRepository.markDeadLetter(rawEvent.id, "Unhandled exception: ${e.message}")
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
