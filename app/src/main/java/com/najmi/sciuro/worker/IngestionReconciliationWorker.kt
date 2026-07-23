package com.najmi.sciuro.worker

import android.content.Context
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.core.ledger.repository.RawEventRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class IngestionReconciliationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val rawEventRepository: RawEventRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            rawEventRepository.countPending()

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
            Result.retry()
        }
    }

    companion object {
        val REPEAT_INTERVAL_HOURS = 6L

        fun repeatIntervalMillis(): Long = TimeUnit.HOURS.toMillis(REPEAT_INTERVAL_HOURS)
    }
}
