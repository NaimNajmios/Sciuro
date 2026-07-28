package com.sciuro.core.ingestion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.engine.AggregatorHeuristicFilter
import com.sciuro.core.ingestion.engine.NotificationTextResolver
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
        val sessionId = UUID.randomUUID().toString()

        if (!allowlist.allows(packageName)) {
            tracer.trace(sessionId, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                detail = mapOf("reason" to "allowlist_reject", "package" to packageName), packageName = packageName)
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = resolveText(notification, packageName)

        if (title.isBlank() && text.isBlank()) {
            val presentKeys = notification.extras.keySet().joinToString(",")
            tracer.trace(sessionId, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                detail = mapOf("reason" to "blank_content", "package" to packageName, "extras_present" to presentKeys),
                packageName = packageName)
            return
        }

        if (allowlist.isDefaultAggregatorPackage(packageName)) {
            if (!AggregatorHeuristicFilter.isFinancial(title, text)) {
                tracer.trace(sessionId, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                    detail = mapOf("reason" to "non_financial_aggregator", "package" to packageName), packageName = packageName)
                return
            }
        }

        val capturedAt = System.currentTimeMillis()
        val rawEvent = RawEvent(
            id = sessionId,
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
            detail = mapOf("source_type" to "NOTIFICATION", "package" to packageName), packageName = packageName)

        notificationSourceAdapter.emitNotification(rawEvent)
    }

    companion object {
        fun resolveText(notification: Notification, packageName: String): String {
            val shortText = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
            val bigText = notification.extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""
            val textLines = notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString("\n") { it.toString() } ?: ""
            val standard = NotificationTextResolver.resolveTextFallback(shortText, bigText, textLines)
            if (standard.isNotBlank()) return standard
            return resolveFromExtras(notification.extras, packageName)
        }

        private fun resolveFromExtras(extras: android.os.Bundle, packageName: String): String {
            val extrasMap = mutableMapOf<String, String>()
            for (key in extras.keySet()) {
                extras.getString(key)?.let { extrasMap[key] = it }
            }
            return NotificationTextResolver.resolveCustomExtrasFallback(packageName, extrasMap)
        }
    }
}
