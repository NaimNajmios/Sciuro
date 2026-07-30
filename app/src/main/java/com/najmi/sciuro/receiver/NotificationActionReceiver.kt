package com.najmi.sciuro.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_APPROVE_TRANSFER -> {
                onTransferApproved(context)
            }
            ACTION_REJECT_TRANSFER -> {
                onTransferRejected(context)
            }
            ACTION_MARK_PAID -> {
                onBillMarkedPaid(context, intent)
            }
            ACTION_SNOOZE_BILL -> {
                onBillSnoozed(context, intent)
            }
        }
    }

    private fun onTransferApproved(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(TRANSFER_NOTIFICATION_ID)
    }

    private fun onTransferRejected(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(TRANSFER_NOTIFICATION_ID)
    }

    private fun onBillMarkedPaid(context: Context, intent: Intent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
    }

    private fun onBillSnoozed(context: Context, intent: Intent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
    }

    companion object {
        const val ACTION_APPROVE_TRANSFER = "com.sciuro.action.APPROVE_TRANSFER"
        const val ACTION_REJECT_TRANSFER = "com.sciuro.action.REJECT_TRANSFER"
        const val ACTION_MARK_PAID = "com.sciuro.action.MARK_PAID"
        const val ACTION_SNOOZE_BILL = "com.sciuro.action.SNOOZE_BILL"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private const val TRANSFER_NOTIFICATION_ID = 12000
    }
}
