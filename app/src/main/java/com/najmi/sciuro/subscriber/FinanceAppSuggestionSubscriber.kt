package com.najmi.sciuro.subscriber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.najmi.sciuro.MainActivity
import com.najmi.sciuro.R
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ingestion.config.IngestionDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinanceAppSuggestionSubscriber(
    private val eventBus: DomainEventBus,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                if (event is DomainEvent.NewFinanceAppDetected) {
                    val displayName = IngestionDefaults.knownFinanceAppSignatures[event.packageName]
                        ?: event.packageName
                    showSuggestionNotification(displayName, event.packageName)
                }
            }
        }
    }

    private fun showSuggestionNotification(displayName: String, packageName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "sciuro_app_suggestions"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Finance App Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Suggestions to add newly detected finance apps"
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "settings")
            putExtra("developer_tab", "sources")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New finance app detected")
            .setContentText("Noticed $displayName is installed — tap to add it to Sciuro")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(packageName.hashCode(), notification)
    }
}
