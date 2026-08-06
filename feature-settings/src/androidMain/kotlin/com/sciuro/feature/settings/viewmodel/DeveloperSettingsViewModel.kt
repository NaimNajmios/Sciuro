package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.parsing.engine.SimulationEngine
import com.sciuro.core.parsing.engine.SimulationResult
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.audit.repository.AuditRepository
import com.sciuro.core.parsing.fixture.FixtureLibrary
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEventMetrics
import com.sciuro.core.parsing.metrics.ParserHealthRepository
import com.sciuro.core.ledger.security.DatabaseRecoveryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID

data class TraceEventSummary(
    val rawEventId: String?,
    val firstAt: Long?,
    val lastAt: Long?,
    val stageCount: Long,
    val packageName: String?
)

data class TraceRow(
    val stage: String,
    val outcome: String,
    val durationMs: Long?,
    val confidence: Double?,
    val detail: String?,
    val createdAt: Long
)

data class PipelineMetrics(
    val llmCalls: Long,
    val deadLetters: Long
)

data class DatabaseRecoveryMetrics(
    val quarantineCount: Int,
    val lastQuarantineTimestamp: Long,
    val quarantinedFileCount: Int,
    val lastIntegrityCheckMs: Long,
    val lastIntegrityResult: String?
)

class DeveloperSettingsViewModel(
    private val notificationSourceAdapter: NotificationSourceAdapter,
    private val transactionRepository: TransactionRepository,
    private val simulationEngine: SimulationEngine,
    private val rawEventRepository: RawEventRepository,
    val ingestionAllowlist: MutableIngestionAllowlist,
    val parserHealthRepository: ParserHealthRepository,
    val database: SciuroDatabase,
    private val tracer: PipelineTracer,
    private val auditRepository: AuditRepository,
    private val eventBus: DomainEventBus? = null,
    private val recoveryManager: DatabaseRecoveryManager
) : ViewModel() {

    private val _simulationResult = MutableStateFlow<SimulationResult?>(null)
    val simulationResult: StateFlow<SimulationResult?> = _simulationResult.asStateFlow()

    // Dead-letter pagination
    private val _deadLetterLimit = MutableStateFlow(50L)
    private val _showReadDeadLetters = MutableStateFlow(false)
    private val _totalFilteredDeadLetterCount = MutableStateFlow(0L)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val deadLetterEvents: StateFlow<List<Raw_event_staging>> = combine(
        _deadLetterLimit, _showReadDeadLetters
    ) { limit, showRead ->
        rawEventRepository.observeDeadLetterEventsPaginated(limit, 0L, showRead)
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasMoreDeadLetters: StateFlow<Boolean> = combine(
        _deadLetterLimit, _totalFilteredDeadLetterCount
    ) { limit, total -> limit < total }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _pendingCount = MutableStateFlow(0L)
    val pendingCount: StateFlow<Long> = _pendingCount.asStateFlow()

    private val _deadLetterCount = MutableStateFlow(0L)
    val deadLetterCount: StateFlow<Long> = _deadLetterCount.asStateFlow()

    private val _lastCapturedAt = MutableStateFlow<Long?>(null)
    val lastCapturedAt: StateFlow<Long?> = _lastCapturedAt.asStateFlow()

    private val _batchRunning = MutableStateFlow(false)
    val batchRunning: StateFlow<Boolean> = _batchRunning.asStateFlow()

    private val _batchProgress = MutableStateFlow("")
    val batchProgress: StateFlow<String> = _batchProgress.asStateFlow()

    private val _batchProgressFraction = MutableStateFlow(0f)
    val batchProgressFraction: StateFlow<Float> = _batchProgressFraction.asStateFlow()

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    // Health metrics
    private val _healthData = MutableStateFlow<List<com.sciuro.core.parsing.metrics.ParserHealthRow>>(emptyList())
    val healthData: StateFlow<List<com.sciuro.core.parsing.metrics.ParserHealthRow>> = _healthData.asStateFlow()

    private val _priorHealthData = MutableStateFlow<List<com.sciuro.core.parsing.metrics.ParserHealthRow>>(emptyList())
    val priorHealthData: StateFlow<List<com.sciuro.core.parsing.metrics.ParserHealthRow>> = _priorHealthData.asStateFlow()

    private val _pipelineMetrics = MutableStateFlow<PipelineMetrics?>(null)
    val pipelineMetrics: StateFlow<PipelineMetrics?> = _pipelineMetrics.asStateFlow()

    // Pipeline Trace
    private val _pipelineEvents = MutableStateFlow<List<TraceEventSummary>>(emptyList())
    val pipelineEvents: StateFlow<List<TraceEventSummary>> = _pipelineEvents.asStateFlow()

    private val _pipelineTraceLimit = MutableStateFlow(100L)

    private val _traceFilterStatus = MutableStateFlow("ALL")
    val traceFilterStatus: StateFlow<String> = _traceFilterStatus.asStateFlow()

    private val _traceFilterPackage = MutableStateFlow("")
    val traceFilterPackage: StateFlow<String> = _traceFilterPackage.asStateFlow()

    private val _showOnlyAllowlisted = MutableStateFlow(true)
    val showOnlyAllowlisted: StateFlow<Boolean> = _showOnlyAllowlisted.asStateFlow()

    private val _eventBusMetrics = MutableStateFlow<DomainEventMetrics?>(null)
    val eventBusMetrics: StateFlow<DomainEventMetrics?> = _eventBusMetrics.asStateFlow()

    private val _auditIntegrityGaps = MutableStateFlow<Long>(0L)
    val auditIntegrityGaps: StateFlow<Long> = _auditIntegrityGaps.asStateFlow()

    private val _recoveryMetrics = MutableStateFlow<DatabaseRecoveryMetrics?>(null)
    val recoveryMetrics: StateFlow<DatabaseRecoveryMetrics?> = _recoveryMetrics.asStateFlow()

    init {
        refreshCounts()
        loadHealthData()
        loadPipelineEvents()
        loadEventBusMetrics()
        loadRecoveryMetrics()
    }

    fun setTraceFilter(status: String, pkg: String) {
        _traceFilterStatus.value = status
        _traceFilterPackage.value = pkg
        _pipelineTraceLimit.value = 100L
        loadPipelineEvents()
    }

    fun setShowOnlyAllowlisted(show: Boolean) {
        _showOnlyAllowlisted.value = show
        _pipelineTraceLimit.value = 100L
        loadPipelineEvents()
    }

    fun loadMorePipelineEvents() {
        _pipelineTraceLimit.value += 100L
        loadPipelineEvents()
    }

    private fun loadPipelineEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = if (_traceFilterStatus.value == "ALL") null else _traceFilterStatus.value
                val pkg = _traceFilterPackage.value.ifBlank { null }
                val allowed = if (_showOnlyAllowlisted.value) ingestionAllowlist.effectivePackages.value else null
                val events = database.pipelineTraceQueries
                    .selectFilteredTraceEvents(pkg, status, _pipelineTraceLimit.value)
                    .executeAsList()
                    .map {
                        TraceEventSummary(it.raw_event_id, it.first_at, it.last_at, it.stage_count, it.package_name)
                    }
                _pipelineEvents.value = if (allowed != null) events.filter { it.packageName in allowed } else events
            } catch (e: Exception) {
                _uiError.value = "Failed to load trace events: ${e.message}"
            }
        }
    }

    suspend fun getTraceDetails(eventId: String): List<TraceRow> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            database.pipelineTraceQueries.selectTraceByEvent(eventId).executeAsList().map {
                TraceRow(it.stage, it.outcome, it.duration_ms, it.confidence, it.detail_json, it.created_at)
            }
        }
    }

    private fun loadHealthData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
                val now = System.currentTimeMillis()
                val sinceMs = now - sevenDaysMs
                val priorStart = sinceMs - sevenDaysMs
                
                _healthData.value = parserHealthRepository.getMatchRatesSince(sinceMs)
                _priorHealthData.value = parserHealthRepository.getMatchRatesInWindow(priorStart, sinceMs)
                
                val outcomeCounts = database.pipelineTraceQueries.countTraceByOutcomeSince(sinceMs).executeAsList()
                val llmTotal = outcomeCounts.filter { it.stage == "PARSE_LLM" }.sumOf { it.cnt }
                val deadLetters = outcomeCounts.filter { it.stage == "STAGING" && it.outcome == "FAILURE" }.sumOf { it.cnt }
                
                _pipelineMetrics.value = PipelineMetrics(llmTotal, deadLetters)

                _auditIntegrityGaps.value = auditRepository.getAuditIntegrityGaps()
            } catch (e: Exception) {
                _uiError.value = "Failed to load health data: ${e.message}"
            }
        }
    }

    fun clearUiError() {
        _uiError.value = null
    }

    private fun loadEventBusMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bus = eventBus ?: return@launch
                _eventBusMetrics.value = bus.metrics()
            } catch (e: Exception) {
                _eventBusMetrics.value = null
            }
        }
    }

    fun refreshEventBusMetrics() {
        loadEventBusMetrics()
    }

    private fun loadRecoveryMetrics() {
        _recoveryMetrics.value = DatabaseRecoveryMetrics(
            quarantineCount = recoveryManager.quarantineCount(),
            lastQuarantineTimestamp = recoveryManager.lastQuarantineTimestamp(),
            quarantinedFileCount = recoveryManager.listQuarantinedFiles().size,
            lastIntegrityCheckMs = recoveryManager.getLastIntegrityCheckMs(),
            lastIntegrityResult = recoveryManager.getLastIntegrityResult()
        )
    }

    fun simulateNotification(title: String, text: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawEvent = RawEvent(
                    id = UUID.randomUUID().toString(),
                    sourceType = SourceType.NOTIFICATION,
                    sourcePackageOrAddress = packageName,
                    title = title,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                rawEventRepository.persistRawEvent(
                    id = rawEvent.id,
                    sourceType = rawEvent.sourceType.name,
                    sourcePackageOrAddress = rawEvent.sourcePackageOrAddress,
                    title = rawEvent.title,
                    text = rawEvent.text,
                    timestamp = rawEvent.timestamp,
                    capturedAt = System.currentTimeMillis()
                )
                tracer.trace(
                    rawEventId = rawEvent.id,
                    transactionId = null,
                    stage = TraceStage.CAPTURE,
                    outcome = TraceOutcome.SUCCESS,
                    detail = mapOf("source_type" to "SIMULATOR", "package" to packageName),
                    packageName = packageName
                )
                _simulationResult.value = null
                val result = simulationEngine.simulate(rawEvent)
                _simulationResult.value = result
                notificationSourceAdapter.emitNotification(rawEvent)
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Simulation failed"
            }
        }
    }

    fun clearSimulationResult() {
        _simulationResult.value = null
    }

    fun refreshCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _pendingCount.value = rawEventRepository.countPending()
                _deadLetterCount.value = rawEventRepository.countDeadLetter()
                _lastCapturedAt.value = rawEventRepository.getLastCapturedAt()
                _totalFilteredDeadLetterCount.value = rawEventRepository.countDeadLetterFiltered(_showReadDeadLetters.value)
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to refresh counts"
            }
        }
    }

    fun loadMoreDeadLetters() {
        _deadLetterLimit.value += 50L
    }

    fun showReadDeadLetters(): StateFlow<Boolean> = _showReadDeadLetters.asStateFlow()

    fun toggleShowRead() {
        _showReadDeadLetters.value = !_showReadDeadLetters.value
        _deadLetterLimit.value = 50L
        refreshCounts()
    }

    fun markRead(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rawEventRepository.markRead(eventId)
                refreshCounts()
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to mark as read"
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rawEventRepository.markAllDeadLetterRead()
                _deadLetterLimit.value = 50L
                refreshCounts()
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to mark all as read"
            }
        }
    }

    fun clearInbox() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = transactionRepository.observeUnreviewedTransactions().first()
                transactions.forEach {
                    transactionRepository.deleteTransaction(it.id)
                }
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to clear inbox"
            }
        }
    }

    fun resendDeadLetter(rawEventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rawEventRepository.requeueRawEvent(rawEventId)
                refreshCounts()
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to resend dead letter"
            }
        }
    }

    fun updateAndResendDeadLetter(rawEventId: String, title: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.rawEventStagingQueries.updateRawEventPayload(title = title, text = text, id = rawEventId)
                rawEventRepository.requeueRawEvent(rawEventId)
                refreshCounts()
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Failed to update and resend dead letter"
            }
        }
    }

    fun runAllFixtures(delayMs: Long = 500L, forceLlm: Boolean = false) {
        if (_batchRunning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _batchRunning.value = true
                _batchProgress.value = "Starting..."
                _batchProgressFraction.value = 0f
                val fixtures = FixtureLibrary.fixtures
                fixtures.forEachIndexed { index, fixture ->
                    _batchProgress.value = "${index + 1}/${fixtures.size}: ${fixture.description}"
                    _batchProgressFraction.value = (index + 1).toFloat() / fixtures.size
                    val rawEvent = RawEvent(
                        id = UUID.randomUUID().toString(),
                        sourceType = SourceType.NOTIFICATION,
                        sourcePackageOrAddress = if (forceLlm) "com.llm.test" else fixture.packageName,
                        title = fixture.title,
                        text = fixture.text,
                        timestamp = System.currentTimeMillis()
                    )
                    rawEventRepository.persistRawEvent(
                        id = rawEvent.id,
                        sourceType = rawEvent.sourceType.name,
                        sourcePackageOrAddress = rawEvent.sourcePackageOrAddress,
                        title = rawEvent.title,
                        text = rawEvent.text,
                        timestamp = rawEvent.timestamp,
                        capturedAt = System.currentTimeMillis()
                    )
                    tracer.trace(
                        rawEventId = rawEvent.id,
                        transactionId = null,
                        stage = TraceStage.CAPTURE,
                        outcome = TraceOutcome.SUCCESS,
                        detail = mapOf("source_type" to "SIMULATOR", "package" to rawEvent.sourcePackageOrAddress),
                        packageName = rawEvent.sourcePackageOrAddress
                    )
                    _simulationResult.value = simulationEngine.simulate(rawEvent)
                    notificationSourceAdapter.emitNotification(rawEvent)
                    delay(delayMs)
                }
                _batchProgress.value = "Done \u2014 ${fixtures.size} fixtures sent"
                _batchProgressFraction.value = 1f
            } catch (e: Exception) {
                _uiError.value = e.message ?: "Batch run failed"
            } finally {
                _batchRunning.value = false
                refreshCounts()
            }
        }
    }
}
