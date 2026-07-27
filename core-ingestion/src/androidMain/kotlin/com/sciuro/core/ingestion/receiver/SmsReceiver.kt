package com.sciuro.core.ingestion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.engine.AggregatorHeuristicFilter
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.sms.SmsSourceAdapter
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val smsSourceAdapter: SmsSourceAdapter by inject()
    private val rawEventRepository: RawEventRepository by inject()
    private val allowlist: MutableIngestionAllowlist by inject()
    private val tracer: PipelineTracer by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                coroutineScope {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (message in messages) {
                        launch { processMessage(message) }
                    }
                }
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }

    private suspend fun processMessage(message: android.telephony.SmsMessage) {
        val sender = message.originatingAddress ?: return
        val body = message.messageBody ?: return
        val sessionId = UUID.randomUUID().toString()

        if (body.isBlank()) return

        val isKnownBankSms = allowlist.isDefaultBankSmsSender(sender)

        if (!isKnownBankSms) {
            if (!allowlist.allows(sender)) {
                tracer.trace(sessionId, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                    detail = mapOf("reason" to "allowlist_reject", "sender" to sender), packageName = sender)
                return
            }
            val hasFinancialSignal = AggregatorHeuristicFilter.isFinancial(sender, body)
            if (!hasFinancialSignal) {
                tracer.trace(sessionId, null, TraceStage.CAPTURE, TraceOutcome.DROP,
                    detail = mapOf("reason" to "non_financial_sms", "sender" to sender), packageName = sender)
                return
            }
        }

        val rawEvent = RawEvent(
            id = sessionId,
            sourceType = SourceType.SMS,
            sourcePackageOrAddress = sender,
            title = sender,
            text = body,
            timestamp = System.currentTimeMillis()
        )

        rawEventRepository.persistRawEvent(
            id = rawEvent.id,
            sourceType = rawEvent.sourceType.name,
            sourcePackageOrAddress = rawEvent.sourcePackageOrAddress,
            title = rawEvent.title,
            text = rawEvent.text,
            timestamp = rawEvent.timestamp,
            capturedAt = System.currentTimeMillis()
        )

        tracer.trace(rawEvent.id, null, TraceStage.CAPTURE, TraceOutcome.SUCCESS,
            detail = mapOf("source_type" to "SMS", "sender" to sender), packageName = sender)

        smsSourceAdapter.emitSms(rawEvent)
    }
}
