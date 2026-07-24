package com.najmi.sciuro.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReviewReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val transactionRepository: TransactionRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val unreviewed = transactionRepository.observeUnreviewedTransactions().first()
            if (unreviewed.isNotEmpty()) {
                NotificationHelper.showReviewNotification(applicationContext, unreviewed.size)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show review reminder", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ReviewReminderWorker"
    }
}
