package com.sciuro.core.ingestion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sciuro.core.ingestion.config.IngestionConfig
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.UUID

class SciuroNotificationService : NotificationListenerService() {

    // Injecting the singleton adapter so it feeds directly into the app's pipeline
    private val notificationSourceAdapter: NotificationSourceAdapter by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        
        // Discard anything that's not from our allowlisted financial apps
        if (packageName !in IngestionConfig.allowedPackages) return

        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""

        if (title.isBlank() && text.isBlank()) return

        // Heuristic Pre-Filtering for Aggregators (Email)
        if (packageName in IngestionConfig.aggregatorPackages) {
            if (!isFinancialAggregatorNotification(title, text)) {
                return // Drop it before it hits the pipeline
            }
        }

        val rawEvent = RawEvent(
            id = UUID.randomUUID().toString(),
            sourceType = SourceType.NOTIFICATION,
            sourcePackageOrAddress = packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        serviceScope.launch {
            notificationSourceAdapter.emitNotification(rawEvent)
        }
    }

    /**
     * Very lightweight heuristic to drop obvious spam or non-financial emails.
     * We look for common keywords found in banking email subjects/titles.
     */
    private fun isFinancialAggregatorNotification(title: String, text: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()

        val combinedText = "$lowerTitle $lowerText"

        val hasBankingKeywords = combinedText.contains("m2u") || 
                                 combinedText.contains("cimb notification") ||
                                 combinedText.contains("transaction") ||
                                 combinedText.contains("funds received") ||
                                 combinedText.contains("transfer") ||
                                 combinedText.contains("receipt")
                              
        val hasCurrencySymbol = combinedText.contains("rm")

        return hasBankingKeywords || hasCurrencySymbol
    }
}
