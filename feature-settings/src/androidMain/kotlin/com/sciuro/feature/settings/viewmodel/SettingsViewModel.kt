package com.sciuro.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(
    private val notificationSourceAdapter: NotificationSourceAdapter,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

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
            notificationSourceAdapter.emitNotification(rawEvent)
        }
    }

    fun clearInbox() {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete all unreviewed transactions
            val transactions = transactionRepository.observeUnreviewedTransactions().first()
            transactions.forEach {
                transactionRepository.deleteTransaction(it.id)
            }
        }
    }
}
