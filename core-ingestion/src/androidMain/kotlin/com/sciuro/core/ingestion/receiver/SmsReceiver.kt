package com.sciuro.core.ingestion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sciuro.core.ingestion.config.MutableIngestionAllowlist
import com.sciuro.core.ingestion.model.RawEvent
import com.sciuro.core.ingestion.model.SourceType
import com.sciuro.core.ingestion.source.sms.SmsSourceAdapter
import com.sciuro.core.ledger.repository.RawEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val smsSourceAdapter: SmsSourceAdapter by inject()
    private val rawEventRepository: RawEventRepository by inject()
    private val allowlist: MutableIngestionAllowlist by inject()
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue

            if (body.isBlank()) continue
            if (!allowlist.allows(sender)) continue

            val hasFinancialSignal = body.lowercase().let { lower ->
                lower.contains("rm") ||
                lower.contains("transaction") ||
                lower.contains("transfer") ||
                lower.contains("credited") ||
                lower.contains("debited") ||
                lower.contains("payment") ||
                lower.contains("receipt")
            }
            if (!hasFinancialSignal) continue

            receiverScope.launch {
                val rawEvent = RawEvent(
                    id = UUID.randomUUID().toString(),
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

                smsSourceAdapter.emitSms(rawEvent)
            }
        }
    }
}
