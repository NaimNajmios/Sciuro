package com.najmi.sciuro.worker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IngestionReconciliationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val rawEventRepository: RawEventRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            retryPendingEvents()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun retryPendingEvents() {
        val pendingEvents = rawEventRepository.observePendingEvents().first()
        if (pendingEvents.isNotEmpty()) {
            println("SCIURO_RECONCILIATION: ${pendingEvents.size} pending events found for retry")
        }
    }
}
