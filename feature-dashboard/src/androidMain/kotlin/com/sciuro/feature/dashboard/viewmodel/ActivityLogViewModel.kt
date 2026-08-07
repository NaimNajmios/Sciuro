package com.sciuro.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ActivityLogViewModel(
    private val rawEventRepository: RawEventRepository
) : ViewModel() {

    val entries: StateFlow<List<IngestionActivityEntry>> = rawEventRepository
        .observeRecentActivity(10)
        .map { events ->
            events.map {
                IngestionActivityEntry(
                    rawEventId = it.id,
                    sourceType = it.source_type,
                    sourcePackageOrAddress = it.source_package_or_address,
                    timestamp = it.timestamp,
                    status = it.activity_status.toActivityLogStatus(),
                    reason = it.activity_reason
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
