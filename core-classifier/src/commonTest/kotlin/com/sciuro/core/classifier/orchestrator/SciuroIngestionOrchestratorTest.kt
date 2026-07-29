package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.IngestionSource
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.parsing.model.StructuredDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SciuroIngestionOrchestratorTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var orchestrator: SciuroIngestionOrchestrator
    private lateinit var fakeIngestionSource: FakeIngestionSource
    private lateinit var fakeRawEventRepository: FakeRawEventRepository
    private lateinit var fakeBookingUseCase: FakeBookingUseCase
    private lateinit var fakeEngineTriggerUseCase: FakeEngineTriggerUseCase
    private lateinit var fakeTracer: FakePipelineTracer

    @AfterTest
    fun tearDown() {
        orchestrator.stopListening()
    }

    @Test
    fun `startListening recovers stranded events`() = runBlocking {
        fakeIngestionSource = FakeIngestionSource()
        fakeRawEventRepository = FakeRawEventRepository(strandedCount = 2)
        fakeBookingUseCase = FakeBookingUseCase(shouldSucceed = true)
        fakeEngineTriggerUseCase = FakeEngineTriggerUseCase()
        fakeTracer = FakePipelineTracer()
        orchestrator = SciuroIngestionOrchestrator(
            ingestionSource = com.sciuro.core.ingestion.source.MultiplexIngestionSource(listOf(fakeIngestionSource)),
            rawEventRepository = fakeRawEventRepository,
            bookingUseCase = fakeBookingUseCase,
            engineTriggerUseCase = fakeEngineTriggerUseCase,
            tracer = fakeTracer
        )

        orchestrator.startListening(scope)

        // Wait until both stranded events are processed or timeout
        var waited = 0
        while (waited < 50 && (fakeBookingUseCase.bookedCount < 2 || fakeEngineTriggerUseCase.triggeredCount < 2)) {
            delay(100)
            waited++
        }

        assertEquals(2, fakeBookingUseCase.bookedCount, "Should have processed 2 stranded events")
        assertEquals(2, fakeEngineTriggerUseCase.triggeredCount, "Should have triggered engines for 2 events")
        assertEquals(2, fakeRawEventRepository.processedCount, "Should have marked 2 events as processed")
        assertTrue(fakeTracer.traces.any { it.stage == TraceStage.STAGING && it.outcome == TraceOutcome.SUCCESS })
    }

    @Test
    fun `stranded event goes to dead letter when booking returns null`() = runBlocking {
        fakeIngestionSource = FakeIngestionSource()
        fakeRawEventRepository = FakeRawEventRepository(strandedCount = 1)
        fakeBookingUseCase = FakeBookingUseCase(shouldSucceed = false)
        fakeEngineTriggerUseCase = FakeEngineTriggerUseCase()
        fakeTracer = FakePipelineTracer()
        orchestrator = SciuroIngestionOrchestrator(
            ingestionSource = com.sciuro.core.ingestion.source.MultiplexIngestionSource(listOf(fakeIngestionSource)),
            rawEventRepository = fakeRawEventRepository,
            bookingUseCase = fakeBookingUseCase,
            engineTriggerUseCase = fakeEngineTriggerUseCase,
            tracer = fakeTracer
        )

        orchestrator.startListening(scope)

        var waited = 0
        while (waited < 50 && fakeRawEventRepository.deadLetterCount < 1) {
            delay(100)
            waited++
        }

        assertEquals(0, fakeBookingUseCase.bookedCount)
        assertEquals(0, fakeEngineTriggerUseCase.triggeredCount)
        assertEquals(1, fakeRawEventRepository.deadLetterCount)
    }

    @Test
    fun `realtime event from source is processed`() = runBlocking {
        val eventFlow = MutableSharedFlow<RawEvent>(extraBufferCapacity = 10)
        fakeIngestionSource = FakeIngestionSource(events = eventFlow)
        fakeRawEventRepository = FakeRawEventRepository(strandedCount = 0)
        fakeBookingUseCase = FakeBookingUseCase(shouldSucceed = true)
        fakeEngineTriggerUseCase = FakeEngineTriggerUseCase()
        fakeTracer = FakePipelineTracer()
        orchestrator = SciuroIngestionOrchestrator(
            ingestionSource = com.sciuro.core.ingestion.source.MultiplexIngestionSource(listOf(fakeIngestionSource)),
            rawEventRepository = fakeRawEventRepository,
            bookingUseCase = fakeBookingUseCase,
            engineTriggerUseCase = fakeEngineTriggerUseCase,
            tracer = fakeTracer
        )

        orchestrator.startListening(scope)
        delay(200)

        eventFlow.emit(RawEvent(
            id = "realtime_1",
            sourceType = SourceType.NOTIFICATION,
            sourcePackageOrAddress = "com.test.bank",
            title = "Payment",
            text = "RM50 paid",
            timestamp = 1000L
        ))

        var waited = 0
        while (waited < 50 && (fakeBookingUseCase.bookedCount < 1 || fakeEngineTriggerUseCase.triggeredCount < 1)) {
            delay(100)
            waited++
        }

        assertTrue(fakeBookingUseCase.bookedCount >= 1, "Should have processed the realtime event")
        assertTrue(fakeEngineTriggerUseCase.triggeredCount >= 1, "Should have triggered engines for realtime event")
    }
}

private class FakeIngestionSource(
    private val events: MutableSharedFlow<RawEvent>? = null
) : IngestionSource {
    override val sourceType: SourceType = SourceType.NOTIFICATION

    override fun observeEvents(): Flow<RawEvent> {
        return events ?: MutableSharedFlow<RawEvent>(extraBufferCapacity = 10)
    }
}

private val dummyDb = object : com.sciuro.core.ledger.db.SciuroDatabase {
    override val accountQueries get() = throw UnsupportedOperationException()
    override val auditLogQueries get() = throw UnsupportedOperationException()
    override val budgetQueries get() = throw UnsupportedOperationException()
    override val cashAdjustmentQueries get() = throw UnsupportedOperationException()
    override val categoryQueries get() = throw UnsupportedOperationException()
    override val debtQueries get() = throw UnsupportedOperationException()
    override val debtPaymentLinkQueries get() = throw UnsupportedOperationException()
    override val investmentQueries get() = throw UnsupportedOperationException()
    override val merchantAccountRuleQueries get() = throw UnsupportedOperationException()
    override val merchantCategoryRuleQueries get() = throw UnsupportedOperationException()
    override val obligationQueries get() = throw UnsupportedOperationException()
    override val pipelineTraceQueries get() = throw UnsupportedOperationException()
    override val rawEventStagingQueries get() = throw UnsupportedOperationException()
    override val transactionCorroborationQueries get() = throw UnsupportedOperationException()
    override val transactionRecordQueries get() = throw UnsupportedOperationException()
    override val transferLinkQueries get() = throw UnsupportedOperationException()
    override fun transaction(noEnclosing: Boolean, body: app.cash.sqldelight.TransactionWithoutReturn.() -> Unit) {}
    override fun <R> transactionWithResult(noEnclosing: Boolean, bodyWithReturn: app.cash.sqldelight.TransactionWithReturn<R>.() -> R): R = throw UnsupportedOperationException()
}

private class FakeRawEventRepository(
    private val strandedCount: Int = 0
) : RawEventRepository(dummyDb) {
    var processedCount = 0
    var deadLetterCount = 0

    override suspend fun getStrandedEvents(staleProcessedBeforeMs: Long): List<com.sciuro.core.ledger.db.Raw_event_staging> {
        if (strandedCount == 0) return emptyList()
        return (1..strandedCount).map { i ->
            com.sciuro.core.ledger.db.Raw_event_staging(
                id = "stranded_$i",
                source_type = "NOTIFICATION",
                source_package_or_address = "com.test.bank",
                title = "Bank Alert",
                text = "RM$i received",
                timestamp = 1000L,
                status = "PROCESSING",
                attempt_count = 1L,
                captured_at = 1000L,
                last_error = null,
                processed_at = null
            )
        }
    }

    override suspend fun persistRawEvent(
        id: String, sourceType: String, sourcePackageOrAddress: String,
        title: String, text: String, timestamp: Long, capturedAt: Long
    ) {}

    override suspend fun markProcessing(id: String, error: String?) {}
    override suspend fun markProcessed(id: String) { processedCount++ }
    override suspend fun markDeadLetter(id: String, error: String) { deadLetterCount++ }
    override suspend fun requeueRawEvent(id: String) {}
    override suspend fun getRawEventById(id: String): com.sciuro.core.ledger.db.Raw_event_staging? = null
    override suspend fun countStrandedEvents(staleProcessedBeforeMs: Long): Long = strandedCount.toLong()
    override suspend fun purgeOldTraces(beforeMs: Long) {}
}

private class FakeBookingUseCase(
    private val shouldSucceed: Boolean
) : TransactionBookingUseCase {
    var bookedCount = 0

    override suspend fun book(rawEvent: RawEvent): BookingResult? {
        if (shouldSucceed) {
            bookedCount++
            return BookingResult(
                transaction = Transaction(
                    id = "tx_${rawEvent.id}",
                    accountId = null,
                    categoryId = null,
                    amount = 0.0,
                    direction = "OUTFLOW",
                    merchant = rawEvent.title,
                    timestamp = rawEvent.timestamp,
                    referenceId = null,
                    isReviewed = false,
                    extractionMethod = "REGEX",
                    confidence = 0.95,
                    rawEventId = rawEvent.id,
                    reviewTier = "manual",
                    autoConfirmedAt = null
                ),
                draft = StructuredDraft(
                    amount = 0.0,
                    merchant = rawEvent.title,
                    referenceId = null,
                    timestamp = rawEvent.timestamp,
                    confidenceScore = 0.95f,
                    direction = null,
                    accountOrChannel = null,
                    counterpartyAccountNumber = null
                ),
                rawEventId = rawEvent.id
            )
        } else {
            return null
        }
    }
}

private class FakeEngineTriggerUseCase : EngineTriggerUseCase {
    var triggeredCount = 0

    override suspend fun triggerAll(
        transaction: Transaction,
        draft: StructuredDraft,
        rawEventId: String
    ) {
        triggeredCount++
    }
}

private class FakePipelineTracer : PipelineTracer {
    data class TraceEntry(
        val rawEventId: String,
        val transactionId: String?,
        val stage: TraceStage,
        val outcome: TraceOutcome,
        val durationMs: Long? = null,
        val confidence: Float? = null,
        val detail: Map<String, String?>? = null,
        val packageName: String? = null
    )

    val traces = mutableListOf<TraceEntry>()

    override suspend fun trace(
        rawEventId: String?,
        transactionId: String?,
        stage: TraceStage,
        outcome: TraceOutcome,
        durationMs: Long?,
        confidence: Float?,
        detail: Map<String, String?>?,
        packageName: String?
    ) {
        traces.add(TraceEntry(rawEventId ?: "", transactionId, stage, outcome, durationMs, confidence, detail, packageName))
    }
}
