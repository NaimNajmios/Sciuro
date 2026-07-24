package com.sciuro.core.ingestion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.notification.NotificationSourceAdapter
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.UUID

class SciuroNotificationService : NotificationListenerService() {

    private val notificationSourceAdapter: NotificationSourceAdapter by inject()
    private val rawEventRepository: RawEventRepository by inject()
    private val allowlist: MutableIngestionAllowlist by inject()
    private val tracer: PipelineTracer by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        val activeNotifications = getActiveNotifications()
        if (activeNotifications != null) {
            serviceScope.launch {
                for (sbn in activeNotifications) {
                    processAndPersistNotification(sbn)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val cn = android.content.ComponentName(this, SciuroNotificationService::class.java)
        NotificationListenerService.requestRebind(cn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        serviceScope.launch {
            processAndPersistNotification(sbn)
        }
    }

    private suspend fun processAndPersistNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (!allowlist.allows(packageName)) {
            tracer.trace(null, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                detail = mapOf("reason" to "allowlist_reject", "package" to packageName))
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""

        if (title.isBlank() && text.isBlank()) {
            tracer.trace(null, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                detail = mapOf("reason" to "blank_content", "package" to packageName))
            return
        }

        if (allowlist.isDefaultAggregatorPackage(packageName)) {
            if (!isFinancialAggregatorNotification(title, text)) {
                tracer.trace(null, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                    detail = mapOf("reason" to "non_financial_aggregator", "package" to packageName))
                return
            }
        }

        val capturedAt = System.currentTimeMillis()
        val rawEvent = RawEvent(
            id = UUID.randomUUID().toString(),
            sourceType = SourceType.NOTIFICATION,
            sourcePackageOrAddress = packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        rawEventRepository.persistRawEvent(
            id = rawEvent.id,
            sourceType = rawEvent.sourceType.name,
            sourcePackageOrAddress = rawEvent.sourcePackageOrAddress,
            title = rawEvent.title,
            text = rawEvent.text,
            timestamp = rawEvent.timestamp,
            capturedAt = capturedAt
        )

        tracer.trace(rawEvent.id, null, TraceStage.CAPTURE, TraceOutcome.SUCCESS,
            detail = mapOf("source_type" to "NOTIFICATION", "package" to packageName))

        notificationSourceAdapter.emitNotification(rawEvent)
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
