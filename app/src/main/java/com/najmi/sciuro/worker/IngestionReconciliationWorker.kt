package com.najmi.sciuro.worker

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.core.ledger.repository.RawEventRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IngestionReconciliationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val rawEventRepository: RawEventRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val staleThresholdMs = System.currentTimeMillis() - STALE_PROCESSING_MS
            val strandedCount = rawEventRepository.countStrandedEvents(staleThresholdMs)

            if (strandedCount > 0) {
                val strandedEvents = rawEventRepository.getStrandedEvents(staleThresholdMs)
                for (event in strandedEvents) {
                    if (event.status == "PROCESSING") {
                        rawEventRepository.requeueRawEvent(event.id)
                    }
                }
            }

            val tracePurgeBefore = System.currentTimeMillis() - TRACE_RETENTION_MS
            rawEventRepository.purgeOldTraces(tracePurgeBefore)

            val notificationListeners = Settings.Secure.getString(
                applicationContext.contentResolver,
                "enabled_notification_listeners"
            )
            val isListenerEnabled = notificationListeners?.contains(applicationContext.packageName) == true
            if (!isListenerEnabled) {
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed, will retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "IngestionReconciliationWorker"
        const val STALE_PROCESSING_MS = 60_000L
        const val TRACE_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
