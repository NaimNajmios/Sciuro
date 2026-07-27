package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.MultiplexIngestionSource
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SciuroIngestionOrchestrator(
    private val ingestionSource: MultiplexIngestionSource,
    private val rawEventRepository: RawEventRepository,
    private val bookingUseCase: TransactionBookingUseCase,
    private val engineTriggerUseCase: EngineTriggerUseCase,
    private val tracer: PipelineTracer
) {
    private var job: Job? = null

    companion object {
        private const val PROCESSING_STALE_MS = 60_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val MAX_CONCURRENT_RECOVERY = 8
    }

    private val recoverySemaphore = Semaphore(MAX_CONCURRENT_RECOVERY)

    fun startListening(scope: CoroutineScope) {
        if (job?.isActive == true) return

        job = scope.launch {
            recoverStrandedEvents()
            collectEventsWithRetry()
        }
    }

    private suspend fun recoverStrandedEvents() = coroutineScope {
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
            launch {
                recoverySemaphore.withPermit {
                    processOneEvent(rawEvent)
                }
            }
        }
    }

    private suspend fun collectEventsWithRetry() = coroutineScope {
        var attempt = 0
        while (true) {
            try {
                ingestionSource.observeEvents().collect { rawEvent ->
                    launch {
                        processOneEvent(rawEvent)
                    }
                }
            } catch (e: CancellationException) {
                throw e
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
            val result = bookingUseCase.book(rawEvent)
            if (result == null) {
                rawEventRepository.markDeadLetter(rawEvent.id, "Booking returned null")
                tracer.trace(rawEvent.id, null, TraceStage.STAGING, TraceOutcome.FAILURE,
                    detail = mapOf("transition" to "DEAD_LETTER", "reason" to "booking_returned_null"))
                return
            }

            engineTriggerUseCase.triggerAll(
                transaction = result.transaction,
                draft = result.draft,
                rawEventId = result.rawEventId
            )

            rawEventRepository.markProcessed(rawEvent.id)
            tracer.trace(rawEvent.id, result.transaction.id, TraceStage.STAGING, TraceOutcome.SUCCESS,
                detail = mapOf("transition" to "PROCESSED"))
        } catch (e: CancellationException) {
            throw e
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
