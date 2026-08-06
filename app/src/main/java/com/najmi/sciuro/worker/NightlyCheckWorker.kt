package com.najmi.sciuro.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sciuro.feature.settings.config.NotificationPreferencesStore
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.trace.PipelineTracer
import com.sciuro.core.audit.trace.TraceOutcome
import com.sciuro.core.audit.trace.TraceStage
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.repository.AccountRepository
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import com.sciuro.core.ledger.security.DatabaseIntegrityChecker
import com.sciuro.core.ledger.security.DatabaseRecoveryManager
import com.sciuro.core.ledger.security.IntegrityCheckPolicy
import com.sciuro.core.obligations.engine.IncomeRecurrencePatternDetector
import com.sciuro.core.obligations.repository.ObligationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class NightlyCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val prefsStore: NotificationPreferencesStore by inject()
    private val settingsProvider: SettingsProvider by inject()
    private val accountRepository: AccountRepository by inject()
    private val obligationRepository: ObligationRepository by inject()
    private val debtRepository: DebtRepository by inject()
    private val incomeDetector: IncomeRecurrencePatternDetector by inject()
    private val transactionRepository: TransactionRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val recoveryManager: DatabaseRecoveryManager by inject()
    private val tracer: PipelineTracer by inject()

    override suspend fun doWork(): Result {
        return try {
            runIntegrityCheckIfDue()
            if (prefsStore.isEnabled(NotificationPreferencesStore.BACKUP_REMINDER)) {
                checkBackupOverdue()
            }
            if (prefsStore.isEnabled(NotificationPreferencesStore.RUNWAY_ALERT)) {
                checkRunwayCritical()
            }
            if (prefsStore.isEnabled(NotificationPreferencesStore.DEBT_DUE)) {
                checkDebtsDueSoon()
            }
            if (prefsStore.isEnabled(NotificationPreferencesStore.INCOME_NOT_ARRIVED)) {
                checkIncomeNotArrived()
            }
            if (prefsStore.isEnabled(NotificationPreferencesStore.WEEKLY_DIGEST)) {
                checkWeeklyDigest()
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Nightly check failed", e)
            Result.retry()
        }
    }

    private suspend fun runIntegrityCheckIfDue() {
        val now = currentTimeMillis()
        if (!IntegrityCheckPolicy.isDue(recoveryManager.getLastIntegrityCheckMs(), now)) return

        val startMs = System.currentTimeMillis()
        val result = withContext(Dispatchers.IO) {
            DatabaseIntegrityChecker.check(applicationContext)
        }
        val durationMs = System.currentTimeMillis() - startMs

        tracer.trace(
            rawEventId = null,
            transactionId = null,
            stage = TraceStage.DATABASE_INTEGRITY,
            outcome = if (result is DatabaseIntegrityChecker.IntegrityResult.Success) {
                TraceOutcome.SUCCESS
            } else {
                TraceOutcome.FAILURE
            },
            durationMs = durationMs,
            detail = mapOf("result" to result.message),
            packageName = "database"
        )

        recoveryManager.setLastIntegrityCheckMs(now)
        recoveryManager.setLastIntegrityResult(result.message)
    }

    private suspend fun checkBackupOverdue() {
        val lastBackup = settingsProvider.getLastBackupTimestamp()
        if (lastBackup == 0L) return
        val interval = prefsStore.getInt(
            NotificationPreferencesStore.BACKUP_REMINDER, "interval", 7
        )
        val daysSince = (currentTimeMillis() - lastBackup) / (24L * 60 * 60 * 1000)
        if (daysSince >= interval) {
            NotificationHelper.showBackupReminder(applicationContext, daysSince.toInt())
        }
    }

    private suspend fun checkRunwayCritical() {
        val accounts = accountRepository.observeAccounts().first()
        val totalAccounts = accounts.sumOf { it.balance }
        val obligations = obligationRepository.observeActiveObligations().first()
        val incomePattern = incomeDetector.detectNextIncome()
        val nextIncome = incomePattern?.nextExpectedDate
            ?: (currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        val expectedIncome = incomePattern?.amount ?: 0.0
        val obligationsDue = obligations.filter {
            it.nextDueDate <= nextIncome
        }.sumOf { it.amount }
        val debts = debtRepository.observeDebts().first()
        val debtsDue = debts.filter { debt ->
            debt.direction == DebtDirection.I_OWE && debt.dueDate != null &&
                debt.dueDate!! <= nextIncome
        }.sumOf { it.remainingBalance.toDouble() }
        val runway = totalAccounts + expectedIncome - obligationsDue - debtsDue
        if (runway < 0) {
            NotificationHelper.showRunwayAlert(applicationContext, kotlin.math.abs(runway))
        }
    }

    private suspend fun checkDebtsDueSoon() {
        val daysBefore = prefsStore.getInt(
            NotificationPreferencesStore.DEBT_DUE, "days_before", 7
        )
        val now = currentTimeMillis()
        val cutoff = now + daysBefore * 24L * 60 * 60 * 1000
        val debts = debtRepository.observeDebts().first()
        for (debt in debts) {
            val dueDate = debt.dueDate ?: continue
            if (dueDate in (now + 1)..cutoff) {
                val daysUntil = ((dueDate - now) / (24L * 60 * 60 * 1000)).toInt()
                NotificationHelper.showDebtDueReminder(
                    applicationContext, debt.name, daysUntil, debt.remainingBalance.toDouble()
                )
            }
        }
    }

    private suspend fun checkIncomeNotArrived() {
        val incomePattern = incomeDetector.detectNextIncome() ?: return
        val now = currentTimeMillis()
        val expectedDate = incomePattern.nextExpectedDate
        val gracePeriod = 24L * 60 * 60 * 1000
        if (now > expectedDate + gracePeriod) {
            val daysOverdue = ((now - expectedDate) / (24L * 60 * 60 * 1000)).toInt()
            NotificationHelper.showIncomeNotArrivedAlert(
                applicationContext, incomePattern.amount, daysOverdue
            )
        }
    }

    private suspend fun checkWeeklyDigest() {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return

        val oneWeekAgo = currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val allTxs = transactionRepository.observeAllTransactions().first()
        val weekTxs = allTxs.filter {
            it.timestamp >= oneWeekAgo && it.direction == "OUTFLOW"
        }
        val totalSpent = weekTxs.sumOf { it.amount }

        val categories = categoryRepository.observeCategories().first()
        val categorySpend = weekTxs.groupBy { it.category_id }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        val topCategory = categorySpend.maxByOrNull { it.value }?.let { (catId, _) ->
            categories.find { it.id == catId }?.name ?: catId
        }

        val unreviewed = allTxs.count { it.is_reviewed == 0L }
        NotificationHelper.showWeeklyDigest(
            applicationContext, totalSpent, topCategory, unreviewed
        )
    }

    companion object {
        private const val TAG = "NightlyCheckWorker"
    }
}
