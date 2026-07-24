package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ingestion.source.MultiplexIngestionSource
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
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import com.sciuro.core.classifier.rule.CategoryResolver
import com.sciuro.core.classifier.rule.ReviewTierDecider
import com.sciuro.core.debt.engine.BnplRiskDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SciuroIngestionOrchestrator(
    private val ingestionSource: MultiplexIngestionSource,
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
    private val categoryResolver: CategoryResolver,
    private val bnplRiskDetector: BnplRiskDetector,
    private val reviewTierDecider: ReviewTierDecider,
    private val tracer: PipelineTracer,
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    private var job: Job? = null

    companion object {
        private const val MAX_ATTEMPTS = 3L
        private const val PROCESSING_STALE_MS = 60_000L
        private const val MAX_BACKOFF_MS = 60_000L
    }

    fun startListening(scope: CoroutineScope) {
        if (job?.isActive == true) return

        job = scope.launch {
            recoverStrandedEvents()
            collectEventsWithRetry()
        }
    }

    private suspend fun recoverStrandedEvents() {
        val staleThreshold = currentTimeMillis() - PROCESSING_STALE_MS
        val strandedEvents = rawEventRepository.getStrandedEvents(staleThreshold)
        for (staging in strandedEvents) {
            val rawEvent = RawEvent(
                id = staging.id,
                sourceType = parseSourceType(staging.source_type),
                sourcePackageOrAddress = staging.source_package_or_address,
                title = staging.title,
                text = staging.text,
                timestamp = staging.timestamp
            )
            processOneEvent(rawEvent)
        }
    }

    private suspend fun collectEventsWithRetry() {
        var attempt = 0
        while (true) {
            try {
                ingestionSource.observeEvents().collect { rawEvent ->
                    processOneEvent(rawEvent)
                }
            } catch (_: Exception) {
                attempt++
                val delayMs = minOf(1000L * (1L shl minOf(attempt, 5)), MAX_BACKOFF_MS)
                delay(delayMs)
            }
        }
    }

    private fun parseSourceType(value: String): SourceType {
        return try {
            SourceType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            SourceType.NOTIFICATION
        }
    }

    private suspend fun processOneEvent(rawEvent: RawEvent) {
        try {
            rawEventRepository.markProcessing(rawEvent.id)

            val staging = rawEventRepository.getRawEventById(rawEvent.id)
            if (staging != null && staging.attempt_count > MAX_ATTEMPTS) {
                rawEventRepository.markDeadLetter(rawEvent.id, "Max attempt count exceeded (${staging.attempt_count})")
                tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.FAILURE,
                    detail = mapOf("transition" to "DEAD_LETTER", "reason" to "max_attempts", "attempts" to "${staging.attempt_count}"))
                return
            }

            tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.SUCCESS,
                detail = mapOf("transition" to "PROCESSING", "source" to rawEvent.sourcePackageOrAddress))

            val parseStart = currentTimeMillis()
            val draft = parserPipeline.process(rawEvent)
            val parseMs = currentTimeMillis() - parseStart

            if (draft == null) {
                rawEventRepository.markDeadLetter(rawEvent.id, "Parser pipeline returned null")
                tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.DROP,
                    detail = mapOf("transition" to "DEAD_LETTER", "reason" to "parser_null", "duration_ms" to "$parseMs"))
                return
            }

            val parseStage = if (draft.confidenceScore >= confidenceThreshold) TraceStage.PARSE_REGEX else TraceStage.PARSE_LLM
            tracer.trace(rawEvent.id, null, parseStage, TraceOutcome.SUCCESS,
                durationMs = parseMs, confidence = draft.confidenceScore,
                detail = mapOf("merchant" to draft.merchant, "direction" to draft.direction?.name, "amount" to "${draft.amount}"))

            val directionName = draft.direction?.name ?: run {
                rawEventRepository.markDeadLetter(rawEvent.id, "Direction could not be determined")
                tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.DROP,
                    detail = mapOf("transition" to "DEAD_LETTER", "reason" to "unknown_direction"))
                return
            }

            val duplicate = transactionRepository.findLikelyDuplicate(
                amount = draft.amount,
                direction = directionName,
                timestamp = draft.timestamp
            )
            if (duplicate != null) {
                transactionRepository.attachCorroboratingSource(duplicate.id, rawEvent.id)
                rawEventRepository.markProcessed(rawEvent.id)
                tracer.trace(rawEvent.id, duplicate.id, TraceStage.DEDUP, TraceOutcome.SKIP,
                    detail = mapOf("duplicate_id" to duplicate.id))
                return
            }

            val categoryId = categoryResolver.resolve(draft.merchant)
            tracer.trace(rawEvent.id, null, TraceStage.CATEGORIZE,
                if (categoryId != null) TraceOutcome.SUCCESS else TraceOutcome.SKIP,
                detail = mapOf("category_id" to categoryId, "merchant" to draft.merchant))

            val matchedAccount = accountRepository.getAccountByPackageName(rawEvent.sourcePackageOrAddress)
            val accountId = matchedAccount?.id
            tracer.trace(rawEvent.id, null, TraceStage.ACCOUNT_MATCH,
                if (accountId != null) TraceOutcome.SUCCESS else TraceOutcome.SKIP,
                detail = mapOf("account_id" to accountId, "package" to rawEvent.sourcePackageOrAddress))

            val extractionMethod = if (draft.confidenceScore >= confidenceThreshold) "REGEX" else "LLM_FALLBACK"

            val tier = reviewTierDecider.decide(
                confidence = draft.confidenceScore,
                categoryId = categoryId,
                accountId = accountId,
                merchant = draft.merchant
            )
            val nowAuto = currentTimeMillis()
            val isReviewed = tier != com.sciuro.core.audit.model.ReviewTier.MANUAL

            val transaction = Transaction(
                id = generateUuid(),
                accountId = accountId,
                categoryId = categoryId,
                amount = draft.amount,
                direction = directionName,
                merchant = draft.merchant,
                timestamp = draft.timestamp,
                referenceId = draft.referenceId,
                isReviewed = isReviewed,
                extractionMethod = extractionMethod,
                confidence = draft.confidenceScore.toDouble(),
                rawEventId = rawEvent.id,
                reviewTier = tier.label,
                autoConfirmedAt = if (isReviewed) nowAuto else null
            )

            val auditSource = if (draft.confidenceScore >= confidenceThreshold) AuditSource.SYSTEM_AUTO else AuditSource.LLM_INFERRED
            transactionRepository.bookTransaction(transaction, source = auditSource)
            tracer.trace(rawEvent.id, transaction.id, TraceStage.BOOK, TraceOutcome.SUCCESS,
                confidence = draft.confidenceScore,
                detail = mapOf("is_reviewed" to "${transaction.isReviewed}", "review_tier" to tier.label, "extraction_method" to extractionMethod))

            transferDetectionEngine.onTransactionBooked(
                newTxId = transaction.id,
                newTxAccountId = transaction.accountId,
                newTxAmount = transaction.amount,
                newTxDirection = transaction.direction,
                newTxTimestamp = transaction.timestamp,
                counterpartyAccountNumber = draft.counterpartyAccountNumber
            )
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "transfer"))

            obligationCycleMatcher.onTransactionBooked(
                transactionId = transaction.id,
                amount = transaction.amount,
                direction = transaction.direction,
                categoryId = transaction.categoryId,
                merchant = transaction.merchant
            )
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "obligation_cycle"))

            budgetEngine.processBudgets()
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "budget"))

            debtEngine.processDebtPayments()
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "debt"))

            investmentEngine.processInvestments()
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "investment"))

            obligationDetectionEngine.runDetection()
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "obligation_detect"))

            bnplRiskDetector.evaluate()
            tracer.trace(rawEvent.id, transaction.id, TraceStage.ENGINE, TraceOutcome.SUCCESS,
                detail = mapOf("engine" to "bnpl"))

            rawEventRepository.markProcessed(rawEvent.id)
            tracer.trace(rawEvent.id, transaction.id, TraceStage.STAGING, TraceOutcome.SUCCESS,
                detail = mapOf("transition" to "PROCESSED"))
        } catch (e: Exception) {
            rawEventRepository.markDeadLetter(rawEvent.id, "Unhandled exception: ${e.message}")
            tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.FAILURE,
                detail = mapOf("transition" to "DEAD_LETTER", "reason" to "exception", "message" to e.message))
        }
    }

    fun stopListening() {
        job?.cancel()
        job = null
    }
}
