package com.najmi.sciuro.worker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ingestion.config.IngestionDefaults
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FinanceAppCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val eventBus: DomainEventBus by inject()

    override suspend fun doWork(): Result {
        val packageName = inputData.getString("package_name") ?: return Result.success()

        val knownApps = IngestionDefaults.knownFinanceAppSignatures
        if (packageName !in knownApps) return Result.success()

        val pm = applicationContext.packageManager
        val isInstalled = try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Package $packageName not installed", e)
            false
        }

        if (!isInstalled) return Result.success()

        eventBus.publish(DomainEvent.NewFinanceAppDetected(packageName))
        return Result.success()
    }

    companion object {
        private const val TAG = "FinanceAppCheckWorker"
    }
}
