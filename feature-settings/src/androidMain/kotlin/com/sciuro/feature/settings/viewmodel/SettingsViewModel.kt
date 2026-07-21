package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.db.Raw_event_staging
import com.sciuro.core.ledger.repository.RawEventRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.parsing.engine.SimulationEngine
import com.sciuro.core.parsing.engine.SimulationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(
    private val notificationSourceAdapter: NotificationSourceAdapter,
    private val transactionRepository: TransactionRepository,
    private val simulationEngine: SimulationEngine,
    private val rawEventRepository: RawEventRepository
) : ViewModel() {

    private val _simulationResult = MutableStateFlow<SimulationResult?>(null)
    val simulationResult: StateFlow<SimulationResult?> = _simulationResult.asStateFlow()

    val deadLetterEvents: StateFlow<List<Raw_event_staging>> =
        rawEventRepository.observeDeadLetterEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingCount = MutableStateFlow(0L)
    val pendingCount: StateFlow<Long> = _pendingCount.asStateFlow()

    private val _deadLetterCount = MutableStateFlow(0L)
    val deadLetterCount: StateFlow<Long> = _deadLetterCount.asStateFlow()

    init {
        refreshCounts()
    }

    fun simulateNotification(title: String, text: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawEvent = RawEvent(
                id = UUID.randomUUID().toString(),
                sourceType = SourceType.NOTIFICATION,
                sourcePackageOrAddress = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            _simulationResult.value = null
            val result = simulationEngine.simulate(rawEvent)
            _simulationResult.value = result
            notificationSourceAdapter.emitNotification(rawEvent)
        }
    }

    fun clearSimulationResult() {
        _simulationResult.value = null
    }

    fun refreshCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            _pendingCount.value = rawEventRepository.countPending()
            _deadLetterCount.value = rawEventRepository.countDeadLetter()
        }
    }

    fun clearInbox() {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = transactionRepository.observeUnreviewedTransactions().first()
            transactions.forEach {
                transactionRepository.deleteTransaction(it.id)
            }
        }
    }
}
