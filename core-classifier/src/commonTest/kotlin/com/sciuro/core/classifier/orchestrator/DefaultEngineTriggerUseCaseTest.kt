package com.sciuro.core.classifier.orchestrator

import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.model.AuditLog
import com.sciuro.core.audit.model.EntityType
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.budget.engine.BudgetEngine
import com.sciuro.core.debt.engine.BnplRiskDetector
import com.sciuro.core.debt.engine.DebtEngine
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.investment.engine.InvestmentEngine
import com.sciuro.core.ledger.config.LlmParsingConfig
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.engine.TransactionMatchingEngine
import com.sciuro.core.ledger.model.Transaction
import com.sciuro.core.obligations.engine.ObligationCycleMatcher
import com.sciuro.core.obligations.engine.ObligationDetectionEngine
import com.sciuro.core.parsing.model.StructuredDraft
import com.sciuro.core.transfer.engine.TransferDetectionEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultEngineTriggerUseCaseTest {

    private val dummyDb = object : SciuroDatabase {
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
        override val domainEventLogQueries get() = throw UnsupportedOperationException()
        override val domainEventDeliveryQueries get() = throw UnsupportedOperationException()
        override fun transaction(noEnclosing: Boolean, body: app.cash.sqldelight.TransactionWithoutReturn.() -> Unit) {}
        override fun <R> transactionWithResult(noEnclosing: Boolean, bodyWithReturn: app.cash.sqldelight.TransactionWithReturn<R>.() -> R): R = throw UnsupportedOperationException()
    }

    private fun buildUseCase(
        settingsProvider: FakeSettingsProvider,
        tracer: RecordingTracer
    ): DefaultEngineTriggerUseCase {
        val eventBus = DomainEventBus()
        val matchingEngine = TransactionMatchingEngine(dummyDb)

        return DefaultEngineTriggerUseCase(
            transferDetectionEngine = object : TransferDetectionEngine(dummyDb, throw UnsupportedOperationException(), eventBus) {},
            obligationCycleMatcher = object : ObligationCycleMatcher(dummyDb, throw UnsupportedOperationException(), eventBus) {},
            budgetEngine = object : BudgetEngine(dummyDb, eventBus, matchingEngine) {},
            debtEngine = object : DebtEngine(dummyDb, throw UnsupportedOperationException(), eventBus, matchingEngine) {},
            investmentEngine = object : InvestmentEngine(dummyDb, eventBus, matchingEngine) {},
            obligationDetectionEngine = object : ObligationDetectionEngine(dummyDb, throw UnsupportedOperationException(), eventBus, settingsProvider) {},
            bnplRiskDetector = object : BnplRiskDetector(throw UnsupportedOperationException(), eventBus) {},
            tracer = tracer,
            settingsProvider = settingsProvider
        )
    }

    @Test
    fun `first triggerAll invokes all 7 engines`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer = RecordingTracer()
        val useCase = buildUseCase(settings, tracer)
        useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_1")

        val engineTraces = tracer.traces.filter { it.stage == TraceStage.ENGINE }
        assertEquals(7, engineTraces.size, "Should have 7 ENGINE traces on first call")

        val engineNames = engineTraces.map { it.detail?.get("engine") }.toSet()
        assertEquals(
            setOf("transfer", "obligation_cycle", "budget", "debt", "investment", "obligation_detect", "bnpl"),
            engineNames
        )
    }

    @Test
    fun `immediate second triggerAll invokes no engines`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer = RecordingTracer()
        val useCase = buildUseCase(settings, tracer)

        useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_1")
        val firstCount = tracer.traces.count { it.stage == TraceStage.ENGINE }

        useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_2")
        val secondCount = tracer.traces.count { it.stage == TraceStage.ENGINE }

        assertEquals(firstCount, secondCount, "Second call should not invoke any engines")
    }

    @Test
    fun `timestamps are persisted per engine`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer = RecordingTracer()
        val useCase = buildUseCase(settings, tracer)
        useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_1")

        val engineNames = listOf("transfer", "obligation_cycle", "budget", "debt", "investment", "obligation_detect", "bnpl")
        for (name in engineNames) {
            assertTrue(settings.getEngineLastRunMs(name) > 0, "$name timestamp should be persisted")
        }
    }

    @Test
    fun `persisted timestamps survive new use case instance`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer1 = RecordingTracer()
        val useCase1 = buildUseCase(settings, tracer1)

        useCase1.triggerAll(dummyTransaction(), dummyDraft(), "evt_1")
        val firstCount = tracer1.traces.count { it.stage == TraceStage.ENGINE }

        val tracer2 = RecordingTracer()
        val useCase2 = buildUseCase(settings, tracer2)
        useCase2.triggerAll(dummyTransaction(), dummyDraft(), "evt_2")
        val secondCount = tracer2.traces.count { it.stage == TraceStage.ENGINE }

        assertEquals(0, secondCount, "New instance should respect persisted timestamps")
        assertEquals(firstCount, 7, "First instance should have triggered all 7")
    }

    @Test
    fun `all 7 engines are attempted even if some fail`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer = RecordingTracer()
        val useCase = buildUseCase(settings, tracer)
        useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_1")

        val engineTraces = tracer.traces.filter { it.stage == TraceStage.ENGINE }
        assertEquals(7, engineTraces.size, "All 7 engine traces should be emitted even if some fail")
    }

    @Test
    fun `concurrent triggerAll calls do not duplicate engine invocations`() = runBlocking {
        val settings = FakeSettingsProvider()
        val tracer = RecordingTracer()
        val useCase = buildUseCase(settings, tracer)

        val jobs = (1..4).map { i ->
            launch {
                useCase.triggerAll(dummyTransaction(), dummyDraft(), "evt_concurrent_$i")
            }
        }
        jobs.forEach { it.join() }

        val engineTraces = tracer.traces.filter { it.stage == TraceStage.ENGINE }
        assertEquals(7, engineTraces.size, "Concurrent calls should produce exactly 7 engine traces total")
    }

    private fun dummyTransaction() = Transaction(
        id = "tx_1",
        accountId = null,
        categoryId = null,
        amount = 50.0,
        direction = "OUTFLOW",
        merchant = "TestMerchant",
        timestamp = 1000L,
        referenceId = null,
        isReviewed = false,
        extractionMethod = "REGEX",
        confidence = 0.95,
        rawEventId = "evt_1",
        reviewTier = "auto",
        autoConfirmedAt = null
    )

    private fun dummyDraft() = StructuredDraft(
        amount = 50.0,
        merchant = "TestMerchant",
        referenceId = null,
        timestamp = 1000L,
        confidenceScore = 0.95f,
        direction = null,
        accountOrChannel = null,
        counterpartyAccountNumber = null
    )
}

private class FakeSettingsProvider : SettingsProvider {
    private val engineTimestamps = mutableMapOf<String, Long>()

    override fun getEngineLastRunMs(engineName: String): Long = engineTimestamps[engineName] ?: 0L
    override fun setEngineLastRunMs(engineName: String, timestampMs: Long) { engineTimestamps[engineName] = timestampMs }

    override fun getLlmModelName(): String = ""
    override fun setLlmModelName(name: String) {}
    override fun getQuickLabels(): List<String> = emptyList()
    override fun setQuickLabels(labels: List<String>) {}
    override fun getBudgetWarningThreshold(): Float = 0.8f
    override fun setBudgetWarningThreshold(threshold: Float) {}
    override fun isLlmEnabled(): Boolean = true
    override fun setLlmEnabled(enabled: Boolean) {}
    override fun getApiKey(): String? = null
    override fun setApiKey(apiKey: String) {}
    override fun isLockEnabled(): Boolean = false
    override fun setLockEnabled(enabled: Boolean) {}
    override fun isObligationAutoConfirmEnabled(): Boolean = false
    override fun setObligationAutoConfirmEnabled(enabled: Boolean) {}
    override fun getAutoConfirmThreshold(): Int = 3
    override fun setAutoConfirmThreshold(threshold: Int) {}
    override fun getSilentAutoConfirmThreshold(): Float = 0.8f
    override fun setSilentAutoConfirmThreshold(threshold: Float) {}
    override fun isTransactionAutoConfirmEnabled(): Boolean = false
    override fun setTransactionAutoConfirmEnabled(enabled: Boolean) {}
    override fun isTrustValidatedLlmEnabled(): Boolean = false
    override fun setTrustValidatedLlmEnabled(enabled: Boolean) {}
    override fun getManualPrice(key: String): Double? = null
    override fun setManualPrice(key: String, price: Double) {}
    override fun isQuietHoursEnabled(): Boolean = false
    override fun setQuietHoursEnabled(enabled: Boolean) {}
    override fun getQuietHoursStart(): Int = 22
    override fun setQuietHoursStart(hour: Int) {}
    override fun getQuietHoursEnd(): Int = 7
    override fun setQuietHoursEnd(hour: Int) {}
    override fun hasSeenBatteryPrompt(): Boolean = false
    override fun setHasSeenBatteryPrompt(hasSeen: Boolean) {}
    override fun isDeveloperOptionsVisible(): Boolean = false
    override fun setDeveloperOptionsVisible(visible: Boolean) {}
    override fun getLlmConfig(): LlmParsingConfig = LlmParsingConfig()
    override fun getLastBackupTimestamp(): Long = 0L
    override fun setLastBackupTimestamp(timestamp: Long) {}
    override fun getLastMilestoneReached(): Double = 0.0
    override fun setLastMilestoneReached(milestone: Double) {}
    override fun hasCompletedOnboarding(): Boolean = false
    override fun setHasCompletedOnboarding(completed: Boolean) {}
    override fun hasSeenDashboardTips(): Boolean = false
    override fun setHasSeenDashboardTips(seen: Boolean) {}
    override fun getIngestionAllowlistAdditions(): Set<String> = emptySet()
    override fun setIngestionAllowlistAdditions(packages: Set<String>) {}
    override fun getIngestionAllowlistRemovals(): Set<String> = emptySet()
    override fun setIngestionAllowlistRemovals(packages: Set<String>) {}
}

private class RecordingTracer : PipelineTracer {
    data class TraceEntry(
        val rawEventId: String,
        val transactionId: String?,
        val stage: TraceStage,
        val outcome: TraceOutcome,
        val detail: Map<String, String?>? = null
    )

    @Volatile
    var traces = mutableListOf<TraceEntry>()
        private set

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
        synchronized(traces) {
            traces.add(TraceEntry(rawEventId ?: "", transactionId, stage, outcome, detail))
        }
    }
}
