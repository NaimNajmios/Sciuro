package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.model.AuditSource
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.classifier.rule.CategoryResolver
import com.sciuro.core.classifier.rule.ReviewTierDecider
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.parsing.engine.SciuroParserPipeline
import com.sciuro.core.parsing.model.StructuredDraft

data class BookingResult(
    val transaction: Transaction,
    val draft: StructuredDraft,
    val rawEventId: String
)

class TransactionBookingUseCase(
    private val parserPipeline: SciuroParserPipeline,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val rawEventRepository: RawEventRepository,
    private val categoryResolver: CategoryResolver,
    private val reviewTierDecider: ReviewTierDecider,
    private val tracer: PipelineTracer,
    private val confidenceThreshold: Float
) {
    suspend fun book(rawEvent: RawEvent): BookingResult? {
        rawEventRepository.markProcessing(rawEvent.id)

        val staging = rawEventRepository.getRawEventById(rawEvent.id)
        if (staging != null && staging.attempt_count > MAX_ATTEMPTS) {
            rawEventRepository.markDeadLetter(rawEvent.id, "Max attempt count exceeded (${staging.attempt_count})")
            tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.FAILURE,
                detail = mapOf("transition" to "DEAD_LETTER", "reason" to "max_attempts", "attempts" to "${staging.attempt_count}"))
            return null
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
            return null
        }

        val parseStage = if (draft.confidenceScore >= confidenceThreshold) TraceStage.PARSE_REGEX else TraceStage.PARSE_LLM
        tracer.trace(rawEvent.id, null, parseStage, TraceOutcome.SUCCESS,
            durationMs = parseMs, confidence = draft.confidenceScore,
            detail = mapOf("merchant" to draft.merchant, "direction" to draft.direction?.name, "amount" to "${draft.amount}"))

        val directionName = draft.direction?.name ?: run {
            rawEventRepository.markDeadLetter(rawEvent.id, "Direction could not be determined")
            tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.DROP,
                detail = mapOf("transition" to "DEAD_LETTER", "reason" to "unknown_direction"))
            return null
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
            return null
        }

        val categoryId = categoryResolver.resolve(draft.merchant)
        tracer.trace(rawEvent.id, null, TraceStage.CATEGORIZE,
            if (categoryId != null) TraceOutcome.SUCCESS else TraceOutcome.SKIP,
            detail = mapOf("category_id" to categoryId, "merchant" to draft.merchant))

        val accountId = matchAccount(rawEvent, draft)
        tracer.trace(rawEvent.id, null, TraceStage.ACCOUNT_MATCH,
            if (accountId != null) TraceOutcome.SUCCESS else TraceOutcome.SKIP,
            detail = mapOf("account_id" to accountId, "package" to rawEvent.sourcePackageOrAddress, "account_channel" to draft.accountOrChannel))

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

        return BookingResult(transaction = transaction, draft = draft, rawEventId = rawEvent.id)
    }

    private suspend fun matchAccount(rawEvent: RawEvent, draft: StructuredDraft): String? {
        var matchedAccount = accountRepository.getAccountByPackageName(rawEvent.sourcePackageOrAddress)
        if (matchedAccount == null) {
            val accChannel = draft.accountOrChannel
            if (!accChannel.isNullOrBlank()) {
                val suffixOnly = accChannel.takeLast(4).filter { it.isDigit() }
                if (suffixOnly.isNotEmpty()) {
                    matchedAccount = accountRepository.getAccountByNumberSuffix(suffixOnly)
                }
            }
        }
        return matchedAccount?.id
    }

    companion object {
        private const val MAX_ATTEMPTS = 3L
    }
}
