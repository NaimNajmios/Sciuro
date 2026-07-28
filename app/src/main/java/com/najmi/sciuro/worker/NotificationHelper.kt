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
    private const val DEBT_CHANNEL_ID = "sciuro_debt_channel"
    private const val DEBT_CHANNEL_NAME = "Debt Alerts"
    private const val OBLIGATION_CHANNEL_ID = "sciuro_obligation_channel"
    private const val OBLIGATION_CHANNEL_NAME = "Obligation Alerts"
    private const val REVIEW_NOTIFICATION_ID = 1001
    private const val BUDGET_NOTIFICATION_BASE = 2000
    private const val BILL_NOTIFICATION_BASE = 3000
    private const val DEBT_NOTIFICATION_BASE = 4000
    private const val OBLIGATION_NOTIFICATION_BASE = 5000

    // New channels
    private const val BACKUP_CHANNEL_ID = "sciuro_backup_channel"
    private const val BACKUP_CHANNEL_NAME = "Backup Reminders"
    private const val RUNWAY_CHANNEL_ID = "sciuro_runway_channel"
    private const val RUNWAY_CHANNEL_NAME = "Runway Alerts"
    private const val SPENDING_CHANNEL_ID = "sciuro_spending_channel"
    private const val SPENDING_CHANNEL_NAME = "Spending Alerts"
    private const val DIGEST_CHANNEL_ID = "sciuro_digest_channel"
    private const val DIGEST_CHANNEL_NAME = "Weekly Digest"
    private const val BNPL_CHANNEL_ID = "sciuro_bnpl_channel"
    private const val BNPL_CHANNEL_NAME = "BNPL Risk"
    private const val CASH_CHANNEL_ID = "sciuro_cash_channel"
    private const val CASH_CHANNEL_NAME = "Cash Anomalies"
    private const val TRANSFER_CHANNEL_ID = "sciuro_transfer_channel"
    private const val TRANSFER_CHANNEL_NAME = "Transfer Review"
    private const val MILESTONE_CHANNEL_ID = "sciuro_milestone_channel"
    private const val MILESTONE_CHANNEL_NAME = "Milestones"

    private const val BACKUP_NOTIFICATION_ID = 6000
    private const val RUNWAY_NOTIFICATION_ID = 7000
    private const val SPENDING_NOTIFICATION_BASE = 8000
    private const val DIGEST_NOTIFICATION_ID = 9000
    private const val BNPL_NOTIFICATION_ID = 10000
    private const val CASH_NOTIFICATION_BASE = 11000
    private const val TRANSFER_NOTIFICATION_BASE = 12000
    private const val MILESTONE_NOTIFICATION_ID = 13000
    private const val DEBT_DUE_NOTIFICATION_BASE = 14000
    private const val INCOME_NOTIFICATION_BASE = 15000

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

    fun showDebtAlert(context: Context, debtId: String, debtName: String, message: String) {
        ensureChannel(context, DEBT_CHANNEL_ID, DEBT_CHANNEL_NAME, "Alerts for debt updates")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, DEBT_NOTIFICATION_BASE + debtId.hashCode(),
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, DEBT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(debtName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(DEBT_NOTIFICATION_BASE + debtId.hashCode(), notification)
    }

    fun showObligationAlert(context: Context, obligationId: String, name: String, message: String) {
        ensureChannel(context, OBLIGATION_CHANNEL_ID, OBLIGATION_CHANNEL_NAME, "Alerts for recurring obligations")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, OBLIGATION_NOTIFICATION_BASE + obligationId.hashCode(),
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, OBLIGATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(OBLIGATION_NOTIFICATION_BASE + obligationId.hashCode(), notification)
    }

    fun showBackupReminder(context: Context, daysSince: Int) {
        ensureChannel(context, BACKUP_CHANNEL_ID, BACKUP_CHANNEL_NAME, "Periodic reminders to export an encrypted backup")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "settings")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BACKUP_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Backup Reminder")
            .setContentText("Haven't backed up in $daysSince days. Export now to protect your data.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BACKUP_NOTIFICATION_ID, notification)
    }

    fun showRunwayAlert(context: Context, deficit: Double) {
        ensureChannel(context, RUNWAY_CHANNEL_ID, RUNWAY_CHANNEL_NAME, "Critical alerts when upcoming bills exceed available funds")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, RUNWAY_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val formatted = "RM %.0f".format(deficit)
        val notification = NotificationCompat.Builder(context, RUNWAY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Runway Critical")
            .setContentText("Your upcoming bills exceed available funds by $formatted")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(RUNWAY_NOTIFICATION_ID, notification)
    }

    fun showLargeTransactionAlert(context: Context, merchant: String?, amount: Double) {
        ensureChannel(context, SPENDING_CHANNEL_ID, SPENDING_CHANNEL_NAME, "Alerts for large or unusual transactions")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, SPENDING_NOTIFICATION_BASE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val merchantName = merchant ?: "an unknown merchant"
        val notification = NotificationCompat.Builder(context, SPENDING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Large Transaction")
            .setContentText("RM %.2f spent at $merchantName".format(amount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(SPENDING_NOTIFICATION_BASE + amount.toInt(), notification)
    }

    fun showUnusualSpendingAlert(context: Context, categoryName: String, pctAbove: Double, amount: Double) {
        ensureChannel(context, SPENDING_CHANNEL_ID, SPENDING_CHANNEL_NAME, "Alerts for large or unusual transactions")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, SPENDING_NOTIFICATION_BASE + 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pct = "%.0f".format(pctAbove * 100)
        val notification = NotificationCompat.Builder(context, SPENDING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Unusual Spending")
            .setContentText("$categoryName spending is $pct% above your usual amount this month (RM %.2f)".format(amount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(SPENDING_NOTIFICATION_BASE + 2 + categoryName.hashCode(), notification)
    }

    fun showDebtDueReminder(context: Context, debtName: String, daysUntil: Int, remaining: Double) {
        ensureChannel(context, DEBT_CHANNEL_ID, DEBT_CHANNEL_NAME, "Alerts for debt updates")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, DEBT_DUE_NOTIFICATION_BASE + debtName.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val body = if (daysUntil <= 0) "$debtName is due today — RM %.2f remaining".format(remaining)
        else "$debtName is due in $daysUntil days — RM %.2f remaining".format(remaining)
        val notification = NotificationCompat.Builder(context, DEBT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Debt Due Soon")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(DEBT_DUE_NOTIFICATION_BASE + debtName.hashCode(), notification)
    }

    fun showIncomeNotArrivedAlert(context: Context, amount: Double, daysOverdue: Int) {
        ensureChannel(context, BILL_CHANNEL_ID, BILL_CHANNEL_NAME, "Reminders for upcoming bills and income")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, INCOME_NOTIFICATION_BASE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, BILL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Income Not Received")
            .setContentText("Expected income of RM %.0f hasn't arrived yet (overdue by $daysOverdue days)".format(amount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(INCOME_NOTIFICATION_BASE, notification)
    }

    fun showWeeklyDigest(context: Context, totalSpent: Double, topCategory: String?, unreviewed: Int) {
        ensureChannel(context, DIGEST_CHANNEL_ID, DIGEST_CHANNEL_NAME, "Weekly spending summary")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, DIGEST_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val top = topCategory ?: "n/a"
        val notification = NotificationCompat.Builder(context, DIGEST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Weekly Summary")
            .setContentText("Spent RM %.0f this week. Top: $top. $unreviewed unreviewed.".format(totalSpent))
            .setStyle(NotificationCompat.InboxStyle().addLine("Total spent: RM %.0f".format(totalSpent))
                .addLine("Top category: $top")
                .addLine("Unreviewed: $unreviewed"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(DIGEST_NOTIFICATION_ID, notification)
    }

    fun showBillAutopayConfirmed(context: Context, obligationName: String, amount: Double, nextDue: Long) {
        ensureChannel(context, BILL_CHANNEL_ID, BILL_CHANNEL_NAME, "Reminders for upcoming bills and income")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        val nextDateStr = dateFormat.format(java.util.Date(nextDue))
        val pendingIntent = PendingIntent.getActivity(
            context, BILL_NOTIFICATION_BASE + obligationName.hashCode() + 1000, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, BILL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bill Paid")
            .setContentText("$obligationName RM %.2f paid. Next due: $nextDateStr".format(amount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BILL_NOTIFICATION_BASE + obligationName.hashCode() + 1000, notification)
    }

    fun showNetPositionMilestone(context: Context, milestone: Double, netWorth: Double) {
        ensureChannel(context, MILESTONE_CHANNEL_ID, MILESTONE_CHANNEL_NAME, "Congratulatory notifications when net worth crosses thresholds")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, MILESTONE_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val milestoneStr = "RM %,.0f".format(milestone)
        val netWorthStr = "RM %,.0f".format(netWorth)
        val notification = NotificationCompat.Builder(context, MILESTONE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Net Worth Milestone Reached!")
            .setContentText("Your net worth crossed $milestoneStr (currently $netWorthStr)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(MILESTONE_NOTIFICATION_ID, notification)
    }

    fun showBnplAlert(context: Context, count: Int) {
        ensureChannel(context, BNPL_CHANNEL_ID, BNPL_CHANNEL_NAME, "Alerts for Buy-Now-Pay-Later risk")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BNPL_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, BNPL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BNPL Risk Alert")
            .setContentText("$count active BNPL debts — review your commitments")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(BNPL_NOTIFICATION_ID, notification)
    }

    fun showCashAnomalyAlert(context: Context, variance: Double, reason: String?) {
        ensureChannel(context, CASH_CHANNEL_ID, CASH_CHANNEL_NAME, "Alerts for cash recount anomalies")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "wallet")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, CASH_NOTIFICATION_BASE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val reasonText = if (reason != null) " — $reason" else ""
        val notification = NotificationCompat.Builder(context, CASH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Cash Anomaly")
            .setContentText("Cash recount shows RM %.2f variance$reasonText".format(variance))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(CASH_NOTIFICATION_BASE + variance.toInt(), notification)
    }

    fun showTransferReviewAlert(context: Context, candidateRecipient: String) {
        ensureChannel(context, TRANSFER_CHANNEL_ID, TRANSFER_CHANNEL_NAME, "Alerts for unmatched transfer candidates")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "kanban")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, TRANSFER_NOTIFICATION_BASE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, TRANSFER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Transfer to Review")
            .setContentText("Possible self-transfer matched to $candidateRecipient")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(TRANSFER_NOTIFICATION_BASE + candidateRecipient.hashCode(), notification)
    }
}
