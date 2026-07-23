package com.najmi.sciuro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class FinanceAppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName.isBlank()) return

        val isInstall = intent.action == Intent.ACTION_PACKAGE_ADDED
        val isUpdate = intent.action == Intent.ACTION_PACKAGE_REPLACED
        if (!isInstall && !isUpdate) return

        val inputData = Data.Builder()
            .putString("package_name", packageName)
            .putBoolean("is_install", isInstall)
            .build()

        val checkWork = OneTimeWorkRequestBuilder<com.najmi.sciuro.worker.FinanceAppCheckWorker>()
            .setInputData(inputData)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(checkWork)
    }
}
