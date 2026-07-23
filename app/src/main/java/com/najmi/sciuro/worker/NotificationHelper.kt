package com.najmi.sciuro.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.najmi.sciuro.MainActivity
import com.najmi.sciuro.R

object NotificationHelper {
    private const val REVIEW_CHANNEL_ID = "sciuro_review_channel"
    private const val REVIEW_CHANNEL_NAME = "Transaction Reviews"
    private const val BUDGET_CHANNEL_ID = "sciuro_budget_channel"
    private const val BUDGET_CHANNEL_NAME = "Budget Alerts"
    private const val BILL_CHANNEL_ID = "sciuro_bill_channel"
    private const val BILL_CHANNEL_NAME = "Bill Reminders"
    private const val REVIEW_NOTIFICATION_ID = 1001
    private const val BUDGET_NOTIFICATION_BASE = 2000
    private const val BILL_NOTIFICATION_BASE = 3000

    private fun ensureChannel(context: Context, channelId: String, channelName: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    this.description = description
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun showReviewNotification(context: Context, unreviewedCount: Int) {
        ensureChannel(context, REVIEW_CHANNEL_ID, REVIEW_CHANNEL_NAME, "Reminders to review unassigned transactions")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, REVIEW_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Transactions to Review")
            .setContentText("You have $unreviewedCount transaction(s) waiting for your review.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(REVIEW_NOTIFICATION_ID, notification)
    }

    fun showBudgetAlert(context: Context, categoryId: String, percentUsed: Double) {
        ensureChannel(context, BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, "Alerts when you approach budget limits")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "budgets")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BUDGET_NOTIFICATION_BASE + categoryId.hashCode(),
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val percent = "%.0f".format(percentUsed * 100)
        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Budget Alert")
            .setContentText("You've used $percent% of your budget in this category.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BUDGET_NOTIFICATION_BASE + categoryId.hashCode(), notification)
    }

    fun showBillReminder(context: Context, obligationId: String, obligationName: String, dueDate: Long) {
        ensureChannel(context, BILL_CHANNEL_ID, BILL_CHANNEL_NAME, "Reminders for upcoming bills")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BILL_NOTIFICATION_BASE + obligationId.hashCode(),
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val daysUntil = (dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
        val body = if (daysUntil <= 0) "$obligationName is due today!"
        else if (daysUntil == 1L) "$obligationName is due tomorrow"
        else "$obligationName is due in $daysUntil days"

        val notification = NotificationCompat.Builder(context, BILL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bill Reminder")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BILL_NOTIFICATION_BASE + obligationId.hashCode(), notification)
    }
}
